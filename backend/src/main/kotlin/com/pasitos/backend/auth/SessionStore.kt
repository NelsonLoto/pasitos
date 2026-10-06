package com.pasitos.backend.auth

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.request.ApplicationRequest
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

fun ApplicationRequest.bearerToken(): String? =
    (parseAuthorizationHeader() as? HttpAuthHeader.Single)?.blob

/**
 * Todas las funciones asumen que ya hay una `transaction { }` abierta (así register/login
 * pueden crear persona, usuario y sesión de forma atómica).
 *
 * La base guarda el SHA-256 del JWT — nunca el token. A diferencia de las contraseñas (bcrypt),
 * acá hace falta un hash determinístico: es lo que permite buscar la sesión a partir del token.
 * Los vencimientos se guardan y comparan siempre en UTC.
 */
object SessionStore {

    fun create(usuarioId: UUID, issued: IssuedToken) {
        Sesiones.insert {
            it[id] = UUID.randomUUID()
            it[Sesiones.usuarioId] = usuarioId
            it[tokenHash] = hash(issued.token)
            it[expiresAt] = LocalDateTime.ofInstant(issued.expiresAt, ZoneOffset.UTC)
        }
    }

    fun isActive(token: String): Boolean =
        !Sesiones.selectAll()
            .where {
                (Sesiones.tokenHash eq hash(token)) and
                    (Sesiones.expiresAt greater LocalDateTime.now(ZoneOffset.UTC))
            }
            .empty()

    fun revoke(token: String): Int =
        Sesiones.deleteWhere { Sesiones.tokenHash eq hash(token) }

    private fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
