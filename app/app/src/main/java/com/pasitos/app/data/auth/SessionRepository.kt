package com.pasitos.app.data.auth

import android.util.Log
import com.pasitos.app.core.network.RefreshResult
import com.pasitos.app.core.network.SessionTokens
import com.pasitos.app.data.auth.local.SessionStore
import com.pasitos.app.data.auth.local.StoredSession
import com.pasitos.app.data.auth.model.AuthSession
import com.pasitos.app.data.auth.model.SessionState
import com.pasitos.shared.api.auth.AccountDto
import com.pasitos.shared.api.auth.TokenPair
import java.io.IOException
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Sesión del usuario (RF-CTA-06): la restaura al abrir la app, la guarda cifrada con
 * [SessionStore] y renueva sus tokens para el cliente de la API. Los cambios se hacen de a uno.
 */
@Singleton
class SessionRepository @Inject constructor(
    private val store: SessionStore,
) : SessionTokens {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _state = MutableStateFlow<SessionState>(SessionState.Restoring)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _session = MutableStateFlow<AuthSession?>(null)
    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    /** Copia en memoria de lo guardado. Se modifica solo con [mutex] tomado. */
    @Volatile
    private var stored: StoredSession? = null

    private val restored: Job = scope.launch {
        stored = try {
            store.read()
        } catch (e: IOException) {
            Log.w(TAG, "No se pudo leer la sesión guardada", e)
            null
        }
        publish()
    }

    override suspend fun current(): TokenPair? {
        restored.join()
        return stored?.tokens
    }

    /**
     * Inicia la sesión de [account]. Si los datos locales son de otra cuenta, cuya sesión venció,
     * devuelve el ID de esa cuenta para que se borren.
     */
    suspend fun start(account: AccountDto, tokens: TokenPair): String? = locked {
        val previousOwner = stored?.account?.id?.takeIf { it != account.id }
        save(StoredSession(account, tokens))
        previousOwner
    }

    override suspend fun refresh(
        rejectedAccessToken: String?,
        renew: suspend (refreshToken: String) -> RefreshResult,
    ): TokenPair? = locked {
        val current = stored ?: return@locked null
        val tokens = current.tokens ?: return@locked null
        if (tokens.accessToken != rejectedAccessToken) return@locked tokens
        when (val result = renew(tokens.refreshToken)) {
            is RefreshResult.Renewed -> {
                save(current.copy(tokens = result.tokens))
                result.tokens
            }
            RefreshResult.Rejected -> {
                Log.i(TAG, "El servidor rechazó la renovación: la sesión venció o fue revocada")
                save(current.copy(tokens = null))
                null
            }
            RefreshResult.Unavailable -> null
        }
    }

    /** Cierre de sesión: borra la cuenta y los tokens guardados. */
    suspend fun clear() = locked {
        stored = null
        publish()
        store.delete()
    }

    private suspend fun <T> locked(block: suspend () -> T): T {
        restored.join()
        return mutex.withLock { block() }
    }

    /**
     * Primero en memoria: si no se puede guardar, la sesión sigue hasta que se cierre la app. Es
     * preferible a perder un token de renovación que el servidor ya rotó.
     */
    private suspend fun save(session: StoredSession) {
        stored = session
        publish()
        try {
            store.save(session)
        } catch (e: GeneralSecurityException) {
            Log.w(TAG, "No se pudo cifrar la sesión", e)
        } catch (e: IOException) {
            Log.w(TAG, "No se pudo guardar la sesión", e)
        }
    }

    private fun publish() {
        val current = stored
        _session.value = current?.takeIf { it.tokens != null }?.account?.toSession()
        _state.value = when {
            current == null -> SessionState.SignedOut()
            current.tokens == null -> SessionState.SignedOut(expired = true)
            else -> SessionState.SignedIn(current.account.toSession())
        }
    }

    private fun AccountDto.toSession() = AuthSession(
        userId = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        isGuardian = isGuardian,
        lastMode = lastMode,
    )

    private companion object {
        const val TAG = "SessionRepository"
    }
}
