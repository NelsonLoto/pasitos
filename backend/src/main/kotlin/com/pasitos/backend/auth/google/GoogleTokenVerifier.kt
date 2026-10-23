package com.pasitos.backend.auth.google

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.RSAKeyProvider
import com.pasitos.backend.common.UnauthorizedException
import com.pasitos.shared.api.ErrorCode
import java.time.Clock

class InvalidGoogleTokenException : UnauthorizedException(
    ErrorCode.INVALID_GOOGLE_TOKEN,
    "No pudimos validar tu cuenta de Google. Probá de nuevo",
)

/** Lo que garantiza un ID token de Google ya verificado. */
data class GoogleIdentity(
    /** Identificador estable de la cuenta de Google: no cambia aunque cambie el email. */
    val subject: String,
    /** En minúsculas, como `accounts.email`. Google lo verificó (`email_verified`). */
    val email: String,
)

/**
 * Verifica el ID token que la app obtiene con Credential Manager (11 · Backend §3, ADR-11): firma
 * RS256 con una clave publicada por Google, emisor de Google, destinatario igual a nuestro cliente
 * web ([clientId]), vencimiento y email verificado. Usa el mismo [clock] que el resto del backend.
 */
class GoogleTokenVerifier(
    private val clientId: String,
    keys: RSAKeyProvider,
    clock: Clock,
) {

    // El algoritmo queda fijo: un token que declare otro (HS256, "none") se rechaza.
    private val verifier = (
        JWT.require(Algorithm.RSA256(keys))
            .withIssuer(*ISSUERS)
            .withAudience(clientId)
            .withClaimPresence("sub")
            .withClaimPresence("email")
            .withClaimPresence("exp")
            .acceptLeeway(CLOCK_LEEWAY_SECONDS) as JWTVerifier.BaseVerification
        ).build(clock)

    /**
     * Lanza [InvalidGoogleTokenException] (401) si el token no es válido, y
     * [GoogleUnavailableException] (503) si no se pudieron obtener las claves de Google.
     */
    fun verify(idToken: String): GoogleIdentity {
        val token = try {
            verifier.verify(idToken)
        } catch (e: JWTVerificationException) {
            throw InvalidGoogleTokenException()
        }
        // `withAudience` acepta un token con varios destinatarios si uno es el nuestro; Google
        // emite uno solo, así que se exige exactamente ese.
        if (token.audience != listOf(clientId)) throw InvalidGoogleTokenException()
        if (token.getClaim("email_verified").asBoolean() != true) throw InvalidGoogleTokenException()
        val email = token.getClaim("email").asString()?.trim()?.lowercase()
        if (email.isNullOrEmpty() || token.subject.isNullOrBlank()) throw InvalidGoogleTokenException()
        return GoogleIdentity(subject = token.subject, email = email)
    }

    private companion object {
        /** Google usa las dos formas. */
        val ISSUERS = arrayOf("https://accounts.google.com", "accounts.google.com")

        /** Diferencia de reloj tolerada entre los servidores de Google y el nuestro. */
        const val CLOCK_LEEWAY_SECONDS = 60L
    }
}
