package com.pasitos.app.core.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.pasitos.app.core.network.FakeServer.Companion.bodyText
import com.pasitos.app.core.security.FakeCipher
import com.pasitos.app.data.auth.AuthTestData.account
import com.pasitos.app.data.auth.AuthTestData.tokens
import com.pasitos.app.data.auth.SessionRepository
import com.pasitos.app.data.auth.local.SessionStore
import com.pasitos.app.data.auth.model.SessionState
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HttpClientTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create { File(folder.root, "secrets.preferences_pb") }
    }
    private val store by lazy { SessionStore(dataStore, FakeCipher()) }
    private val server = FakeServer()

    /** Sesión iniciada con el par 1. El servidor ya venció ese token de acceso y renueva con el par 2. */
    private suspend fun signedInWithExpiredAccessToken(): Pair<SessionRepository, HttpClient> {
        val session = SessionRepository(store)
        session.start(account(), tokens(1))
        server.validAccessToken = "otro-token"
        server.refreshable = "refresh-1" to tokens(2)
        return session to createHttpClient(server.engine, "http://localhost", session)
    }

    @Test
    fun `cada pedido lleva el token de acceso de la sesión`() = runTest {
        val session = SessionRepository(store).apply { start(account(), tokens(1)) }
        server.validAccessToken = "access-1"
        val client = createHttpClient(server.engine, "http://localhost", session)

        val response = client.get("/children")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Bearer access-1", server.requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `I0-06 CA2 con el token de acceso vencido lo renueva solo y repite el pedido`() = runTest {
        val (session, client) = signedInWithExpiredAccessToken()

        val response = client.get("/children")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listOf("/children", "/api/v1/auth/refresh", "/children"), server.requests.map { it.url.encodedPath })
        assertEquals("Bearer access-2", server.requests.last().headers[HttpHeaders.Authorization])
        assertEquals(tokens(2), session.current())
        assertEquals(tokens(2), store.read()?.tokens)
    }

    @Test
    fun `la renovación manda solo el token de renovación`() = runTest {
        val (_, client) = signedInWithExpiredAccessToken()

        client.get("/children")

        val refresh = server.requestsTo("/api/v1/auth/refresh").single()
        assertNull(refresh.headers[HttpHeaders.Authorization])
        assertTrue(refresh.bodyText().contains("refresh-1"))
    }

    @Test
    fun `si varios pedidos fallan a la vez renueva una sola vez`() = runTest {
        val (_, client) = signedInWithExpiredAccessToken()
        val rejected = server.requests
        val bothRejected = CompletableDeferred<Unit>()
        // La renovación espera a que el segundo pedido también haya fallado con el token viejo
        server.beforeRefresh = {
            withTimeout(5_000) {
                while (rejected.count { it.url.encodedPath == "/children" } < 2) delay(10)
            }
            bothRejected.complete(Unit)
        }

        val responses = List(2) { async { client.get("/children") } }.awaitAll()

        assertTrue(bothRejected.isCompleted)
        assertEquals(listOf(HttpStatusCode.OK, HttpStatusCode.OK), responses.map { it.status })
        assertEquals(1, server.requestsTo("/api/v1/auth/refresh").size)
    }

    @Test
    fun `RF-CTA-06 CA2 si el servidor rechaza la renovación la sesión vence`() = runTest {
        val (session, client) = signedInWithExpiredAccessToken()
        server.refreshable = null

        val response = client.get("/children")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(SessionState.SignedOut(expired = true), session.state.value)
        assertNull(store.read()?.tokens)
    }

    @Test
    fun `si se cancela el pedido durante la renovación igual guarda los tokens nuevos`() = runTest {
        val (session, client) = signedInWithExpiredAccessToken()
        val refreshStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        server.beforeRefresh = {
            refreshStarted.complete(Unit)
            release.await()
        }

        val request = launch { client.get("/children") }
        refreshStarted.await()
        request.cancel()
        release.complete(Unit)
        request.join()

        // La renovación termina en otro hilo: se espera en tiempo real
        withContext(Dispatchers.IO) {
            withTimeout(5_000) { while (store.read()?.tokens != tokens(2)) delay(10) }
        }
        assertEquals(tokens(2), session.current())
    }

    @Test
    fun `sin sesión los pedidos van sin token y no intenta renovar`() = runTest {
        val session = SessionRepository(store)
        val client = createHttpClient(server.engine, "http://localhost", session)

        val response = client.get("/children")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertNull(server.requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `RN-CTA-05 un login rechazado no se repite ni dispara una renovación`() = runTest {
        val (_, client) = signedInWithExpiredAccessToken()

        val response = client.post("/api/v1/auth/login") { withoutSession() }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(listOf("/api/v1/auth/login"), server.requests.map { it.url.encodedPath })
        assertNull(server.requests.single().headers[HttpHeaders.Authorization])
    }
}
