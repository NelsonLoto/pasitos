package com.pasitos.app.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import com.pasitos.app.core.database.LocalDatabase
import com.pasitos.app.core.network.FakeServer
import com.pasitos.app.core.network.FakeServer.Companion.bodyText
import com.pasitos.app.core.network.FakeServer.Companion.error
import com.pasitos.app.core.network.FakeServer.Companion.json
import com.pasitos.app.core.network.NoConnectionException
import com.pasitos.app.core.network.RefreshResult
import com.pasitos.app.core.network.createHttpClient
import com.pasitos.app.core.process.ProcessRestarter
import com.pasitos.app.core.security.DatabaseKeyStore
import com.pasitos.app.core.security.FakeCipher
import com.pasitos.app.core.sync.SyncScheduler
import com.pasitos.app.core.sync.SyncTrigger
import com.pasitos.app.core.sync.local.PendingMutationDao
import com.pasitos.app.core.sync.local.PendingMutationEntity
import com.pasitos.app.data.auth.AuthTestData.account
import com.pasitos.app.data.auth.AuthTestData.tokens
import com.pasitos.app.data.auth.local.SessionStore
import com.pasitos.app.data.auth.model.SessionState
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.auth.AuthResponse
import com.pasitos.shared.api.auth.RegisterRequest
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthApiRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cipher = FakeCipher()
    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create { File(folder.root, "secrets.preferences_pb") }
    }
    private val store by lazy { SessionStore(dataStore, cipher) }
    private val databaseKeyStore by lazy { DatabaseKeyStore(dataStore, cipher) }
    private val server = FakeServer()

    /** Lo que hizo la app, en orden: cancelar la sincronización y reiniciar. */
    private val events = mutableListOf<String>()
    private val pendingMutations = FakePendingMutationDao()

    private val session by lazy { SessionRepository(store) }
    private val repository by lazy {
        AuthApiRepository(
            httpClient = createHttpClient(server.engine, "http://localhost", session),
            sessionRepository = session,
            localDatabase = LocalDatabase(context, databaseKeyStore, name = "auth-test.db"),
            pendingMutationDao = pendingMutations,
            syncScheduler = object : SyncScheduler {
                override fun schedulePeriodic() = Unit
                override fun requestSync(trigger: SyncTrigger) = Unit
                override fun cancelAll() {
                    events += "cancelar sincronización"
                }
            },
            processRestarter = object : ProcessRestarter {
                override fun restart() {
                    events += "reiniciar"
                }
            },
        )
    }

    private fun serverSignsIn(
        accountId: String = account().id,
        path: String = "/api/v1/auth/login",
        status: HttpStatusCode = HttpStatusCode.OK,
    ) {
        server.routes[path] = {
            json(Json.encodeToString(AuthResponse(account(id = accountId), tokens(1))), status)
        }
    }

    @Test
    fun `RF-CTA-04 el inicio de sesión guarda la sesión y no manda token`() = runTest {
        serverSignsIn()

        val result = repository.login(" ana@pasitos.test ", "secreta123")

        assertEquals("Ana", result.getOrThrow().firstName)
        assertTrue(session.state.value is SessionState.SignedIn)
        assertEquals(tokens(1), store.read()?.tokens)
        val request = server.requests.single()
        assertNull(request.headers[HttpHeaders.Authorization])
        assertTrue(request.bodyText().contains("\"email\":\"ana@pasitos.test\""))
    }

    @Test
    fun `RN-CTA-05 con la contraseña equivocada muestra el mensaje del servidor y no reintenta`() = runTest {
        server.routes["/api/v1/auth/login"] = {
            error(HttpStatusCode.Unauthorized, ErrorCode.INVALID_CREDENTIALS, "Email o contraseña incorrectos")
        }

        val result = repository.login("ana@pasitos.test", "otraClave1")

        assertEquals("Email o contraseña incorrectos", result.exceptionOrNull()?.message)
        assertEquals(1, server.requests.size)
        assertEquals(SessionState.SignedOut(), session.state.value)
    }

    @Test
    fun `sin conexión el error lo explica`() = runTest {
        server.routes["/api/v1/auth/login"] = { throw IOException("sin red") }

        val result = repository.login("ana@pasitos.test", "secreta123")

        assertTrue(result.exceptionOrNull() is NoConnectionException)
    }

    @Test
    fun `RF-CTA-01 el registro manda nombre, apellido y la aceptación de los términos`() = runTest {
        serverSignsIn(path = "/api/v1/auth/register", status = HttpStatusCode.Created)

        val result = repository.register(" Ana ", "Pérez ", "ana@pasitos.test", "secreta123", acceptTerms = true)

        assertTrue(result.isSuccess)
        assertEquals(
            RegisterRequest("Ana", "Pérez", "ana@pasitos.test", "secreta123", acceptTerms = true),
            Json.decodeFromString<RegisterRequest>(server.requests.single().bodyText()),
        )
    }

    @Test
    fun `RF-CTA-07 CA1 cerrar sesión la revoca, borra tokens y base local, y reinicia la app`() = runTest {
        session.start(account(), tokens(1))
        databaseKeyStore.create()
        server.validAccessToken = "access-1"
        server.routes["/api/v1/auth/logout"] = { request ->
            assertEquals("Bearer access-1", request.headers[HttpHeaders.Authorization])
            respond("", HttpStatusCode.NoContent)
        }

        val result = repository.logout()

        assertTrue(result.isSuccess)
        assertEquals(1, server.requestsTo("/api/v1/auth/logout").size)
        assertEquals(listOf("cancelar sincronización", "reiniciar"), events)
        assertNull(store.read())
        assertNull(databaseKeyStore.read())
        assertEquals(SessionState.SignedOut(), session.state.value)
    }

    @Test
    fun `si el servidor ya había revocado la sesión igual se cierra en el teléfono`() = runTest {
        session.start(account(), tokens(1))

        val result = repository.logout()

        assertTrue(result.isSuccess)
        assertEquals(listOf("cancelar sincronización", "reiniciar"), events)
        assertNull(store.read())
    }

    @Test
    fun `sin conexión no se cierra la sesión`() = runTest {
        session.start(account(), tokens(1))
        databaseKeyStore.create()
        server.routes["/api/v1/auth/logout"] = { throw IOException("sin red") }

        val result = repository.logout()

        assertTrue(result.exceptionOrNull() is NoConnectionException)
        assertTrue(events.isEmpty())
        assertEquals(tokens(1), store.read()?.tokens)
        assertNotNull(databaseKeyStore.read())
    }

    @Test
    fun `si el servidor falla no se cierra la sesión`() = runTest {
        session.start(account(), tokens(1))
        server.validAccessToken = "access-1"
        server.routes["/api/v1/auth/logout"] = {
            error(HttpStatusCode.InternalServerError, ErrorCode.INTERNAL_ERROR, "Ocurrió un error, intentá de nuevo")
        }

        val result = repository.logout()

        assertEquals("Ocurrió un error, intentá de nuevo", result.exceptionOrNull()?.message)
        assertTrue(events.isEmpty())
        assertTrue(session.state.value is SessionState.SignedIn)
    }

    @Test
    fun `RF-CTA-07 CA2 informa los cambios que todavía no se sincronizaron`() = runTest {
        pendingMutations.count = 3

        assertEquals(3, repository.pendingChanges())
    }

    @Test
    fun `al iniciar sesión con otra cuenta después de vencer se borran los datos locales`() = runTest {
        session.start(account(id = "cuenta-anterior"), tokens(1))
        session.refresh("access-1") { RefreshResult.Rejected }
        databaseKeyStore.create()
        serverSignsIn(accountId = "cuenta-nueva")

        repository.login("otra@pasitos.test", "secreta123")

        assertEquals(listOf("cancelar sincronización", "reiniciar"), events)
        assertNull(databaseKeyStore.read())
        assertEquals("cuenta-nueva", store.read()?.account?.id)
    }

    @Test
    fun `al volver a iniciar sesión con la misma cuenta se conservan los datos locales`() = runTest {
        session.start(account(), tokens(1))
        session.refresh("access-1") { RefreshResult.Rejected }
        databaseKeyStore.create()
        serverSignsIn()

        repository.login("ana@pasitos.test", "secreta123")

        assertTrue(events.isEmpty())
        assertNotNull(databaseKeyStore.read())
        assertFalse(session.state.value is SessionState.SignedOut)
    }

    private class FakePendingMutationDao : PendingMutationDao {
        var count = 0
        override suspend fun insert(mutation: PendingMutationEntity) = Unit
        override suspend fun oldest(limit: Int): List<PendingMutationEntity> = emptyList()
        override suspend fun delete(mutationIds: List<String>) = Unit
        override suspend fun deleteForChild(childId: String) = Unit
        override suspend fun count(): Int = count
        override fun observeCount(): Flow<Int> = flowOf(count)
    }
}
