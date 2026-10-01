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

/**
 * Solo emite tokens — todavía no hay ningún endpoint protegido que los valide (ningún
 * dominio lo necesita en este PR), así que no se instala el plugin Authentication/jwt de
 * Ktor. Se agrega recién en el primer dominio que necesite una ruta protegida.
 */
class JwtService(private val config: JwtConfig) {

    fun generateToken(userId: UUID, email: String): String {
        val now = Instant.now()
        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withIssuedAt(now)
            .withExpiresAt(now.plus(config.expirationMinutes, ChronoUnit.MINUTES))
            .sign(Algorithm.HMAC256(config.secret))
    }
}
