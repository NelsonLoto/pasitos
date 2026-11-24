package com.pasitos.app.core.security

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pasitos.shared.api.auth.AccountResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class StoredSession(val account: AccountResponse, val accessToken: String?, val refreshToken: String?, val simulated: Boolean = false) {
    override fun toString() = "StoredSession([redacted])"
}

interface SessionStorage {
    val invalidations: Flow<Unit> get() = emptyFlow()
    suspend fun read(): StoredSession?
    suspend fun save(value: StoredSession?)
    suspend fun pendingRevocations(): Set<String>
    suspend fun addRevocation(token: String)
    suspend fun removeRevocation(token: String)
}

@Singleton
class SessionStore @Inject constructor(private val secrets: DataStore<Preferences>, private val cipher: LocalCipher) : SessionStorage {
    private val json = Json { ignoreUnknownKeys = true }
    override val invalidations: Flow<Unit> = secrets.data.map { it[SESSION] }.distinctUntilChanged()
        .drop(1).filter { it == null }.map { }
    override suspend fun read(): StoredSession? = secrets.data.first()[SESSION]?.let {
        json.decodeFromString<StoredSession>(decode(it, SESSION_AD))
    }

    override suspend fun save(value: StoredSession?) {
        val encrypted = value?.let {
            try { encode(json.encodeToString(it), SESSION_AD) }
            catch (failure: Exception) {
                if (failure !is java.security.GeneralSecurityException && failure !is java.io.IOException) throw failure
                secrets.edit { values -> values.clear() }
                cipher.reset()
                encode(json.encodeToString(it), SESSION_AD)
            }
        }
        secrets.edit { if (encrypted == null) it.remove(SESSION) else it[SESSION] = encrypted }
    }

    override suspend fun pendingRevocations(): Set<String> = secrets.data.first()[REVOCATIONS]?.let {
        json.decodeFromString<Set<String>>(decode(it, REVOKE_AD))
    } ?: emptySet()

    override suspend fun addRevocation(token: String) = editRevocations { it + token }
    override suspend fun removeRevocation(token: String) = editRevocations { it - token }

    private suspend fun editRevocations(change: (Set<String>) -> Set<String>) {
        secrets.edit { values ->
            val current = values[REVOCATIONS]?.let { json.decodeFromString<Set<String>>(decode(it, REVOKE_AD)) } ?: emptySet()
            val next = change(current)
            if (next.isEmpty()) values.remove(REVOCATIONS) else values[REVOCATIONS] = encode(json.encodeToString(next), REVOKE_AD)
        }
    }

    private fun encode(value: String, purpose: String) = Base64.encodeToString(cipher.encrypt(value.toByteArray(), purpose), Base64.NO_WRAP)
    private fun decode(value: String, purpose: String): String {
        val plain = cipher.decrypt(Base64.decode(value, Base64.NO_WRAP), purpose)
        return try { plain.toString(Charsets.UTF_8) } finally { plain.fill(0) }
    }

    companion object {
        internal val SESSION = stringPreferencesKey("session")
        private val REVOCATIONS = stringPreferencesKey("pending_session_revocations")
        internal const val SESSION_AD = "pasitos/session/v1"
        private const val REVOKE_AD = "pasitos/session-revocations/v1"
    }
}
