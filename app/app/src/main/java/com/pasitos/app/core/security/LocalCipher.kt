package com.pasitos.app.core.security

/**
 * Cifra datos que se guardan en el teléfono con una clave que no sale del Android Keystore
 * (ADR-17). Hoy protege la clave de la base local; en I0-06 también los tokens de sesión.
 */
interface LocalCipher {

    /**
     * [associatedData] ata el resultado a su uso: algo cifrado como clave de la base no se puede
     * descifrar haciéndolo pasar por un token, ni al revés.
     */
    fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray

    /** Lanza [java.security.GeneralSecurityException] si el dato fue alterado o la clave ya no existe. */
    fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray
}
