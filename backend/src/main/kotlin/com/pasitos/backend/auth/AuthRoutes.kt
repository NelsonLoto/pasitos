package com.pasitos.backend.auth

import com.pasitos.backend.common.caller
import com.pasitos.backend.common.toUuidOrNull
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlin.time.Duration.Companion.minutes

/** Nombre del esquema de autenticación de las rutas de la app. */
const val AUTH_JWT = "auth-jwt"

private val LOGIN_LIMIT = RateLimitName("auth-login")
private val REGISTER_LIMIT = RateLimitName("auth-register")

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
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(ErrorCode.UNAUTHORIZED, "Token inválido o ausente"))
            }
        }
    }

    // Límites por IP (RNF-SEG-07). Detrás de un proxy hará falta el plugin XForwardedHeaders para
    // que `origin` sea la IP real del usuario (I6).
    install(RateLimit) {
        register(LOGIN_LIMIT) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteAddress }
        }
        register(REGISTER_LIMIT) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteAddress }
        }
    }
}

/** Rutas de autenticación (`/auth/...`) y `/me` dentro de `/api/v1` (12 · API). */
fun Route.authRoutes(authService: AuthService) {
    rateLimit(REGISTER_LIMIT) {
        post("/auth/register") {
            call.respond(HttpStatusCode.Created, authService.register(call.receive()))
        }
    }

    rateLimit(LOGIN_LIMIT) {
        post("/auth/login") {
            call.respond(HttpStatusCode.OK, authService.login(call.receive()))
        }
    }

    // Requiere el token de renovación, que no se puede adivinar: no necesita un límite propio.
    post("/auth/refresh") {
        call.respond(HttpStatusCode.OK, authService.refresh(call.receive()))
    }

    authenticate(AUTH_JWT) {
        post("/auth/logout") {
            authService.logout(call.caller())
            call.respond(HttpStatusCode.NoContent)
        }

        get("/me") {
            call.respond(HttpStatusCode.OK, authService.me(call.caller()))
        }
    }
}

/** Endpoints viejos de `/auth` (sin `/api/v1`) que usa la app actual. Se borran en I0-06. */
fun Route.legacyAuthRoutes(authService: AuthService) {
    rateLimit(REGISTER_LIMIT) {
        post("/auth/register") {
            call.respond(HttpStatusCode.Created, authService.legacyRegister(call.receive()))
        }
    }

    rateLimit(LOGIN_LIMIT) {
        post("/auth/login") {
            call.respond(HttpStatusCode.OK, authService.legacyLogin(call.receive()))
        }
    }

    authenticate(AUTH_JWT) {
        post("/auth/logout") {
            authService.logout(call.caller())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
