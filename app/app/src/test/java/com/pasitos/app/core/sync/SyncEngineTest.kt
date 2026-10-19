package com.pasitos.app.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pasitos.app.core.database.PasitosDatabase
import com.pasitos.app.core.network.ApiException
import com.pasitos.app.core.network.FakeServer
import com.pasitos.app.core.network.FakeServer.Companion.error
import com.pasitos.app.core.network.FakeServer.Companion.json
import com.pasitos.app.core.network.NoConnectionException
import com.pasitos.app.core.network.createHttpClient
import com.pasitos.app.core.sync.local.MutationEntity
import com.pasitos.app.core.sync.local.MutationOp
import com.pasitos.app.core.sync.local.PendingMutationEntity
import com.pasitos.app.data.child.ChildSyncedEntity
import com.pasitos.app.data.child.ChildTestData
import com.pasitos.app.data.child.ChildTestData.SOFIA
import com.pasitos.app.data.child.ChildTestData.TOMAS
import com.pasitos.app.data.child.ChildTestData.childRow
import com.pasitos.app.data.child.ChildTestData.guardianshipRow
import com.pasitos.app.data.child.FixedSessionTokens
import com.pasitos.app.data.child.GuardianshipSyncedEntity
import com.pasitos.app.data.child.model.GuardianRole
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.sync.SyncChangesResponse
import com.pasitos.shared.api.sync.SyncSnapshotResponse
import io.ktor.http.HttpStatusCode
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncEngineTest {

    private lateinit var database: PasitosDatabase
    private val server = FakeServer(validAccessToken = "access-1")

    /** Otra entidad de otro dominio, para comprobar que el motor la trata igual que a las de hijos. */
    private val other = RecordingEntity("MEASUREMENT")

    private val engine by lazy {
        SyncEngine(
            httpClient = createHttpClient(server.engine, "http://localhost", FixedSessionTokens()),
            database = database,
            syncStateDao = database.syncStateDao(),
            pendingMutationDao = database.pendingMutationDao(),
            entities = setOf(ChildSyncedEntity(database.childDao()), GuardianshipSyncedEntity(database.childDao()), other),
            clock = ChildTestData.clock,
        )
    }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), PasitosDatabase::class.java)
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `trae hijos y tutorías, guarda el cursor y la hora de la sincronización`() = runTest {
        respondChanges(page(cursor = 12, "CHILD" to listOf(childRow()), "GUARDIANSHIP" to listOf(guardianshipRow())))

        val result = engine.sync()

        val child = database.childDao().observeAll().first().single()
        assertEquals("Sofía", child.firstName)
        assertEquals(LocalDate.of(2026, 3, 15), child.birthDate)
        assertEquals(38, child.gestationalWeeks)
        assertEquals("rabbit", child.avatarKey)
        assertEquals(GuardianRole.PRINCIPAL, database.childDao().observeRole(SOFIA, ChildTestData.ACCOUNT_ID).first())
        assertEquals(12L, database.syncStateDao().get()?.cursor)
        assertEquals(Instant.parse("2026-10-19T15:00:00Z"), database.syncStateDao().get()?.lastSyncedAt)
        assertEquals(SyncResult(received = 2, skipped = 0), result)
    }

    @Test
    fun `pide desde el último cursor y sigue mientras haya más páginas`() = runTest {
        database.syncStateDao().save(com.pasitos.app.core.sync.local.SyncStateEntity(cursor = 7))
        val pages = ArrayDeque(
            listOf(
                page(cursor = 20, "CHILD" to listOf(childRow(SOFIA)), hasMore = true),
                page(cursor = 25, "CHILD" to listOf(childRow(TOMAS, firstName = "Tomás"))),
            ),
        )
        server.routes["/api/v1/sync/changes"] = { json(encode(pages.removeFirst())) }

        engine.sync()

        val cursors = server.requestsTo("/api/v1/sync/changes").map { it.url.parameters["cursor"] }
        assertEquals(listOf("7", "20"), cursors)
        assertEquals(2, database.childDao().observeAll().first().size)
        assertEquals(25L, database.syncStateDao().get()?.cursor)
    }

    @Test
    fun `un niño nuevo en el alcance llega con su foto completa`() = runTest {
        respondChanges(page(cursor = 30, newChildIds = listOf(SOFIA)))
        server.routes["/api/v1/sync/snapshot/$SOFIA"] = {
            json(Json.encodeToString(SyncSnapshotResponse(cursor = 28, changes = mapOf("CHILD" to listOf(childRow())))))
        }

        engine.sync()

        assertEquals(listOf(SOFIA), database.childDao().observeAll().first().map { it.id })
        assertEquals(30L, database.syncStateDao().get()?.cursor)
    }

    @Test
    fun `RNF-OFF-05 al perder el acceso se borran los datos del niño en todas las entidades y sus cambios pendientes`() = runTest {
        respondChanges(page(cursor = 5, "CHILD" to listOf(childRow(SOFIA), childRow(TOMAS)), "GUARDIANSHIP" to listOf(guardianshipRow(SOFIA))))
        engine.sync()
        database.pendingMutationDao().insert(pendingMutation(SOFIA))
        database.pendingMutationDao().insert(pendingMutation(TOMAS))

        respondChanges(page(cursor = 9, revokedChildIds = listOf(SOFIA)))
        engine.sync()

        assertEquals(listOf(TOMAS), database.childDao().observeAll().first().map { it.id })
        assertNull(database.childDao().observeRole(SOFIA, ChildTestData.ACCOUNT_ID).first())
        assertEquals(listOf(TOMAS), database.pendingMutationDao().oldest(10).map { it.childId })
        assertEquals(listOf(SOFIA), other.deletedChildren)
    }

    @Test
    fun `RN-HIJ-08 la lápida de un niño borra sus datos en todas las entidades`() = runTest {
        respondChanges(page(cursor = 5, "CHILD" to listOf(childRow(SOFIA))))
        engine.sync()

        respondChanges(page(cursor = 8, "CHILD" to listOf(childRow(SOFIA, deleted = true))))
        engine.sync()

        assertTrue(database.childDao().observeAll().first().isEmpty())
        assertEquals(listOf(SOFIA), other.deletedChildren)
    }

    @Test
    fun `una fila ilegible se saltea sin frenar las demás`() = runTest {
        val unreadable = buildJsonObject { put("id", TOMAS) }
        val badDate = childRow(TOMAS, birthDate = "15/03/2026")
        respondChanges(page(cursor = 3, "CHILD" to listOf(unreadable, badDate, childRow(SOFIA))))

        val result = engine.sync()

        assertEquals(listOf(SOFIA), database.childDao().observeAll().first().map { it.id })
        assertEquals(SyncResult(received = 1, skipped = 2), result)
        assertEquals(3L, database.syncStateDao().get()?.cursor)
    }

    @Test
    fun `una entidad sin manejador se ignora`() = runTest {
        respondChanges(page(cursor = 4, "HABIT" to listOf(buildJsonObject { put("id", "h1") }), "CHILD" to listOf(childRow())))

        val result = engine.sync()

        assertEquals(SyncResult(received = 1, skipped = 0), result)
        assertEquals(4L, database.syncStateDao().get()?.cursor)
    }

    @Test
    fun `si falla la foto de un niño no se guarda la página y el cursor no avanza`() = runTest {
        respondChanges(page(cursor = 30, "CHILD" to listOf(childRow(TOMAS)), newChildIds = listOf(SOFIA)))
        server.routes["/api/v1/sync/snapshot/$SOFIA"] = { error(HttpStatusCode.InternalServerError, ErrorCode.UNKNOWN, "Falló") }

        try {
            engine.sync()
            fail("Tenía que fallar")
        } catch (_: ApiException) {
        }

        assertTrue(database.childDao().observeAll().first().isEmpty())
        assertNull(database.syncStateDao().get())
    }

    @Test
    fun `si el niño ya no está al pedir su foto, se sigue sin él`() = runTest {
        respondChanges(page(cursor = 30, newChildIds = listOf(SOFIA)))
        server.routes["/api/v1/sync/snapshot/$SOFIA"] = { error(HttpStatusCode.NotFound, ErrorCode.CHILD_NOT_FOUND, "No existe") }

        engine.sync()

        assertTrue(database.childDao().observeAll().first().isEmpty())
        assertEquals(30L, database.syncStateDao().get()?.cursor)
    }

    @Test
    fun `sin red avisa NoConnectionException`() = runTest {
        server.routes["/api/v1/sync/changes"] = { throw IOException("sin red") }

        try {
            engine.sync()
            fail("Tenía que fallar")
        } catch (_: NoConnectionException) {
        }
    }

    @Test
    fun `una sincronización espera a que termine lo que se guarda con withLock`() = runTest {
        respondChanges(page(cursor = 1))
        val release = CompletableDeferred<Unit>()
        val writing = launch { engine.withLock { release.await() } }
        testScheduler.advanceUntilIdle()

        val syncing = async { engine.sync() }
        testScheduler.advanceUntilIdle()
        assertTrue(server.requestsTo("/api/v1/sync/changes").isEmpty())

        release.complete(Unit)
        writing.join()
        syncing.await()
        assertEquals(1, server.requestsTo("/api/v1/sync/changes").size)
    }

    private fun respondChanges(response: SyncChangesResponse) {
        server.routes["/api/v1/sync/changes"] = { json(encode(response)) }
    }

    private fun page(
        cursor: Long,
        vararg changes: Pair<String, List<JsonObject>>,
        hasMore: Boolean = false,
        newChildIds: List<String> = emptyList(),
        revokedChildIds: List<String> = emptyList(),
    ) = SyncChangesResponse(
        cursor = cursor,
        hasMore = hasMore,
        changes = changes.associate { (name, rows) -> name to rows.map<JsonObject, JsonElement> { it } },
        newChildIds = newChildIds,
        revokedChildIds = revokedChildIds,
    )

    private fun encode(response: SyncChangesResponse) = Json.encodeToString(response)

    private fun pendingMutation(childId: String) = PendingMutationEntity(
        mutationId = "m-$childId",
        entity = MutationEntity.MEASUREMENT,
        entityId = "e-$childId",
        childId = childId,
        op = MutationOp.UPSERT,
        payload = "{}",
        createdAt = Instant.parse("2026-10-19T12:00:00Z"),
    )

    private class RecordingEntity(override val name: String) : SyncedEntity {
        val deletedChildren = mutableListOf<String>()

        override suspend fun apply(row: JsonObject) = Unit

        override suspend fun deleteChild(childId: String) {
            deletedChildren += childId
        }
    }
}
