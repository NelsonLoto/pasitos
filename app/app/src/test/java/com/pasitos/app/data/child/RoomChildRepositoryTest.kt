package com.pasitos.app.data.child

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pasitos.app.core.database.PasitosDatabase
import com.pasitos.app.core.network.ApiException
import com.pasitos.app.core.network.FakeServer
import com.pasitos.app.core.network.FakeServer.Companion.bodyText
import com.pasitos.app.core.network.FakeServer.Companion.error
import com.pasitos.app.core.network.FakeServer.Companion.json
import com.pasitos.app.core.network.NoConnectionException
import com.pasitos.app.core.network.createHttpClient
import com.pasitos.app.core.sync.SyncEngine
import com.pasitos.app.core.sync.SyncTrigger
import com.pasitos.app.core.sync.local.MutationEntity
import com.pasitos.app.core.sync.local.MutationOp
import com.pasitos.app.core.sync.local.PendingMutationEntity
import com.pasitos.app.data.child.ChildTestData.ACCOUNT_ID
import com.pasitos.app.data.child.ChildTestData.SOFIA
import com.pasitos.app.data.child.ChildTestData.TOMAS
import com.pasitos.app.data.child.local.ChildEntity
import com.pasitos.app.data.child.local.GuardianshipEntity
import com.pasitos.app.data.child.model.ChildInput
import com.pasitos.app.data.child.model.ChildSex
import com.pasitos.app.data.child.model.GuardianRole
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.child.ChildResponse
import com.pasitos.shared.api.child.ChildStatus
import com.pasitos.shared.api.child.CreateChildRequest
import com.pasitos.shared.api.child.DeletionPreviewResponse
import com.pasitos.shared.api.child.PatchChildRequest
import com.pasitos.shared.api.child.Sex
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.client.engine.mock.respond
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomChildRepositoryTest {

    private lateinit var database: PasitosDatabase
    private val server = FakeServer(validAccessToken = "access-1")
    private val scheduler = RecordingSyncScheduler()
    private val dao get() = database.childDao()

    private val repository by lazy {
        val httpClient = createHttpClient(server.engine, "http://localhost", FixedSessionTokens())
        RoomChildRepository(
            dao = dao,
            remote = com.pasitos.app.data.child.remote.ChildRemoteDataSource(httpClient),
            syncEngine = SyncEngine(
                httpClient = httpClient,
                database = database,
                syncStateDao = database.syncStateDao(),
                pendingMutationDao = database.pendingMutationDao(),
                entities = setOf(ChildSyncedEntity(dao), GuardianshipSyncedEntity(dao)),
                outboxEntities = emptySet(),
                clock = ChildTestData.clock,
            ),
            syncScheduler = scheduler,
            authRepository = SignedInAuthRepository(),
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
    fun `RF-HIJ-01 CA1 el alta guarda el niño, deja al usuario como principal y al niño como activo`() = runTest {
        server.routes["/api/v1/children"] = { json(Json.encodeToString(response(SOFIA)), HttpStatusCode.Created) }

        val result = repository.addChild(input(firstName = "  Sofía "), representationDeclared = true)

        assertEquals(SOFIA, result.getOrThrow().id)
        assertEquals(listOf(SOFIA), repository.children.first { it.isNotEmpty() }.map { it.id })
        assertEquals(SOFIA, repository.activeChildId.first { it != null })
        assertEquals(GuardianRole.PRINCIPAL, repository.observeRole(SOFIA).first())
        val sent = Json.decodeFromString<CreateChildRequest>(server.requestsTo("/api/v1/children").single().bodyText())
        assertEquals("Sofía", sent.firstName)
        assertEquals(Sex.FEMALE, sent.sex)
        assertEquals("2026-03-15", sent.birthDate)
        assertEquals("rabbit", sent.avatarKey)
        assertTrue(sent.representationDeclared)
        assertEquals(listOf(SyncTrigger.LOCAL_CHANGE), scheduler.requests)
    }

    @Test
    fun `sin conexión el alta no guarda nada`() = runTest {
        server.routes["/api/v1/children"] = { throw IOException("sin red") }

        val result = repository.addChild(input(), representationDeclared = true)

        assertTrue(result.exceptionOrNull() is NoConnectionException)
        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun `si el servidor rechaza el alta devuelve su mensaje`() = runTest {
        server.routes["/api/v1/children"] = {
            error(HttpStatusCode.BadRequest, ErrorCode.INVALID_INPUT, "La fecha de nacimiento no puede ser futura")
        }

        val error = repository.addChild(input(), representationDeclared = true).exceptionOrNull()

        assertTrue(error is ApiException)
        assertEquals("La fecha de nacimiento no puede ser futura", error?.message)
    }

    @Test
    fun `RF-HIJ-02 y RF-HIJ-03 la edición guarda lo que responde el servidor`() = runTest {
        dao.upsert(entity(SOFIA))
        server.routes["/api/v1/children/$SOFIA"] = {
            json(Json.encodeToString(response(SOFIA).copy(firstName = "Sofi", allergies = "Polen")))
        }

        repository.updateChild(SOFIA, input(firstName = "Sofi", allergies = " Polen ", relevantHistory = "  ")).getOrThrow()

        val child = repository.children.first { it.firstOrNull()?.firstName == "Sofi" }.single()
        assertEquals("Polen", child.allergies)
        val sent = Json.decodeFromString<PatchChildRequest>(server.requestsTo("/api/v1/children/$SOFIA").single().bodyText())
        assertEquals("Polen", sent.allergies)
        assertEquals(null, sent.relevantHistory)
    }

    @Test
    fun `RF-HIJ-09 eliminar borra del teléfono al niño, sus tutorías y sus cambios pendientes`() = runTest {
        dao.insertCreated(entity(SOFIA), GuardianshipEntity(SOFIA, ACCOUNT_ID, GuardianRole.PRINCIPAL))
        dao.upsert(entity(TOMAS))
        database.pendingMutationDao().insert(pendingMutation(SOFIA))
        server.routes["/api/v1/children/$SOFIA"] = { request ->
            assertEquals(HttpMethod.Delete, request.method)
            respond("", HttpStatusCode.NoContent)
        }

        repository.deleteChild(SOFIA).getOrThrow()

        assertEquals(listOf(TOMAS), dao.observeAll().first().map { it.id })
        assertEquals(null, dao.observeRole(SOFIA, ACCOUNT_ID).first())
        assertEquals(0, database.pendingMutationDao().count())
    }

    @Test
    fun `si la eliminación no llega al servidor no se borra nada`() = runTest {
        dao.upsert(entity(SOFIA))
        server.routes["/api/v1/children/$SOFIA"] = { throw IOException("sin red") }

        val result = repository.deleteChild(SOFIA)

        assertTrue(result.exceptionOrNull() is NoConnectionException)
        assertEquals(listOf(SOFIA), dao.observeAll().first().map { it.id })
    }

    @Test
    fun `RF-HIJ-09 la vista previa dice si es el único tutor`() = runTest {
        server.routes["/api/v1/children/$SOFIA/deletion-preview"] = {
            json(Json.encodeToString(DeletionPreviewResponse(onlyGuardian = false)))
        }

        assertEquals(false, repository.isOnlyGuardian(SOFIA).getOrThrow())
    }

    @Test
    fun `RF-HIJ-04 la lista va del mayor al menor`() = runTest {
        dao.upsert(entity(SOFIA, birthDate = LocalDate.of(2026, 3, 15)))
        dao.upsert(entity(TOMAS, birthDate = LocalDate.of(2023, 1, 10)))

        assertEquals(listOf(TOMAS, SOFIA), repository.children.first { it.size == 2 }.map { it.id })
    }

    @Test
    fun `RF-HIJ-04 CA3 se conserva el hijo elegido y, si ya no está, pasa a ser el primero`() = runTest {
        dao.upsert(entity(SOFIA, birthDate = LocalDate.of(2026, 3, 15)))
        dao.upsert(entity(TOMAS, birthDate = LocalDate.of(2023, 1, 10)))
        assertEquals(TOMAS, repository.activeChildId.first { it != null })

        repository.selectChild(SOFIA)
        assertEquals(SOFIA, repository.activeChildId.first { it == SOFIA })

        dao.delete(SOFIA)
        assertEquals(TOMAS, repository.activeChildId.first { it == TOMAS })

        dao.delete(TOMAS)
        assertEquals(null, repository.activeChildId.first { it == null })
    }

    private fun input(
        firstName: String = "Sofía",
        allergies: String? = null,
        relevantHistory: String? = null,
    ) = ChildInput(
        firstName = firstName,
        lastName = "Pérez",
        sex = ChildSex.Female,
        birthDate = LocalDate.of(2026, 3, 15),
        gestationalWeeks = 38,
        avatarKey = "rabbit",
        allergies = allergies,
        relevantHistory = relevantHistory,
    )

    private fun response(id: String) = ChildResponse(
        id = id,
        firstName = "Sofía",
        lastName = "Pérez",
        sex = Sex.FEMALE,
        birthDate = "2026-03-15",
        gestationalWeeks = 38,
        gestationalWeeksUnknown = false,
        birthWeightKg = null,
        birthLengthCm = null,
        birthHeadCircumferenceCm = null,
        allergies = null,
        relevantHistory = null,
        avatarKey = "rabbit",
        status = ChildStatus.ACTIVE,
    )

    private fun entity(id: String, birthDate: LocalDate = LocalDate.of(2026, 3, 15)) = ChildEntity(
        id = id,
        firstName = if (id == SOFIA) "Sofía" else "Tomás",
        lastName = "Pérez",
        sex = ChildSex.Female,
        birthDate = birthDate,
        gestationalWeeks = null,
        birthWeightKg = null,
        birthLengthCm = null,
        birthHeadCircumferenceCm = null,
        allergies = null,
        relevantHistory = null,
        avatarKey = null,
    )

    private fun pendingMutation(childId: String) = PendingMutationEntity(
        mutationId = "m-$childId",
        entity = MutationEntity.MEASUREMENT,
        entityId = "e-$childId",
        childId = childId,
        op = MutationOp.UPSERT,
        payload = "{}",
        createdAt = Instant.parse("2026-10-19T12:00:00Z"),
    )
}
