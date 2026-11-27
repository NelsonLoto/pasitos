package com.pasitos.app.data.auth

import com.pasitos.app.core.security.SessionStorage
import com.pasitos.app.core.security.StoredSession
import com.pasitos.app.data.auth.model.SessionState
import com.pasitos.app.data.auth.remote.AuthApi
import com.pasitos.app.data.auth.remote.AuthApiException
import com.pasitos.shared.api.auth.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

interface SessionEffects {
    suspend fun stopSync()
    suspend fun wipeDatabase()
    fun restart()
    fun scheduleRevocations()
}

@Singleton
class SessionRepository @Inject constructor(
    private val storage: SessionStorage,
    private val api: AuthApi,
    private val effects: SessionEffects,
    @Named("sessionScope") private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val ready = CompletableDeferred<Unit>()
    private var stored: StoredSession? = null
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Restoring)
    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    init {
        scope.launch {
            try {
                stored = storage.read()
                publish()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                // Sin identidad recuperable no se puede atribuir la base a una futura cuenta.
                effects.stopSync()
                storage.save(null)
                effects.wipeDatabase()
                effects.restart()
            } finally { ready.complete(Unit) }
            try { effects.scheduleRevocations() } catch (_: Exception) { /* Se reintenta al abrir la app. */ }
        }
        scope.launch {
            ready.await()
            storage.invalidations.collect {
                mutex.withLock {
                    if (stored != null && storage.read() == null) {
                        stored = stored?.copy(accessToken = null, refreshToken = null, simulated = false)
                        effects.stopSync(); mutableState.value = SessionState.SignedOut(expired = true)
                    }
                }
            }
        }
    }

    suspend fun tokens(): Pair<String, String>? {
        ready.await()
        return mutex.withLock { stored?.let { session -> session.accessToken?.let { access -> session.refreshToken?.let { access to it } } } }
    }

    suspend fun login(email: String, password: String): AccountResponse = accept { api.login(email, password) }
    suspend fun register(request: RegisterRequest): AccountResponse = accept(registration = true) { api.register(request) }

    suspend fun simulateGoogle(account: com.pasitos.app.data.auth.model.AuthSession) {
        ready.await()
        mutex.withLock {
            withContext(NonCancellable) {
                val previous = stored?.account?.id
                val value = AccountResponse(account.userId, account.email, account.displayName, "", false, false, true, AccountMode.GUARDIAN, "1", false)
                if (previous != account.userId) {
                    mutableState.value = SessionState.Restoring; effects.stopSync(); effects.wipeDatabase()
                }
                val next = StoredSession(value, null, null, simulated = true)
                storage.save(next); stored = next
                if (previous != null && previous != account.userId) effects.restart() else publish()
            }
        }
    }

    private suspend fun accept(registration: Boolean = false, request: suspend () -> AuthResponse): AccountResponse {
        ready.await()
        return mutex.withLock {
            val response = request()
            withContext(NonCancellable) {
                val different = stored?.account?.id?.let { it != response.account.id } == true
                // También limpiar una copia huérfana si el proceso murió entre borrar sesión y base.
                if (stored == null || different) { mutableState.value = SessionState.Restoring; effects.stopSync(); effects.wipeDatabase() }
                val next = StoredSession(response.account, response.accessToken, response.refreshToken)
                storage.save(next); stored = next
                if (different) effects.restart() else publish(registration)
            }
            response.account
        }
    }

    /** El token que recibió 401 se compara dentro del lock para no reutilizar una renovación rotada. */
    suspend fun refresh(rejectedAccess: String?): Pair<String, String>? {
        ready.await()
        return withContext(NonCancellable) {
            mutex.withLock {
                val current = stored ?: return@withLock null
                val access = current.accessToken ?: return@withLock null
                val refresh = current.refreshToken ?: return@withLock null
                if (rejectedAccess != null && rejectedAccess != access) return@withLock access to refresh
                try {
                    val response = api.refresh(refresh)
                    val next = StoredSession(response.account, response.accessToken, response.refreshToken)
                    storage.save(next); stored = next; publish()
                    response.accessToken to response.refreshToken
                } catch (failure: AuthApiException) {
                    if (failure.status == 401) {
                        // Conservar cuenta y datos pendientes; solo retirar permisos vencidos.
                        val expired = current.copy(accessToken = null, refreshToken = null)
                        storage.save(expired); stored = expired
                        effects.stopSync(); mutableState.value = SessionState.SignedOut(expired = true)
                    }
                    null
                } catch (_: Exception) { null } // Sin red o 5xx: no cerrar la sesión.
            }
        }
    }

    fun logout() { scope.launch { close() } }

    suspend fun close() {
        ready.await()
        withContext(NonCancellable) {
            mutex.withLock {
                mutableState.value = SessionState.Restoring
                val current = stored
                try {
                    if (current?.accessToken != null) {
                        try { api.logout(current.accessToken) }
                        catch (failure: AuthApiException) {
                            if (failure.status != 401 || current.refreshToken == null) throw failure
                            // El JWT puede haber vencido mientras la sesión de 30 días sigue activa.
                            try { api.logout(api.refresh(current.refreshToken).accessToken) }
                            catch (revocation: AuthApiException) { if (revocation.status != 401) throw revocation }
                        }
                    }
                } catch (_: Exception) {
                    current?.refreshToken?.let { token ->
                        try { storage.addRevocation(token); effects.scheduleRevocations() } catch (_: Exception) { /* El cierre local sigue. */ }
                    }
                }
                effects.stopSync()
                storage.save(null); stored = null
                effects.wipeDatabase()
                effects.restart()
            }
        }
    }

    private fun publish(registration: Boolean = (mutableState.value as? SessionState.SignedIn)?.needsOnboarding ?: false) {
        mutableState.value = stored?.takeIf { it.simulated || (it.accessToken != null && it.refreshToken != null) }
            ?.let { SessionState.SignedIn(it.account, registration) } ?: SessionState.SignedOut(expired = stored != null)
    }
}
