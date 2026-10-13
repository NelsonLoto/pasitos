package com.pasitos.backend.medicion

import com.pasitos.backend.auth.ErrorResponse
import com.pasitos.backend.auth.Personas
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

private fun ApplicationCall.tutorId(): UUID = UUID.fromString(principal<JWTPrincipal>()!!.subject)

class ChildNotFoundException : RuntimeException()

/** Confirma que [childId] sea un "hijo" de [tutorId] -no de otro tutor-. Debe llamarse dentro de una transacción. */
private fun ownedChildOrNull(childId: UUID, tutorId: UUID) =
    Personas.selectAll()
        .where { (Personas.id eq childId) and (Personas.tutorId eq tutorId) and (Personas.tipoPersona eq "hijo") }
        .firstOrNull()

fun Route.medicionRoutes() {
    authenticate("auth-jwt") {
        post("/children/{childId}/measurements") {
            val tutorId = call.tutorId()
            val childId = call.parameters["childId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (childId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_input", "Id de hijo inválido"))
                return@post
            }

            val request = call.receive<CreateMedicionRequest>()

            val fechaMedicion = try {
                LocalDate.parse(request.fechaMedicion)
            } catch (e: DateTimeParseException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_input", "Fecha de medición inválida"))
                return@post
            }
            if (fechaMedicion.isAfter(LocalDate.now())) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_input", "La fecha no puede ser futura"))
                return@post
            }
            if (request.pesoKg == null && request.tallaCm == null && request.perimetroCefalicoCm == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_input", "Cargá al menos un valor"))
                return@post
            }

            val response = withContext(Dispatchers.IO) {
                transaction {
                    val child = ownedChildOrNull(childId, tutorId) ?: throw ChildNotFoundException()

                    val sexo = child[Personas.sexo]
                    val fechaNacimiento = child[Personas.fechaNacimiento]
                    val edadMeses = fechaNacimiento?.let {
                        val periodo = Period.between(it, fechaMedicion)
                        periodo.years * 12 + periodo.months
                    }

                    val percentilPeso = calcularPercentil(sexo, edadMeses, "peso", request.pesoKg)
                    val percentilTalla = calcularPercentil(sexo, edadMeses, "talla", request.tallaCm)
                    val percentilPerimetro = calcularPercentil(sexo, edadMeses, "perimetro_cefalico", request.perimetroCefalicoCm)

                    // RN03 - fuera de rango si algún percentil calculado está por debajo de 3 o por
                    // encima de 97. Si no se pudo calcular ninguno (sin datos SAP cargados todavía),
                    // queda en false: no hay forma de determinarlo todavía, no es un "no" definitivo.
                    // <= / >= (no < / >): cuando el valor cae por fuera del rango de puntos cargados,
                    // calcularPercentil lo "clampea" exactamente a 3.0 o 97.0 -ese clamp ya significa
                    // "en el límite o más allá", así que debe contar como fuera de rango también.
                    val fueraDeRango = listOfNotNull(percentilPeso, percentilTalla, percentilPerimetro)
                        .any { it <= 3.0 || it >= 97.0 }

                    val nuevoId = Mediciones.insert {
                        it[personaId] = childId
                        it[Mediciones.fechaMedicion] = fechaMedicion
                        it[pesoKg] = request.pesoKg?.toBigDecimal()
                        it[tallaCm] = request.tallaCm?.toBigDecimal()
                        it[perimetroCefalicoCm] = request.perimetroCefalicoCm?.toBigDecimal()
                        it[Mediciones.percentilPeso] = percentilPeso?.toBigDecimal()
                        it[Mediciones.percentilTalla] = percentilTalla?.toBigDecimal()
                        it[percentilPerimetroCefalico] = percentilPerimetro?.toBigDecimal()
                        it[Mediciones.fueraDeRango] = fueraDeRango
                    } get Mediciones.id

                    MedicionResponse(
                        id = nuevoId,
                        fechaMedicion = fechaMedicion.toString(),
                        pesoKg = request.pesoKg,
                        tallaCm = request.tallaCm,
                        perimetroCefalicoCm = request.perimetroCefalicoCm,
                        percentilPeso = percentilPeso,
                        percentilTalla = percentilTalla,
                        percentilPerimetroCefalico = percentilPerimetro,
                        fueraDeRango = fueraDeRango,
                    )
                }
            }

            call.respond(HttpStatusCode.Created, response)
        }

        get("/children/{childId}/measurements") {
            val tutorId = call.tutorId()
            val childId = call.parameters["childId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (childId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_input", "Id de hijo inválido"))
                return@get
            }

            val response = withContext(Dispatchers.IO) {
                transaction {
                    ownedChildOrNull(childId, tutorId) ?: throw ChildNotFoundException()

                    Mediciones.selectAll()
                        .where { Mediciones.personaId eq childId }
                        .orderBy(Mediciones.fechaMedicion to SortOrder.ASC)
                        .map {
                            MedicionResponse(
                                id = it[Mediciones.id],
                                fechaMedicion = it[Mediciones.fechaMedicion].toString(),
                                pesoKg = it[Mediciones.pesoKg]?.toDouble(),
                                tallaCm = it[Mediciones.tallaCm]?.toDouble(),
                                perimetroCefalicoCm = it[Mediciones.perimetroCefalicoCm]?.toDouble(),
                                percentilPeso = it[Mediciones.percentilPeso]?.toDouble(),
                                percentilTalla = it[Mediciones.percentilTalla]?.toDouble(),
                                percentilPerimetroCefalico = it[Mediciones.percentilPerimetroCefalico]?.toDouble(),
                                fueraDeRango = it[Mediciones.fueraDeRango],
                            )
                        }
                }
            }

            call.respond(HttpStatusCode.OK, response)
        }
    }
}
