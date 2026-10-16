package com.pasitos.app.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.pasitos.app.core.network.FakeServer
import com.pasitos.app.core.network.FakeServer.Companion.bodyText
import com.pasitos.app.core.network.FakeServer.Companion.error
import com.pasitos.app.core.network.createHttpClient
import com.pasitos.app.core.security.FakeCipher
import com.pasitos.app.data.auth.AuthTestData.tokens
import com.pasitos.app.data.auth.local.SessionStore
import com.pasitos.shared.api.ErrorCode
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** RF-CTA-07, CA3: revocar en el servidor una sesión cerrada sin conexión. */
@RunWith(RobolectricTestRunner::class)
class SessionRevocationTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create { File(folder.root, "secrets.preferences_pb") }
    }
    private val store by lazy { SessionStore(dataStore, FakeCipher()) }
    private val server = FakeServer()

    @Before
    fun setUp() {
        val configuration = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
    }

    @After
    fun tearDown() {
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    private suspend fun runWorker(): ListenableWorker.Result {
        val client = createHttpClient(server.engine, "http://localhost", SessionRepository(store))
        val worker = TestListenableWorkerBuilder<RevokeSessionsWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    RevokeSessionsWorker(appContext, workerParameters, client, store)
            })
            .build()
        return worker.doWork()
    }

    @Test
    fun `programarla guarda el token y espera a que haya red`() = runTest {
        WorkManagerSessionRevocation(context, store).schedule("refresh-1")

        assertEquals(listOf("refresh-1"), store.pendingRevocations())
        val work = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WorkManagerSessionRevocation.WORK_NAME).get().single()
        assertEquals(WorkInfo.State.ENQUEUED, work.state)
        assertEquals(NetworkType.CONNECTED, work.constraints.requiredNetworkType)
    }

    @Test
    fun `con red renueva con el token guardado y cierra la sesión con el de acceso nuevo`() = runTest {
        store.addPendingRevocation("refresh-1")
        server.refreshable = "refresh-1" to tokens(2)
        server.routes["/api/v1/auth/logout"] = { request ->
            assertEquals("Bearer access-2", request.headers[HttpHeaders.Authorization])
            respond("", HttpStatusCode.NoContent)
        }

        val result = runWorker()

        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(server.requestsTo("/api/v1/auth/refresh").single().bodyText().contains("refresh-1"))
        assertEquals(1, server.requestsTo("/api/v1/auth/logout").size)
        assertTrue(store.pendingRevocations().isEmpty())
    }

    @Test
    fun `si la sesión ya no existe en el servidor no hay nada más que hacer`() = runTest {
        store.addPendingRevocation("refresh-1")

        val result = runWorker()

        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(server.requestsTo("/api/v1/auth/logout").isEmpty())
        assertTrue(store.pendingRevocations().isEmpty())
    }

    @Test
    fun `si se corta la red la conserva y lo vuelve a intentar`() = runTest {
        store.addPendingRevocation("refresh-1")
        server.routes["/api/v1/auth/refresh"] = { throw IOException("sin red") }

        val result = runWorker()

        assertEquals(ListenableWorker.Result.retry(), result)
        assertEquals(listOf("refresh-1"), store.pendingRevocations())
    }

    @Test
    fun `si el servidor falla la conserva y lo vuelve a intentar`() = runTest {
        store.addPendingRevocation("refresh-1")
        server.routes["/api/v1/auth/refresh"] = {
            error(HttpStatusCode.InternalServerError, ErrorCode.INTERNAL_ERROR, "Ocurrió un error, intentá de nuevo")
        }

        val result = runWorker()

        assertEquals(ListenableWorker.Result.retry(), result)
        assertEquals(listOf("refresh-1"), store.pendingRevocations())
    }

    @Test
    fun `si el cierre falla después de renovar, el reintento con el token ya rotado termina`() = runTest {
        store.addPendingRevocation("refresh-1")
        server.refreshable = "refresh-1" to tokens(2)
        server.routes["/api/v1/auth/logout"] = {
            error(HttpStatusCode.InternalServerError, ErrorCode.INTERNAL_ERROR, "Ocurrió un error, intentá de nuevo")
        }

        assertEquals(ListenableWorker.Result.retry(), runWorker())
        // El backend real revoca la sesión al recibir un token ya rotado y responde 401
        assertEquals(ListenableWorker.Result.success(), runWorker())
        assertTrue(store.pendingRevocations().isEmpty())
    }

    @Test
    fun `cada sesión pendiente se revoca por separado`() = runTest {
        store.addPendingRevocation("refresh-vieja")
        store.addPendingRevocation("refresh-1")
        server.refreshable = "refresh-1" to tokens(2)
        server.routes["/api/v1/auth/logout"] = { respond("", HttpStatusCode.NoContent) }

        val result = runWorker()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(2, server.requestsTo("/api/v1/auth/refresh").size)
        assertTrue(store.pendingRevocations().isEmpty())
    }
}
