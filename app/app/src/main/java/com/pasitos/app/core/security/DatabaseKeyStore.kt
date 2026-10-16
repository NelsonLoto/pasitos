package com.pasitos.app.core.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import com.pasitos.app.core.security.di.SecretsDataStore
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Clave de la base local (ADR-17): 32 bytes aleatorios que se guardan en DataStore cifrados con
 * [LocalCipher]. En claro solo existen en memoria; nunca se registran en logs.
 */
@Singleton
class DatabaseKeyStore @Inject constructor(
    @param:SecretsDataStore private val dataStore: DataStore<Preferences>,
    private val cipher: LocalCipher,
) {

    /**
     * La clave guardada, o nulo si no hay ninguna o si ya no se puede descifrar (por ejemplo,
     * porque el sistema borró la clave del Keystore). En los dos casos el archivo de la base, si
     * existe, ya no se puede abrir.
     */
    suspend fun read(): ByteArray? {
        val encrypted = dataStore.data.first()[ENCRYPTED_KEY] ?: return null
        return try {
            cipher.decrypt(encrypted, ASSOCIATED_DATA)
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    /** Genera una clave nueva y la guarda en lugar de la anterior. */
    suspend fun create(): ByteArray {
        val key = ByteArray(KEY_SIZE_BYTES).also(SecureRandom()::nextBytes)
        val encrypted = cipher.encrypt(key, ASSOCIATED_DATA)
        dataStore.edit { it[ENCRYPTED_KEY] = encrypted }
        return key
    }

    suspend fun delete() {
        dataStore.edit { it.remove(ENCRYPTED_KEY) }
    }

    companion object {
        const val KEY_SIZE_BYTES = 32
        private val ENCRYPTED_KEY = byteArrayPreferencesKey("database_key")
        private val ASSOCIATED_DATA = "pasitos.database_key".toByteArray()
    }
}
