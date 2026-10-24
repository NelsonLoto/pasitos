package com.pasitos.app.data.measurement

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pasitos.app.core.database.PasitosDatabase
import com.pasitos.app.core.sync.SyncState
import com.pasitos.app.core.sync.SyncTrigger
import com.pasitos.app.core.sync.local.MutationOp
import com.pasitos.app.core.sync.local.PendingMutationEntity
import com.pasitos.app.data.child.ChildTestData
import com.pasitos.app.data.child.ChildTestData.SOFIA
import com.pasitos.app.data.child.RecordingSyncScheduler
import com.pasitos.app.data.child.SignedInAuthRepository
import com.pasitos.app.data.measurement.model.LengthPosition
import com.pasitos.app.data.measurement.model.MeasurementInput
import com.pasitos.shared.api.ErrorCode
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Mediciones en Room y en la bandeja de salida (RF-CRE-01, RF-CRE-02, RNF-OFF-02, 14 · Sincronización §3). */
@RunWith(RobolectricTestRunner::class)
class RoomMeasurementRepositoryTest {

    private lateinit var database: PasitosDatabase
    private val scheduler = RecordingSyncScheduler()

    private val repository by lazy {
        RoomMeasurementRepository(
            database = database,
            dao = database.measurementDao(),
            pendingMutationDao = database.pendingMutationDao(),
            syncScheduler = scheduler,
            authRepository = SignedInAuthRepository(),
            clock = ChildTestData.clock,
        )
    }
    private val synced by lazy { MeasurementSyncedEntity(database.measurementDao(), database.pendingMutationDao()) }

    private val input = MeasurementInput(
        date = LocalDate.of(2026, 10, 14),
        weightKg = 7.25,
        lengthCm = 66.5,
        lengthPosition = LengthPosition.LYING,
        headCircumferenceCm = null,
    )

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
    fun `RNF-OFF-02 una medición nueva queda pendiente en el teléfono y en la bandeja, y pide sincronizar`() = runTest {
        val created = repository.addMeasurement(SOFIA, input).getOrThrow()

        val stored = repository.observeMeasurements(SOFIA).first().single()
        assertEquals(SyncState.PENDING, stored.syncState)
        assertEquals(ChildTestData.ACCOUNT_ID, stored.authorAccountId)
        val mutation = outbox().single()
        assertEquals(MutationOp.UPSERT, mutation.op)
        assertEquals(created.id, mutation.entityId)
        assertEquals(SOFIA, mutation.childId)
        val data = Json.parseToJsonElement(mutation.payload).jsonObject
        assertEquals("2026-10-14", data.text("measuredOn"))
        assertEquals("LYING", data.text("lengthPosition"))
        assertFalse("sin confirmar un atípico no viaja: el backend lo toma como falso", "outlierConfirmed" in data)
        assertFalse("un valor sin cargar no viaja", "headCircumferenceCm" in data)
        assertFalse("el autor lo pone el servidor", "authorAccountId" in data)
        assertEquals(listOf(SyncTrigger.LOCAL_CHANGE), scheduler.requests)
    }

    @Test
    fun `RN-CRE-12 una segunda medición propia en la misma fecha falla con la que ya existe`() = runTest {
        val first = repository.addMeasurement(SOFIA, input).getOrThrow()

        val error = repository.addMeasurement(SOFIA, input.copy(weightKg = 7.3)).exceptionOrNull()

        assertEquals(first.id, (error as DuplicateMeasurementException).existingId)
        assertEquals(1, repository.observeMeasurements(SOFIA).first().size)
        assertEquals(first.id, repository.findOwnOn(SOFIA, input.date)?.id)
        assertNull(repository.findOwnOn(SOFIA, input.date, excludeId = first.id))
    }

    @Test
    fun `aplicada por el servidor queda sincronizada, pero si se editó mientras se enviaba sigue pendiente`() = runTest {
        val created = repository.addMeasurement(SOFIA, input).getOrThrow()
        repository.updateMeasurement(created.id, input.copy(weightKg = 7.3)).getOrThrow()
        val (first, second) = outbox()

        answer(first) { synced.onApplied(it) }
        assertEquals(SyncState.PENDING, measurement(created.id)?.syncState)

        answer(second) { synced.onApplied(it) }
        assertEquals(SyncState.SYNCED, measurement(created.id)?.syncState)
        assertEquals(7.3, measurement(created.id)?.weightKg)
    }

    @Test
    fun `un rechazo deja el motivo, y descartar una medición nueva la borra`() = runTest {
        val created = repository.addMeasurement(SOFIA, input).getOrThrow()

        answer(outbox().single()) { synced.onRejected(it, ErrorCode.BUSINESS_RULE, "Ya cargaste una medición en esa fecha") }
        val rejected = measurement(created.id)
        assertEquals(SyncState.ERROR, rejected?.syncState)
        assertEquals("Ya cargaste una medición en esa fecha", rejected?.syncError)

        repository.discardChange(created.id).getOrThrow()
        assertNull(measurement(created.id))
        assertTrue(outbox().isEmpty())
    }

    @Test
    fun `descartar una edición rechazada vuelve a la última versión del servidor`() = runTest {
        synced.apply(row(id = "m-1", weightKg = 7.0))
        repository.updateMeasurement("m-1", input.copy(weightKg = 9.9)).getOrThrow()

        answer(outbox().single()) { synced.onRejected(it, ErrorCode.BUSINESS_RULE, "No") }
        repository.discardChange("m-1").getOrThrow()

        val restored = measurement("m-1")
        assertEquals(7.0, restored?.weightKg)
        assertEquals(LocalDate.of(2026, 10, 1), restored?.date)
        assertEquals(SyncState.SYNCED, restored?.syncState)
        assertNull(restored?.syncError)
    }

    @Test
    fun `si el servidor ya no tiene la medición, se borra del teléfono`() = runTest {
        synced.apply(row(id = "m-1"))
        repository.updateMeasurement("m-1", input).getOrThrow()

        answer(outbox().single()) { synced.onRejected(it, ErrorCode.MEASUREMENT_NOT_FOUND, "No se encontró esa medición") }

        assertNull(measurement("m-1"))
    }

    @Test
    fun `RF-CRE-02 solo el autor edita o borra`() = runTest {
        synced.apply(row(id = "m-otro", authorAccountId = "22222222-2222-2222-2222-222222222222"))

        assertTrue(repository.updateMeasurement("m-otro", input).isFailure)
        assertTrue(repository.deleteMeasurement("m-otro").isFailure)
        assertTrue(outbox().isEmpty())
        assertEquals(SyncState.SYNCED, measurement("m-otro")?.syncState)
    }

    @Test
    fun `RF-CRE-02 borrar la propia la saca del teléfono y envía el borrado, salvo que el servidor nunca la haya tenido`() = runTest {
        synced.apply(row(id = "m-1"))
        repository.deleteMeasurement("m-1").getOrThrow()
        assertNull(measurement("m-1"))
        assertEquals(listOf(MutationOp.DELETE), outbox().map { it.op })
        database.pendingMutationDao().delete(outbox().map { it.mutationId })

        val created = repository.addMeasurement(SOFIA, input).getOrThrow()
        answer(outbox().single()) { synced.onRejected(it, ErrorCode.BUSINESS_RULE, "No") }
        repository.deleteMeasurement(created.id).getOrThrow()
        assertTrue(outbox().isEmpty())
    }

    @Test
    fun `una fila del servidor se guarda sincronizada y su lápida la borra`() = runTest {
        synced.apply(row(id = "m-1", lengthCm = 101.0, lengthPosition = "STANDING"))
        assertEquals(LengthPosition.STANDING, measurement("m-1")?.lengthPosition)
        assertEquals(SyncState.SYNCED, measurement("m-1")?.syncState)

        synced.apply(row(id = "m-1", deleted = true))
        assertNull(measurement("m-1"))
    }

    private suspend fun outbox(): List<PendingMutationEntity> = database.pendingMutationDao().oldest(10)

    private suspend fun measurement(id: String) = repository.observeMeasurement(id).first()

    /** Igual que el motor: el cambio sale de la bandeja y la entidad recibe la respuesta. */
    private suspend fun answer(mutation: PendingMutationEntity, handler: suspend (PendingMutationEntity) -> Unit) {
        database.pendingMutationDao().delete(listOf(mutation.mutationId))
        handler(mutation)
    }

    private fun JsonObject.text(key: String) = get(key)?.toString()?.trim('"')

    /** Fila `MEASUREMENT` como la arma el backend en `/sync/changes`. */
    private fun row(
        id: String,
        weightKg: Double = 6.4,
        lengthCm: Double? = null,
        lengthPosition: String? = null,
        authorAccountId: String = ChildTestData.ACCOUNT_ID,
        deleted: Boolean = false,
    ): JsonObject = buildJsonObject {
        put("id", id)
        put("childId", SOFIA)
        put("measuredOn", "2026-10-01")
        put("weightKg", weightKg)
        lengthCm?.let { put("lengthCm", it) }
        lengthPosition?.let { put("lengthPosition", it) }
        put("outlierConfirmed", false)
        put("authorAccountId", authorAccountId)
        put("authorRole", "GUARDIAN")
        put("deleted", deleted)
    }
}
