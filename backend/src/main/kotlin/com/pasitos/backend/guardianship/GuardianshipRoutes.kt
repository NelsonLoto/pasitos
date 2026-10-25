package com.pasitos.backend.guardianship

import com.pasitos.backend.auth.AUTH_JWT
import com.pasitos.backend.common.ChildNotFoundException
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.common.caller
import com.pasitos.backend.common.toUuidOrNull
import com.pasitos.backend.invitation.InvitationService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.util.UUID

/** `/children/{id}/guardians`, `/children/{id}/guardian-invitations`, `/children/{id}/principal-transfer` (RF-HIJ-05, 07, 08, 09; 12 · API). */
fun Route.guardianshipRoutes(guardianshipService: GuardianshipService, invitationService: InvitationService) {
    authenticate(AUTH_JWT) {
        get("/children/{id}/guardians") {
            call.respond(HttpStatusCode.OK, guardianshipService.list(call.caller(), call.childId()))
        }

        get("/children/{id}/guardian-invitations") {
            call.respond(HttpStatusCode.OK, invitationService.listSent(call.caller(), call.childId()))
        }

        post("/children/{id}/guardian-invitations") {
            invitationService.invite(call.caller(), call.childId(), call.receive())
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/children/{id}/guardians/{accountId}") {
            guardianshipService.remove(call.caller(), call.childId(), call.accountId())
            call.respond(HttpStatusCode.NoContent)
        }

        post("/children/{id}/principal-transfer") {
            guardianshipService.transferPrincipal(call.caller(), call.childId(), call.receive())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun ApplicationCall.childId(): UUID =
    parameters["id"]?.toUuidOrNull() ?: throw ChildNotFoundException()

private fun ApplicationCall.accountId(): UUID =
    parameters["accountId"]?.toUuidOrNull() ?: throw InvalidInputException("Ese tutor no existe")
