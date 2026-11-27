package com.pasitos.app.data.auth

import com.pasitos.app.core.network.backendConfiguration
import com.pasitos.app.core.network.withoutSession
import com.pasitos.app.core.security.StoredSession
import com.pasitos.app.data.auth.remote.HttpAuthApi
import com.pasitos.shared.api.auth.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class BackendClientTest {
    private val fixtures = SessionRepositoryTest()
    private fun account() = AccountResponse("id", "test@example.test", "Test", "Account", false, true, true, AccountMode.GUARDIAN, "1", false)

    @Test fun `RF-CTA-06 plugin renueva ante 401 y repite con el nuevo permiso`() = runBlocking {
        val api = fixtures.Api(); val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val sessions = SessionRepository(SessionRepositoryTest.MemoryStore(StoredSession(account(), "access-0", "refresh-0")), api, SessionRepositoryTest.Effects(), scope)
        val seen = mutableListOf<String?>()
        val engine = MockEngine { request ->
            val header = request.headers[HttpHeaders.Authorization]; seen += header
            respond("{}", if (header == "Bearer access-1") HttpStatusCode.OK else HttpStatusCode.Unauthorized)
        }
        val client = HttpClient(engine) { backendConfiguration("http://localhost:8080/", sessions) }
        try {
            assertEquals(HttpStatusCode.OK, client.get("api/v1/me").status)
            assertEquals(listOf("Bearer access-0", "Bearer access-1"), seen)
            assertEquals(1, api.refreshCount)
        } finally { client.close(); scope.cancel() }
    }

    @Test fun `RN-CTA-05 login incorrecto no envia token ni duplica el intento`() = runBlocking {
        val api = fixtures.Api(); val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val sessions = SessionRepository(SessionRepositoryTest.MemoryStore(StoredSession(account(), "access-0", "refresh-0")), api, SessionRepositoryTest.Effects(), scope)
        var count = 0
        val client = HttpClient(MockEngine { request -> count++; assertNull(request.headers[HttpHeaders.Authorization]); respond("{}", HttpStatusCode.Unauthorized) }) {
            backendConfiguration("http://localhost:8080/", sessions)
        }
        try {
            assertEquals(HttpStatusCode.Unauthorized, client.post("api/v1/auth/login") { withoutSession() }.status)
            assertEquals(1, count); assertEquals(0, api.refreshCount)
        } finally { client.close(); scope.cancel() }
    }

    @Test fun `RNF-SEG un origen ajeno que pide autenticacion nunca recibe el token`() = runBlocking {
        val api = fixtures.Api(); val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val sessions = SessionRepository(SessionRepositoryTest.MemoryStore(StoredSession(account(), "access-0", "refresh-0")), api, SessionRepositoryTest.Effects(), scope)
        var count = 0
        val client = HttpClient(MockEngine { request -> count++; assertNull(request.headers[HttpHeaders.Authorization]); respond("", HttpStatusCode.Unauthorized) }) {
            backendConfiguration("http://localhost:8080/", sessions)
        }
        try {
            assertEquals(HttpStatusCode.Unauthorized, client.get("https://other.invalid/api/v1/me").status)
            assertEquals(1, count); assertEquals(0, api.refreshCount)
        } finally { client.close(); scope.cancel() }
    }

    @Test fun `RF-CTA-06 cliente no conserva permisos de una sesion cerrada`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val sessions = SessionRepository(SessionRepositoryTest.MemoryStore(StoredSession(account(), "access-0", "refresh-0")), fixtures.Api(), SessionRepositoryTest.Effects(), scope)
        val seen = mutableListOf<String?>()
        val client = HttpClient(MockEngine { request -> seen += request.headers[HttpHeaders.Authorization]; respond("{}", HttpStatusCode.OK) }) {
            backendConfiguration("http://localhost:8080/", sessions)
        }
        try {
            client.get("api/v1/me"); sessions.close(); client.get("api/v1/me")
            assertEquals(listOf("Bearer access-0", null), seen)
        } finally { client.close(); scope.cancel() }
    }
}
