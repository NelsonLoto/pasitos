package com.pasitos.backend.auth

import com.pasitos.shared.api.auth.LoginRequest
import com.pasitos.shared.api.auth.RefreshRequest
import com.pasitos.shared.api.auth.RegisterRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes

private data class SessionPrincipal(val caller: Caller)

fun ApplicationCall.caller(): Caller = principal<SessionPrincipal>()?.caller ?: throw unauthorized()

fun Application.configureAuth(service: AuthService, config: AuthConfig) {
    install(Authentication) {
        jwt("session") {
            realm = "pasitos"
            verifier(service.tokens.verifier)
            validate { credential ->
                withContext(Dispatchers.IO) { service.validate(credential.payload)?.let(::SessionPrincipal) }
            }
            challenge { _, _ -> throw unauthorized() }
        }
    }
    install(RateLimit) {
        register(RateLimitName("auth")) {
            rateLimiter(limit = config.requestsPerMinute, refillPeriod = 1.minutes)
            // No confiar en cabeceras X-Forwarded-For que pueda inventar un cliente.
            requestKey { it.request.local.remoteHost }
        }
        register(RateLimitName("account")) {
            rateLimiter(limit = 120, refillPeriod = 1.minutes)
            requestKey { it.principal<SessionPrincipal>()!!.caller.accountId }
        }
    }
    routing {
        route("/api/v1") {
            rateLimit(RateLimitName("auth")) {
                route("/auth") {
                    post("/register") {
                        val request = call.receive<RegisterRequest>()
                        call.respond(HttpStatusCode.Created, withContext(Dispatchers.IO) { service.register(request) })
                    }
                    post("/login") {
                        val request = call.receive<LoginRequest>()
                        call.respond(withContext(Dispatchers.IO) { service.login(request) })
                    }
                    post("/refresh") {
                        val request = call.receive<RefreshRequest>()
                        call.respond(withContext(Dispatchers.IO) { service.refresh(request) })
                    }
                    authenticate("session") {
                        post("/logout") {
                            withContext(Dispatchers.IO) { service.logout(call.caller()) }
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
            }
            authenticate("session") {
                rateLimit(RateLimitName("account")) {
                    get("/me") {
                        call.respond(withContext(Dispatchers.IO) { service.me(call.caller()) })
                    }
                }
            }
        }
    }
}
