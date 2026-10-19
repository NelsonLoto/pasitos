package com.pasitos.backend.vaccination

import com.pasitos.backend.auth.AUTH_JWT
import com.pasitos.backend.common.ChildNotFoundException
import com.pasitos.backend.common.caller
import com.pasitos.backend.common.toUuidOrNull
import com.pasitos.shared.api.vaccination.VaccinationCalendarResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import java.security.MessageDigest
import kotlinx.serialization.json.Json

/** Vacunación (RF-VAC-01 a 05, 07; 12 · API). */
fun Route.vaccinationRoutes(vaccinationService: VaccinationService) {
    authenticate(AUTH_JWT) {
        get("/vaccination-calendar") {
            call.respondCalendar(vaccinationService.calendar())
        }

        post("/children/{id}/applied-doses") {
            call.respond(HttpStatusCode.Created, vaccinationService.createAppliedDose(call.caller(), call.childId(), call.receive()))
        }

        post("/children/{id}/applied-doses/retroactive") {
            call.respond(HttpStatusCode.Created, vaccinationService.registerRetroactive(call.caller(), call.childId(), call.receive()))
        }

        patch("/applied-doses/{id}") {
            val doseId = call.parameters["id"]?.toUuidOrNull() ?: throw AppliedDoseNotFoundException()
            call.respond(HttpStatusCode.OK, vaccinationService.updateAppliedDose(call.caller(), doseId, call.receive()))
        }

        delete("/applied-doses/{id}") {
            val doseId = call.parameters["id"]?.toUuidOrNull() ?: throw AppliedDoseNotFoundException()
            vaccinationService.deleteAppliedDose(call.caller(), doseId)
            call.respond(HttpStatusCode.NoContent)
        }

        post("/children/{id}/other-vaccines") {
            call.respond(HttpStatusCode.Created, vaccinationService.createOtherVaccine(call.caller(), call.childId(), call.receive()))
        }

        patch("/other-vaccines/{id}") {
            val vaccineId = call.parameters["id"]?.toUuidOrNull() ?: throw OtherVaccineNotFoundException()
            call.respond(HttpStatusCode.OK, vaccinationService.updateOtherVaccine(call.caller(), vaccineId, call.receive()))
        }

        delete("/other-vaccines/{id}") {
            val vaccineId = call.parameters["id"]?.toUuidOrNull() ?: throw OtherVaccineNotFoundException()
            vaccinationService.deleteOtherVaccine(call.caller(), vaccineId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

/**
 * El calendario con un `ETag` que es el hash de la respuesta: cambia ante cualquier cambio (una
 * versión nueva, una que entra en vigencia, un texto corregido). Si la app manda el mismo valor en
 * `If-None-Match`, recibe 304 sin cuerpo (14 · Sincronización). `private, no-cache`: el teléfono
 * lo guarda pero vuelve a preguntar, y ningún intermediario lo guarda.
 */
private suspend fun ApplicationCall.respondCalendar(calendar: VaccinationCalendarResponse) {
    val json = Json.encodeToString(VaccinationCalendarResponse.serializer(), calendar)
    val etag = "\"${sha256(json)}\""
    response.header(HttpHeaders.ETag, etag)
    response.header(HttpHeaders.CacheControl, "private, no-cache")
    val ifNoneMatch = request.header(HttpHeaders.IfNoneMatch)?.split(',')?.map { it.trim().removePrefix("W/") }.orEmpty()
    if (etag in ifNoneMatch || "*" in ifNoneMatch) {
        respond(HttpStatusCode.NotModified)
    } else {
        respondText(json, ContentType.Application.Json)
    }
}

private fun sha256(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

/** Un id que ni siquiera es un UUID no puede ser el de ningún niño: 404, no 400 (11 · Backend §4). */
private fun ApplicationCall.childId() =
    parameters["id"]?.toUuidOrNull() ?: throw ChildNotFoundException()
