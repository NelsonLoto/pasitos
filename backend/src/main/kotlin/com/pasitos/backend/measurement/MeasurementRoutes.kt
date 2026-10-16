package com.pasitos.backend.measurement

import com.pasitos.backend.auth.AUTH_JWT
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.common.caller
import com.pasitos.backend.common.toUuidOrNull
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.measurementRoutes(measurementService: MeasurementService) {
    authenticate(AUTH_JWT) {
        route("/children/{childId}/measurements") {
            post {
                val childId = call.childId()
                call.respond(HttpStatusCode.Created, measurementService.create(call.caller(), childId, call.receive()))
            }

            get {
                call.respond(HttpStatusCode.OK, measurementService.list(call.caller(), call.childId()))
            }
        }
    }
}

private fun ApplicationCall.childId(): UUID =
    parameters["childId"]?.toUuidOrNull() ?: throw InvalidInputException("Id de hijo inválido")
