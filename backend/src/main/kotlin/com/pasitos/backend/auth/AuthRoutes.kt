package com.pasitos.backend.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
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
                }
                Usuarios.insert {
                    it[personaId] = id
                    it[passwordHash] = PasswordHasher.hash(request.password)
                    it[authProvider] = "email"
                }

                val issued = jwtService.generateToken(id, email)
                SessionStore.create(id, issued)

                AuthResponse(
                    userId = id.toString(),
                    email = email,
                    nombre = nombre,
                    token = issued.token,
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
                val id = persona[Personas.id]

                val usuario = Usuarios.selectAll().where { Usuarios.personaId eq id }.firstOrNull()
                    ?: throw InvalidCredentialsException()
                val hash = usuario[Usuarios.passwordHash] ?: throw InvalidCredentialsException()
                if (!PasswordHasher.verify(request.password, hash)) throw InvalidCredentialsException()

                val issued = jwtService.generateToken(id, email)
                SessionStore.create(id, issued)

                AuthResponse(
                    userId = id.toString(),
                    email = email,
                    nombre = persona[Personas.nombre],
                    token = issued.token,
                )
            }
        }

        call.respond(HttpStatusCode.OK, response)
    }

    authenticate("auth-jwt") {
        post("/auth/logout") {
            // authenticate ya garantizó que hay un Bearer válido con sesión vigente.
            val token = call.request.bearerToken()!!
            withContext(Dispatchers.IO) { transaction { SessionStore.revoke(token) } }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
