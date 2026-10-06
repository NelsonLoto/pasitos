package com.pasitos.backend.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.config.ApplicationConfig
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val expirationMinutes: Long,
) {
    companion object {
        fun from(config: ApplicationConfig): JwtConfig = JwtConfig(
            secret = config.property("jwt.secret").getString(),
            issuer = config.property("jwt.issuer").getString(),
            audience = config.property("jwt.audience").getString(),
            expirationMinutes = config.property("jwt.expirationMinutes").getString().toLong(),
        )
    }
}

/** Token emitido + su vencimiento, para que la fila de `sesiones` use exactamente el mismo. */
data class IssuedToken(val token: String, val expiresAt: Instant)

class JwtService(private val config: JwtConfig) {

    fun generateToken(userId: UUID, email: String): IssuedToken {
        val now = Instant.now()
        val expiresAt = now.plus(config.expirationMinutes, ChronoUnit.MINUTES)
        val token = JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withIssuedAt(now)
            .withExpiresAt(expiresAt)
            // `iat` tiene precisión de segundos: sin un jti único, dos logins del mismo usuario
            // en el mismo segundo darían el mismo token y chocarían con sesiones.token_hash UNIQUE.
            .withJWTId(UUID.randomUUID().toString())
            .sign(Algorithm.HMAC256(config.secret))
        return IssuedToken(token, expiresAt)
    }
}
