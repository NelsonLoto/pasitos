package com.pasitos.app.core.security

/**
 * Cifra datos que se guardan en el teléfono con una clave que no sale del Android Keystore
 * (ADR-17). Hoy protege la clave de la base local; en I0-06 también los tokens de sesión.
 *
 * Todas las fallas, también las del Keystore, llegan como [java.security.GeneralSecurityException].
 */
interface LocalCipher {

    /**
     * [associatedData] ata el resultado a su uso: algo cifrado como clave de la base no se puede
     * descifrar haciéndolo pasar por un token, ni al revés.
     */
    fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray

    /** Falla si el dato fue alterado, si se cifró con otra clave o si la clave ya no existe. */
    fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray

    /**
     * Borra las claves. Todo lo cifrado hasta ahora queda ilegible para siempre; el próximo uso
     * crea claves nuevas. Solo para cuando el cifrado dejó de funcionar.
     */
    fun reset()
}
