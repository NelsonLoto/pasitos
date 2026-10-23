package com.pasitos.backend.auth.google

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pasitos.backend.support.TestClock
import com.pasitos.backend.support.TestGoogle
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Verificación del ID token de Google (I1-B2, 11 · Backend §3), sin red ni base de datos. */
class GoogleTokenVerifierTest {

    private val clock = TestClock()
    private val now = clock.instant()
    private val verifier = GoogleTokenVerifier(TestGoogle.CLIENT_ID, TestGoogle.keys, clock)

    @Test
    fun `un token válido devuelve la cuenta de Google y el email en minúsculas`() {
        val identity = verifier.verify(TestGoogle.idToken(subject = "123", email = "Ana@Gmail.com", issuedAt = now))

        assertEquals(GoogleIdentity(subject = "123", email = "ana@gmail.com"), identity)
    }

    @Test
    fun `acepta las dos formas del emisor de Google`() {
        listOf("https://accounts.google.com", "accounts.google.com").forEach { issuer ->
            verifier.verify(TestGoogle.idToken(issuer = issuer, issuedAt = now))
        }
    }

    @Test
    fun `rechaza un token vencido, salvo dentro del minuto de tolerancia de reloj`() {
        verifier.verify(TestGoogle.idToken(issuedAt = now.minus(Duration.ofHours(1)), expiresAt = now.minusSeconds(30)))

        assertInvalid(TestGoogle.idToken(issuedAt = now.minus(Duration.ofHours(1)), expiresAt = now.minusSeconds(90)))
    }

    @Test
    fun `rechaza un token para otro destinatario, aunque también venga para el nuestro`() {
        assertInvalid(TestGoogle.idToken(audience = listOf("otro-cliente.apps.googleusercontent.com"), issuedAt = now))
        assertInvalid(TestGoogle.idToken(audience = listOf(TestGoogle.CLIENT_ID, "otro-cliente"), issuedAt = now))
    }

    @Test
    fun `rechaza un token de otro emisor`() {
        assertInvalid(TestGoogle.idToken(issuer = "https://evil.example.com", issuedAt = now))
    }

    @Test
    fun `rechaza un email sin verificar, sin el dato o con el dato como texto`() {
        assertInvalid(TestGoogle.idToken(emailVerified = false, issuedAt = now))
        assertInvalid(TestGoogle.idToken(emailVerified = null, issuedAt = now))
        assertInvalid(TestGoogle.idToken(emailVerified = "true", issuedAt = now))
    }

    @Test
    fun `rechaza un token sin email`() {
        assertInvalid(TestGoogle.idToken(email = null, issuedAt = now))
    }

    @Test
    fun `rechaza un token firmado con una clave que Google no publicó`() {
        assertInvalid(TestGoogle.idToken(privateKey = TestGoogle.foreignPrivateKey, issuedAt = now))
        assertInvalid(TestGoogle.idToken(keyId = "clave-inventada", issuedAt = now))
    }

    @Test
    fun `rechaza un token firmado con otro algoritmo o que no es un JWT`() {
        val hs256 = JWT.create()
            .withKeyId(TestGoogle.KEY_ID)
            .withIssuer("https://accounts.google.com")
            .withAudience(TestGoogle.CLIENT_ID)
            .withSubject("123")
            .withClaim("email", "ana@gmail.com")
            .withClaim("email_verified", true)
            .withExpiresAt(now.plus(Duration.ofHours(1)))
            .sign(Algorithm.HMAC256("secreto"))

        assertInvalid(hs256)
        assertInvalid("no-es-un-jwt")
    }

    @Test
    fun `si Google no responde lanza GoogleUnavailableException`() {
        val offline = GoogleTokenVerifier(TestGoogle.CLIENT_ID, TestGoogle.unavailableKeys, clock)

        assertFailsWith<GoogleUnavailableException> { offline.verify(TestGoogle.idToken(issuedAt = now)) }
    }

    private fun assertInvalid(token: String) {
        assertFailsWith<InvalidGoogleTokenException> { verifier.verify(token) }
    }
}
