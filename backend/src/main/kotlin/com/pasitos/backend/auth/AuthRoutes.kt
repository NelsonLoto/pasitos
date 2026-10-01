package com.pasitos.backend.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.authRoutes(jwtService: JwtService) {

    post("/auth/register") {
        val request = call.receive<RegisterRequest>()
        val nombre = request.nombre.trim()
        val email = request.email.trim().lowercase()

        if (nombre.isBlank() || email.isBlank() || request.password.length < 6) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("invalid_input", "Completá nombre, email y una contraseña de al menos 6 caracteres"),
            )
            return@post
        }

        val response = withContext(Dispatchers.IO) {
            transaction {
                val yaExiste = Personas.selectAll().where { Personas.email eq email }.firstOrNull() != null
                if (yaExiste) throw EmailAlreadyInUseException()

                val id = UUID.randomUUID()
                Personas.insert {
                    it[Personas.id] = id
                    it[tipoPersona] = "tutor"
                    it[Personas.nombre] = nombre
                    it[Personas.email] = email
                    it[passwordHash] = PasswordHasher.hash(request.password)
                    it[authProvider] = "email"
                }

                AuthResponse(
                    userId = id.toString(),
                    email = email,
                    nombre = nombre,
                    token = jwtService.generateToken(id, email),
                )
            }
        }

        call.respond(HttpStatusCode.Created, response)
    }

    post("/auth/login") {
        val request = call.receive<LoginRequest>()
        val email = request.email.trim().lowercase()

        val response = withContext(Dispatchers.IO) {
            transaction {
                val persona = Personas.selectAll().where { Personas.email eq email }.firstOrNull()
                    ?: throw InvalidCredentialsException()

                val hash = persona[Personas.passwordHash] ?: throw InvalidCredentialsException()
                if (!PasswordHasher.verify(request.password, hash)) throw InvalidCredentialsException()

                val id = persona[Personas.id]
                AuthResponse(
                    userId = id.toString(),
                    email = email,
                    nombre = persona[Personas.nombre],
                    token = jwtService.generateToken(id, email),
                )
            }
        }

        call.respond(HttpStatusCode.OK, response)
    }
}
