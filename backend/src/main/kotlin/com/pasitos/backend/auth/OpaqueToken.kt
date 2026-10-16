package com.pasitos.backend.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Tokens opacos (renovación de sesión, links de email): 256 bits aleatorios. La base guarda solo
 * su SHA-256. A diferencia de las contraseñas, el hash es determinístico: así se busca la fila a
 * partir del token.
 */
object OpaqueToken {

    private val random = SecureRandom()

    fun generate(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
