package com.pasitos.app.core.security

import java.security.GeneralSecurityException

/**
 * Antepone el dato asociado e invierte los bytes. Como Tink, falla si el dato asociado no coincide
 * o si la clave del Keystore ya no existe.
 */
class FakeCipher : LocalCipher {
    var keystoreKeyLost = false
    var wasReset = false

    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
        associatedData + plaintext.reversedArray()

    override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray {
        if (keystoreKeyLost) throw GeneralSecurityException("decryption failed")
        val prefix = ciphertext.copyOfRange(0, minOf(associatedData.size, ciphertext.size))
        if (!prefix.contentEquals(associatedData)) throw GeneralSecurityException("decryption failed")
        return ciphertext.copyOfRange(associatedData.size, ciphertext.size).reversedArray()
    }

    override fun reset() {
        wasReset = true
        keystoreKeyLost = false
    }
}
