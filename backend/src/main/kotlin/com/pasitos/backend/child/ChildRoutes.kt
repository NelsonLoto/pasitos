package com.pasitos.backend.child

import com.pasitos.backend.auth.AUTH_JWT
import com.pasitos.backend.common.ChildNotFoundException
import com.pasitos.backend.common.caller
import com.pasitos.backend.common.toUuidOrNull
import com.pasitos.shared.api.child.CreatePatientRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post

/** `/children` (RF-HIJ-01 a 03, 09; 12 · API). */
fun Route.childRoutes(childService: ChildService) {
    authenticate(AUTH_JWT) {
        post("/patients") {
            call.respond(HttpStatusCode.Created, childService.createPatient(call.caller(), call.receive<CreatePatientRequest>()))
        }
        post("/children") {
            call.respond(HttpStatusCode.Created, childService.create(call.caller(), call.receive()))
        }

        get("/children/{id}") {
            call.respond(HttpStatusCode.OK, childService.get(call.caller(), call.childId()))
        }

        patch("/children/{id}") {
            call.respond(HttpStatusCode.OK, childService.update(call.caller(), call.childId(), call.receive()))
        }

        get("/children/{id}/deletion-preview") {
            call.respond(HttpStatusCode.OK, childService.deletionPreview(call.caller(), call.childId()))
        }

        delete("/children/{id}") {
            childService.delete(call.caller(), call.childId())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

/** Un id que ni siquiera es un UUID no puede ser el de ningún niño: 404, no 400 (11 · Backend §4). */
private fun ApplicationCall.childId() =
    parameters["id"]?.toUuidOrNull() ?: throw ChildNotFoundException()
