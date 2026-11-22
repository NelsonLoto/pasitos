package com.pasitos.backend.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

class PasswordHasher {
    private val strategy = LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)
    private val hasher = BCrypt.with(strategy)
    private val verifier = BCrypt.verifyer(BCrypt.Version.VERSION_2A, strategy)
    private val dummy = hash(UUID.randomUUID().toString())

    fun hash(password: String): String = hasher.hashToString(12, password.toCharArray())
    fun matches(password: String, hash: String?): Boolean {
        val result = verifier.verify(password.toCharArray(), hash ?: dummy).verified
        return hash != null && result
    }
}

class AuthTokens(config: AuthConfig, private val clock: Clock) {
    private val algorithm = Algorithm.HMAC256(config.secret)
    private val random = SecureRandom()
    val verifier: com.auth0.jwt.interfaces.JWTVerifier = (JWT.require(algorithm)
        .withIssuer("pasitos").withAudience("pasitos-app")
        .withClaimPresence("sub").withClaimPresence("sid").withClaimPresence("jti")
        .withClaimPresence("iat").withClaimPresence("exp") as JWTVerifier.BaseVerification).build(clock)

    fun access(accountId: UUID, sessionId: UUID): Pair<String, Instant> {
        val now = clock.instant()
        val expires = now.plus(ACCESS_LIFETIME)
        val token = JWT.create().withIssuer("pasitos").withAudience("pasitos-app")
            .withSubject(accountId.toString()).withClaim("sid", sessionId.toString())
            .withJWTId(UUID.randomUUID().toString()).withIssuedAt(now).withExpiresAt(expires).sign(algorithm)
        return token to expires
    }

    fun refresh(): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(ByteArray(32).also(random::nextBytes))

    fun hash(token: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))

    companion object {
        val ACCESS_LIFETIME: Duration = Duration.ofMinutes(15)
        val SESSION_LIFETIME: Duration = Duration.ofDays(30)
    }
}
