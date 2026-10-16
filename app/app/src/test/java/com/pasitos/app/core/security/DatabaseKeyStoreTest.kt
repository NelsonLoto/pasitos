package com.pasitos.app.core.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import java.security.GeneralSecurityException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DatabaseKeyStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val cipher = FakeCipher()
    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create { File(folder.root, "secrets.preferences_pb") }
    }
    private val keyStore by lazy { DatabaseKeyStore(dataStore, cipher) }

    @Test
    fun `ADR-17 sin clave creada no hay nada que leer`() = runTest {
        assertNull(keyStore.read())
    }

    @Test
    fun `ADR-17 crea una clave de 32 bytes y la guarda cifrada`() = runTest {
        val key = keyStore.create()

        assertEquals(32, key.size)
        val stored = dataStore.data.first()[byteArrayPreferencesKey("database_key")]!!
        assertArrayEquals(cipher.encrypt(key, "pasitos.database_key".toByteArray()), stored)
        assertFalse(stored.contentEquals(key))
    }

    @Test
    fun `ADR-17 los usos siguientes leen la misma clave`() = runTest {
        val key = keyStore.create()

        assertArrayEquals(key, keyStore.read())
        assertArrayEquals(key, DatabaseKeyStore(dataStore, cipher).read())
    }

    @Test
    fun `ADR-17 una clave nueva reemplaza a la anterior`() = runTest {
        val first = keyStore.create()
        val second = keyStore.create()

        assertFalse(first.contentEquals(second))
        assertArrayEquals(second, keyStore.read())
    }

    @Test
    fun `ADR-17 si la clave guardada ya no se puede descifrar no la devuelve`() = runTest {
        keyStore.create()
        cipher.keystoreKeyLost = true

        assertNull(keyStore.read())
    }

    @Test
    fun `RF-CTA-07 al borrar la clave ya no se puede leer`() = runTest {
        keyStore.create()

        keyStore.delete()

        assertNull(keyStore.read())
    }

    @Test
    fun `ADR-17 si el cifrado dejó de funcionar se borran todos los secretos y las claves`() = runTest {
        keyStore.create()
        val token = stringPreferencesKey("refresh_token")
        dataStore.edit { it[token] = "ilegible-sin-la-clave" }

        keyStore.resetEncryption()

        assertTrue(dataStore.data.first().asMap().isEmpty())
        assertTrue(cipher.wasReset)
        assertNull(keyStore.read())
    }

    /** Invierte los bytes y antepone el dato asociado; falla como Tink si la clave ya no existe. */
    private class FakeCipher : LocalCipher {
        var keystoreKeyLost = false
        var wasReset = false

        override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
            associatedData + plaintext.reversedArray()

        override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray {
            if (keystoreKeyLost) throw GeneralSecurityException("decryption failed")
            return ciphertext.copyOfRange(associatedData.size, ciphertext.size).reversedArray()
        }

        override fun reset() {
            wasReset = true
            keystoreKeyLost = false
        }
    }
}
