package com.pasitos.backend

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pasitos.backend.auth.EmailAlreadyInUseException
import com.pasitos.backend.auth.ErrorResponse
import com.pasitos.backend.auth.InvalidCredentialsException
import com.pasitos.backend.auth.JwtConfig
import com.pasitos.backend.auth.JwtService
import com.pasitos.backend.auth.SessionStore
import com.pasitos.backend.auth.authRoutes
import com.pasitos.backend.auth.bearerToken
import com.pasitos.backend.child.childRoutes
import com.pasitos.backend.db.DatabaseFactory
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>) {
    // En desarrollo local carga backend/.env (no versionado) a system properties,
    // para que application.conf las resuelva con ${?DB_HOST}. Si el archivo no
    // existe (Docker/CI, donde las variables ya están en el entorno), no hace nada.
    dotenv {
        ignoreIfMissing = true
        systemProperties = true
    }
    EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    install(StatusPages) {
        exception<InvalidCredentialsException> { call, _ ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("invalid_credentials", "Email o contraseña incorrectos"))
        }
        exception<EmailAlreadyInUseException> { call, _ ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse("email_already_in_use", "Ya existe una cuenta con este email"))
        }
        exception<Throwable> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error", "Ocurrió un error, intentá de nuevo"))
        }
    }

    DatabaseFactory.init(environment.config)

    val jwtConfig = JwtConfig.from(environment.config)
    val jwtService = JwtService(jwtConfig)

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtConfig.issuer
            verifier(
                JWT.require(Algorithm.HMAC256(jwtConfig.secret))
                    .withIssuer(jwtConfig.issuer)
                    .withAudience(jwtConfig.audience)
                    .build()
            )
            validate { credential ->
                // Firma y vencimiento ya los verificó el verifier; acá se exige además una sesión
                // vigente en la base — así revocar (borrar la fila) invalida el token al instante.
                val token = request.bearerToken()
                if (credential.payload.subject != null && token != null &&
                    withContext(Dispatchers.IO) { transaction { SessionStore.isActive(token) } }
                ) {
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

    routing {
        get("/health") {
            call.respondText("OK")
        }
        authRoutes(jwtService)
        childRoutes()
    }
}
