package com.pasitos.backend.account

import com.pasitos.backend.auth.AUTH_JWT
import com.pasitos.backend.common.caller
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import io.ktor.server.routing.post

/** Edición de la cuenta propia, más allá del login (RF-CTA-09, RF-CTA-11, RN-CTA-02; 12 · API). */
fun Route.accountRoutes(accountService: AccountService) {
    authenticate(AUTH_JWT) {
        patch("/me") {
            call.respond(HttpStatusCode.OK, accountService.patchMe(call.caller(), call.receive()))
        }

        post("/me/password") {
            accountService.changePassword(call.caller(), call.receive())
            call.respond(HttpStatusCode.NoContent)
        }

        post("/me/email-change") {
            accountService.requestEmailChange(call.caller(), call.receive())
            call.respond(HttpStatusCode.NoContent)
        }

        post("/me/consents") {
            accountService.acceptConsents(call.caller())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
