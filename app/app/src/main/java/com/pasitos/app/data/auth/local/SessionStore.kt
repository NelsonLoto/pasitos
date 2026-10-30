package com.pasitos.app.data.auth.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import com.pasitos.app.core.security.LocalCipher
import com.pasitos.app.core.security.di.SecretsDataStore
import com.pasitos.shared.api.auth.AccountDto
import com.pasitos.shared.api.auth.TokenPair
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Sesión guardada para restaurarla al abrir la app (RF-CTA-06). [tokens] es nulo si la sesión
 * venció: la cuenta se conserva para saber de quién son los datos de la base local.
 */
@Serializable
data class StoredSession(
    val account: AccountDto,
    val tokens: TokenPair?,
)

/**
 * Guarda la sesión en el DataStore `secrets`, cifrada con [LocalCipher] (15 · App Android §4). En
 * claro solo existe en memoria; nunca se registra en logs.
 */
@Singleton
class SessionStore @Inject constructor(
    @param:SecretsDataStore private val dataStore: DataStore<Preferences>,
    private val cipher: LocalCipher,
) {

    /** La sesión guardada, o nula si no hay ninguna o si ya no se puede descifrar. */
    suspend fun read(): StoredSession? {
        val encrypted = dataStore.data.first()[ENCRYPTED_SESSION] ?: return null
        return try {
            Json.decodeFromString<StoredSession>(cipher.decrypt(encrypted, ASSOCIATED_DATA).decodeToString())
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: SerializationException) {
            null
        }
    }

    suspend fun save(session: StoredSession) {
        val encrypted = cipher.encrypt(Json.encodeToString(session).encodeToByteArray(), ASSOCIATED_DATA)
        dataStore.edit { it[ENCRYPTED_SESSION] = encrypted }
    }

    suspend fun delete() {
        dataStore.edit { it.remove(ENCRYPTED_SESSION) }
    }

    /**
     * Tokens de renovación de sesiones cerradas sin conexión, que todavía hay que revocar en el
     * servidor (RF-CTA-07, CA3). Sobreviven al cierre de sesión hasta que se revocan.
     */
    suspend fun pendingRevocations(): List<String> = decryptRevocations(dataStore.data.first())

    suspend fun addPendingRevocation(refreshToken: String) {
        dataStore.edit { it.setRevocations(decryptRevocations(it) + refreshToken) }
    }

    suspend fun removePendingRevocations(refreshTokens: Collection<String>) {
        dataStore.edit { it.setRevocations(decryptRevocations(it) - refreshTokens.toSet()) }
    }

    /** Si ya no se pueden descifrar, tampoco se pueden revocar: el servidor las vence a los 30 días. */
    private fun decryptRevocations(preferences: Preferences): List<String> {
        val encrypted = preferences[ENCRYPTED_REVOCATIONS] ?: return emptyList()
        return try {
            Json.decodeFromString<List<String>>(cipher.decrypt(encrypted, REVOCATIONS_ASSOCIATED_DATA).decodeToString())
        } catch (_: GeneralSecurityException) {
            emptyList()
        } catch (_: SerializationException) {
            emptyList()
        }
    }

    private fun MutablePreferences.setRevocations(refreshTokens: List<String>) {
        if (refreshTokens.isEmpty()) {
            remove(ENCRYPTED_REVOCATIONS)
        } else {
            this[ENCRYPTED_REVOCATIONS] =
                cipher.encrypt(Json.encodeToString(refreshTokens).encodeToByteArray(), REVOCATIONS_ASSOCIATED_DATA)
        }
    }

    private companion object {
        val ENCRYPTED_SESSION = byteArrayPreferencesKey("session")
        val ENCRYPTED_REVOCATIONS = byteArrayPreferencesKey("pending_revocations")

        /** Distinto del de la clave de la base: un valor no se puede descifrar como si fuera el otro. */
        val ASSOCIATED_DATA = "pasitos.session".toByteArray()
        val REVOCATIONS_ASSOCIATED_DATA = "pasitos.pending_revocations".toByteArray()
    }
}
