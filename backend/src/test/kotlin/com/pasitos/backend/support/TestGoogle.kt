package com.pasitos.backend.support

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.NetworkException
import com.auth0.jwk.SigningKeyNotFoundException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pasitos.backend.auth.google.GooglePublicKeys
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.time.Instant
import java.util.Base64

/**
 * Google de prueba (I1-B2): firma ID tokens con una clave RSA propia y la publica como lo hace
 * Google, en formato JWK. Las pruebas no consultan a Google ni necesitan conexión.
 */
object TestGoogle {

    const val CLIENT_ID = "cliente-de-prueba.apps.googleusercontent.com"
    const val KEY_ID = "clave-de-prueba"

    private val publishedKey = newKeyPair()

    /** Una clave que Google no publicó: un token firmado con ella no es válido. */
    val foreignPrivateKey: RSAPrivateKey = newKeyPair().second

    /** Solo publica [KEY_ID]. Pasa por el mismo camino que las claves reales: JWK → clave RSA. */
    val keys = GooglePublicKeys(
        JwkProvider { keyId ->
            if (keyId == KEY_ID) jwk(publishedKey.first) else throw SigningKeyNotFoundException("No hay una clave $keyId", null)
        },
    )

    /** Como si Google no respondiera. */
    val unavailableKeys = GooglePublicKeys(JwkProvider { throw NetworkException("Sin conexión", null) })

    fun idToken(
        subject: String = "google-ana",
        email: String? = "ana@gmail.com",
        emailVerified: Any? = true,
        audience: List<String> = listOf(CLIENT_ID),
        issuer: String = "https://accounts.google.com",
        issuedAt: Instant = Instant.now(),
        expiresAt: Instant = issuedAt.plus(Duration.ofHours(1)),
        keyId: String = KEY_ID,
        privateKey: RSAPrivateKey = publishedKey.second,
    ): String {
        val builder = JWT.create()
            .withKeyId(keyId)
            .withIssuer(issuer)
            .withAudience(*audience.toTypedArray())
            .withSubject(subject)
            .withIssuedAt(issuedAt)
            .withExpiresAt(expiresAt)
        email?.let { builder.withClaim("email", it) }
        when (emailVerified) {
            is Boolean -> builder.withClaim("email_verified", emailVerified)
            is String -> builder.withClaim("email_verified", emailVerified)
        }
        return builder.sign(Algorithm.RSA256(null, privateKey))
    }

    private fun newKeyPair(): Pair<RSAPublicKey, RSAPrivateKey> {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        return pair.public as RSAPublicKey to pair.private as RSAPrivateKey
    }

    private fun jwk(key: RSAPublicKey): Jwk = Jwk.fromValues(
        mapOf(
            "kid" to KEY_ID,
            "kty" to "RSA",
            "alg" to "RS256",
            "use" to "sig",
            "n" to base64Url(key.modulus),
            "e" to base64Url(key.publicExponent),
        ),
    )

    private fun base64Url(value: BigInteger): String {
        val bytes = value.toByteArray().let { if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
