package com.pasitos.backend.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.config.ApplicationConfig
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    /** Duración del token de los endpoints viejos de /auth, que se borran en I0-06. */
    val legacyExpirationMinutes: Long,
) {
    companion object {
        fun from(config: ApplicationConfig): JwtConfig = JwtConfig(
            secret = config.property("jwt.secret").getString(),
            issuer = config.property("jwt.issuer").getString(),
            audience = config.property("jwt.audience").getString(),
            legacyExpirationMinutes = config.property("jwt.expirationMinutes").getString().toLong(),
        )
    }
}

/** Token emitido y su vencimiento. */
data class IssuedToken(val token: String, val expiresAt: Instant)

/**
 * Token de acceso: JWT HS256 con `sub` (cuenta), `sid` (sesión) y `jti` (11 · Backend §3). Firma y
 * vence con el mismo [clock] que usa el resto del backend, así las pruebas pueden adelantar la hora.
 */
class JwtService(private val config: JwtConfig, private val clock: Clock) {

    private val algorithm = Algorithm.HMAC256(config.secret)

    val issuer: String get() = config.issuer

    val legacyLifetime: Duration get() = Duration.ofMinutes(config.legacyExpirationMinutes)

    fun generateToken(accountId: UUID, sessionId: UUID, issuedAt: Instant, lifetime: Duration): IssuedToken {
        val expiresAt = issuedAt.plus(lifetime)
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
    fun verifier(): JWTVerifier = (
        JWT.require(algorithm)
            .withIssuer(config.issuer)
            .withAudience(config.audience) as JWTVerifier.BaseVerification
        ).build(clock)

    companion object {
        const val SESSION_CLAIM = "sid"
    }
}
