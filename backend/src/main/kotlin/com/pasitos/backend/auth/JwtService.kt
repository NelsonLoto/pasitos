package com.pasitos.backend.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
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

/** Token emitido y su vencimiento, para que la sesión venza en el mismo momento. */
data class IssuedToken(val token: String, val expiresAt: Instant)

/** Token de acceso: JWT HS256 con `sub` (cuenta), `sid` (sesión) y `jti` (11 · Backend §3). */
class JwtService(private val config: JwtConfig) {

    private val algorithm = Algorithm.HMAC256(config.secret)

    val issuer: String get() = config.issuer

    fun generateToken(accountId: UUID, sessionId: UUID, issuedAt: Instant): IssuedToken {
        val expiresAt = issuedAt.plus(config.expirationMinutes, ChronoUnit.MINUTES)
        val token = JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(accountId.toString())
            .withClaim(SESSION_CLAIM, sessionId.toString())
            .withIssuedAt(issuedAt)
            .withExpiresAt(expiresAt)
            .withJWTId(UUID.randomUUID().toString())
            .sign(algorithm)
        return IssuedToken(token, expiresAt)
    }

    /** Verifica firma, emisor, audiencia y vencimiento. La sesión la verifica [AuthService]. */
    fun verifier(): JWTVerifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    companion object {
        const val SESSION_CLAIM = "sid"
    }
}
