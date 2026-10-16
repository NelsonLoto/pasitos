package com.pasitos.backend.auth

import com.pasitos.backend.common.ErrorResponse
import com.pasitos.backend.common.caller
import com.pasitos.backend.common.toUuidOrNull
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

/** Nombre del esquema de autenticación de las rutas de la app. */
const val AUTH_JWT = "auth-jwt"

fun Application.configureAuthentication(jwt: JwtService, authService: AuthService) {
    install(Authentication) {
        jwt(AUTH_JWT) {
            realm = jwt.issuer
            verifier(jwt.verifier())
            validate { credential ->
                // Firma y vencimiento ya los verificó el verifier. Además la sesión tiene que seguir
                // vigente en la base (RNF-SEG-04).
                val accountId = credential.payload.subject?.toUuidOrNull()
                val sessionId = credential.payload.getClaim(JwtService.SESSION_CLAIM).asString()?.toUuidOrNull()
                if (accountId != null && sessionId != null && authService.isSessionActive(sessionId)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized", "Token inválido o ausente"))
            }
        }
    }
}

fun Route.authRoutes(authService: AuthService) {
    post("/auth/register") {
        call.respond(HttpStatusCode.Created, authService.register(call.receive()))
    }

    post("/auth/login") {
        call.respond(HttpStatusCode.OK, authService.login(call.receive()))
    }

    authenticate(AUTH_JWT) {
        post("/auth/logout") {
            authService.logout(call.caller())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
