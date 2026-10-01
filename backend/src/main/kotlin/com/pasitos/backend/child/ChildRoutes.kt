package com.pasitos.backend.child

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

fun Route.childRoutes() {
    authenticate("auth-jwt") {
        post("/children") {
            val request = call.receive<CreateChildRequest>()
            val nombre = request.nombre.trim()

            if (nombre.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_input", "Ingresá el nombre"))
                return@post
            }

            val fechaNacimiento = try {
                LocalDate.parse(request.fechaNacimiento)
            } catch (e: DateTimeParseException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_input", "Fecha de nacimiento inválida"))
                return@post
            }
            if (fechaNacimiento.isAfter(LocalDate.now())) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_input", "La fecha no puede ser futura"))
                return@post
            }

            if (request.sexo !in listOf("M", "F")) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_input", "Sexo inválido"))
                return@post
            }

            val tutorId = call.tutorId()
            val response = withContext(Dispatchers.IO) {
                transaction {
                    val id = UUID.randomUUID()
                    Personas.insert {
                        it[Personas.id] = id
                        it[tipoPersona] = "hijo"
                        it[Personas.tutorId] = tutorId
                        it[Personas.nombre] = nombre
                        it[Personas.fechaNacimiento] = fechaNacimiento
                        it[sexo] = request.sexo
                    }
                    ChildResponse(
                        id = id.toString(),
                        nombre = nombre,
                        fechaNacimiento = fechaNacimiento.toString(),
                        sexo = request.sexo,
                    )
                }
            }

            call.respond(HttpStatusCode.Created, response)
        }

        get("/children") {
            val tutorId = call.tutorId()
            val children = withContext(Dispatchers.IO) {
                transaction {
                    Personas.selectAll()
                        .where { (Personas.tutorId eq tutorId) and (Personas.tipoPersona eq "hijo") }
                        .orderBy(Personas.createdAt to SortOrder.ASC)
                        .map {
                            ChildResponse(
                                id = it[Personas.id].toString(),
                                nombre = it[Personas.nombre],
                                fechaNacimiento = it[Personas.fechaNacimiento]!!.toString(),
                                sexo = it[Personas.sexo] ?: "",
                            )
                        }
                }
            }
            call.respond(HttpStatusCode.OK, children)
        }
    }
}
