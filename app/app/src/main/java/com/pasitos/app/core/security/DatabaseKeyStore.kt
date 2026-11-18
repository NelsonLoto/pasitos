package com.pasitos.app.core.security

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

internal class DatabaseKey(val bytes: ByteArray, val isNew: Boolean)

@Singleton
class DatabaseKeyStore @Inject constructor(
    private val secrets: DataStore<Preferences>,
    private val cipher: LocalCipher,
) {
    private val mutex = Mutex()

    internal suspend fun getOrCreate(): DatabaseKey = mutex.withLock {
        val stored = secrets.data.first()[KEY]
        if (stored != null) {
            try {
                val plain = cipher.decrypt(Base64.decode(stored, Base64.NO_WRAP), ASSOCIATED_DATA)
                if (plain.size == 32) return@withLock DatabaseKey(plain, false)
                plain.fill(0)
            } catch (_: GeneralSecurityException) {
                // La copia local solo es recuperable con esta clave. Se recreará sin datos anteriores.
            } catch (_: IllegalArgumentException) {
                // Un valor corrupto nunca se interpreta como clave en claro.
            } catch (_: IOException) {
                // Un keyset ilegible se recupera al volver a intentar cifrar.
            }
        }
        val key = ByteArray(32).also(SecureRandom()::nextBytes)
        try {
            val encrypted = try {
                cipher.encrypt(key, ASSOCIATED_DATA)
            } catch (_: GeneralSecurityException) {
                // Si el Keystore fue invalidado también se pierden los secretos de la sesión.
                secrets.edit { it.clear() }
                cipher.reset()
                cipher.encrypt(key, ASSOCIATED_DATA)
            } catch (_: IOException) {
                secrets.edit { it.clear() }
                cipher.reset()
                cipher.encrypt(key, ASSOCIATED_DATA)
            }
            secrets.edit { it[KEY] = Base64.encodeToString(encrypted, Base64.NO_WRAP) }
            DatabaseKey(key, true)
        } catch (failure: Exception) {
            key.fill(0)
            throw failure
        }
    }

    suspend fun delete() = mutex.withLock { secrets.edit { it.remove(KEY) }; Unit }

    companion object {
        internal val KEY = stringPreferencesKey("database_key")
        internal const val ASSOCIATED_DATA = "pasitos/database-key/v1"
    }
}
