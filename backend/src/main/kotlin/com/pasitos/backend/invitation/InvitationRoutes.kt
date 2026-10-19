package com.pasitos.backend.invitation

import com.pasitos.backend.auth.AUTH_JWT
import com.pasitos.backend.common.caller
import com.pasitos.backend.common.toUuidOrNull
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

/** `/me/invitations`, `/invitations/{id}` (RF-HIJ-06; 12 · API). */
fun Route.invitationRoutes(invitationService: InvitationService) {
    authenticate(AUTH_JWT) {
        get("/me/invitations") {
            call.respond(HttpStatusCode.OK, invitationService.listMine(call.caller()))
        }

        post("/invitations/{id}/accept") {
            invitationService.accept(call.caller(), call.invitationId(), call.receive())
            call.respond(HttpStatusCode.NoContent)
        }

        post("/invitations/{id}/reject") {
            invitationService.reject(call.caller(), call.invitationId())
            call.respond(HttpStatusCode.NoContent)
        }

        post("/invitations/{id}/resend") {
            invitationService.resend(call.caller(), call.invitationId())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

/** Un id que ni siquiera es un UUID no puede ser el de ninguna invitación: 404, no 400. */
private fun ApplicationCall.invitationId() =
    parameters["id"]?.toUuidOrNull() ?: throw InvitationNotFoundException()
