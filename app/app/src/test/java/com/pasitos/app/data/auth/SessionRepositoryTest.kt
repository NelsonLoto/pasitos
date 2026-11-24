package com.pasitos.app.data.auth

import com.pasitos.app.core.security.*
import com.pasitos.app.data.auth.model.SessionState
import com.pasitos.app.data.auth.remote.*
import com.pasitos.shared.api.auth.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class SessionRepositoryTest {
    private val account = AccountResponse("account-1", "test@example.test", "Familia", "Prueba", false, true, true, AccountMode.GUARDIAN, "1", false)
    private fun initial() = StoredSession(account, "access-0", "refresh-0")
    private fun response(id: String = account.id) = AuthResponse(account.copy(id = id), "access-1", "refresh-1", "2026-11-24T20:00:00Z")

    @Test fun `RF-CTA-06 restaura sin pedir red`() = runBlocking {
        val store = MemoryStore(initial()); val api = Api(); val effects = Effects()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repository = SessionRepository(store, api, effects, scope)
            assertEquals("access-0" to "refresh-0", repository.tokens())
            assertTrue(repository.state.value is SessionState.SignedIn)
            assertEquals(0, api.refreshCount)
        } finally { scope.cancel() }
    }

    @Test fun `RF-CTA-06 veinte rechazos simultaneos renuevan solo una vez`() = runBlocking {
        val api = Api(); val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repository = SessionRepository(MemoryStore(initial()), api, Effects(), scope)
            val values = coroutineScope { List(20) { async(Dispatchers.Default) { repository.refresh("access-0") } }.awaitAll() }
            assertTrue(values.all { it == "access-1" to "refresh-1" })
            assertEquals(1, api.refreshCount)
        } finally { scope.cancel() }
    }

    @Test fun `RF-CTA-06 cancelar el pedido no descarta tokens ya rotados`() = runBlocking {
        val api = Api(); val gate = CompletableDeferred<Unit>(); api.gate = gate
        val store = MemoryStore(initial()); val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repository = SessionRepository(store, api, Effects(), scope)
            val request = launch { repository.refresh("access-0") }
            api.started.await(); request.cancel(); gate.complete(Unit); request.join()
            assertEquals("refresh-1", store.value!!.refreshToken)
        } finally { scope.cancel() }
    }

    @Test fun `RF-CTA-06 rechazo 401 borra permisos y conserva cuenta y base`() = runBlocking {
        val api = Api(); api.failure = AuthApiException(401, "Venció")
        val store = MemoryStore(initial()); val effects = Effects(); val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repository = SessionRepository(store, api, effects, scope)
            assertNull(repository.refresh("access-0"))
            assertEquals(SessionState.SignedOut(expired = true), repository.state.value)
            assertEquals(account, store.value!!.account); assertNull(store.value!!.accessToken); assertNull(store.value!!.refreshToken)
            assertEquals(listOf("cancel"), effects.events)
        } finally { scope.cancel() }
    }

    @Test fun `RF-CTA-06 error transitorio no cierra sesion`() = runBlocking {
        for (failure in listOf(IOException("offline"), AuthApiException(500, "Temporal"))) {
            val api = Api(); api.failure = failure
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val repository = SessionRepository(MemoryStore(initial()), api, Effects(), scope)
                assertNull(repository.refresh("access-0"))
                assertEquals("access-0" to "refresh-0", repository.tokens())
                assertTrue(repository.state.value is SessionState.SignedIn)
            } finally { scope.cancel() }
        }
    }

    @Test fun `RF-CTA-07 logout offline guarda revocacion y termina limpieza`() = runBlocking {
        val api = Api(); api.logoutFailure = IOException("offline")
        val store = MemoryStore(initial()); val effects = Effects(); val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repository = SessionRepository(store, api, effects, scope)
            repository.close()
            assertNull(store.value); assertEquals(setOf("refresh-0"), store.pending)
            assertEquals(listOf("cancel", "wipe", "restart"), effects.events)
        } finally { scope.cancel() }
    }

    @Test fun `RF-CTA-06 misma cuenta conserva datos y otra cuenta limpia antes de mostrarse`() = runBlocking {
        for (id in listOf(account.id, "another-account")) {
            val store = MemoryStore(initial().copy(accessToken = null, refreshToken = null))
            val api = Api(); api.accountId = id
            val effects = Effects(); val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val repository = SessionRepository(store, api, effects, scope)
                repository.login("test@example.test", "Synthetic123")
                if (id == account.id) { assertTrue(effects.events.isEmpty()); assertTrue(repository.state.value is SessionState.SignedIn) }
                else { assertEquals(listOf("cancel", "wipe", "restart"), effects.events); assertEquals(SessionState.Restoring, repository.state.value) }
            } finally { scope.cancel() }
        }
    }

    @Test fun `RF-CTA-07 JWT vencido se renueva para revocar tambien su sesion`() = runBlocking {
        val api = Api(); api.logoutFailure = AuthApiException(401, "JWT vencido")
        val effects = Effects(); val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            SessionRepository(MemoryStore(initial()), api, effects, scope).close()
            assertEquals(1, api.refreshCount); assertEquals(2, api.logoutCount)
            assertEquals(listOf("cancel", "wipe", "restart"), effects.events)
        } finally { scope.cancel() }
    }

    @Test fun `RF-CTA-07 fallo al guardar revocacion no impide borrar sesion local`() = runBlocking {
        val raw = MemoryStore(initial())
        val store = object : SessionStorage by raw {
            override suspend fun addRevocation(token: String) { throw IOException("storage unavailable") }
        }
        val api = Api(); api.logoutFailure = IOException("offline")
        val effects = Effects(); val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            SessionRepository(store, api, effects, scope).close()
            assertNull(raw.value); assertEquals(listOf("cancel", "wipe", "restart"), effects.events)
        } finally { scope.cancel() }
    }

    @Test fun `RF-CTA-06 no publica login ni inicio mientras restaura`() = runBlocking {
        val readGate = CompletableDeferred<Unit>()
        val store = object : SessionStorage by MemoryStore(initial()) {
            override suspend fun read(): StoredSession { readGate.await(); return initial() }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repository = SessionRepository(store, Api(), Effects(), scope)
            assertEquals(SessionState.Restoring, repository.state.value)
            readGate.complete(Unit)
            assertTrue(repository.state.first { it !is SessionState.Restoring } is SessionState.SignedIn)
        } finally { scope.cancel() }
    }

    @Test fun `RNF-SEG sesion ilegible no permite reutilizar la base sin saber de quien es`() = runBlocking {
        val raw = MemoryStore(initial())
        val store = object : SessionStorage by raw {
            override suspend fun read(): StoredSession? { throw IOException("corrupt session") }
        }
        val effects = Effects(); val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repository = SessionRepository(store, Api(), effects, scope)
            assertNull(repository.tokens()); assertNull(raw.value)
            assertEquals(listOf("cancel", "wipe", "restart"), effects.events)
            assertEquals(SessionState.Restoring, repository.state.value)
        } finally { scope.cancel() }
    }

    @Test fun `RNF-SEG primer ingreso limpia cualquier base huerfana sin identidad`() = runBlocking {
        val effects = Effects(); val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repository = SessionRepository(MemoryStore(null), Api(), effects, scope)
            repository.login("test@example.test", "Synthetic123")
            assertEquals(listOf("cancel", "wipe"), effects.events)
            assertTrue(repository.state.value is SessionState.SignedIn)
        } finally { scope.cancel() }
    }

    inner class Api : AuthApi {
        var refreshCount = 0; var logoutCount = 0; var accountId = account.id
        var failure: Exception? = null; var logoutFailure: Exception? = null
        val started = CompletableDeferred<Unit>(); var gate: CompletableDeferred<Unit>? = null
        override suspend fun login(email: String, password: String) = response(accountId)
        override suspend fun register(request: RegisterRequest) = response()
        override suspend fun refresh(token: String): AuthResponse {
            refreshCount++; started.complete(Unit); gate?.await(); failure?.let { throw it }; delay(10); return response()
        }
        override suspend fun logout(access: String) { logoutCount++; if (logoutCount == 1) logoutFailure?.let { throw it } }
    }
    class MemoryStore(var value: StoredSession?) : SessionStorage {
        var pending = emptySet<String>()
        override suspend fun read() = value
        override suspend fun save(value: StoredSession?) { this.value = value }
        override suspend fun pendingRevocations() = pending
        override suspend fun addRevocation(token: String) { pending += token }
        override suspend fun removeRevocation(token: String) { pending -= token }
    }
    class Effects : SessionEffects {
        val events = mutableListOf<String>()
        override suspend fun stopSync() { events += "cancel" }
        override suspend fun wipeDatabase() { events += "wipe" }
        override fun restart() { events += "restart" }
        override fun scheduleRevocations() {}
    }
}
