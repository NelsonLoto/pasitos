package com.pasitos.app.data.vaccination

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
import com.pasitos.app.data.child.ChildTestData
import com.pasitos.app.data.child.ChildTestData.ACCOUNT_ID
import com.pasitos.app.data.child.ChildTestData.SOFIA
import com.pasitos.app.data.child.ChildTestData.TOMAS
import com.pasitos.app.data.child.FixedSessionTokens
import com.pasitos.app.data.child.RecordingSyncScheduler
import com.pasitos.app.data.vaccination.local.AppliedDoseEntity
import com.pasitos.app.data.vaccination.model.DoseApplicationInput
import com.pasitos.app.data.vaccination.model.OtherVaccineInput
import com.pasitos.app.data.vaccination.remote.VaccinationRemoteDataSource
import com.pasitos.shared.api.AuthorRole
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.vaccination.AppliedDoseResponse
import com.pasitos.shared.api.vaccination.CalendarDoseDto
import com.pasitos.shared.api.vaccination.CreateAppliedDoseRequest
import com.pasitos.shared.api.vaccination.FormerDoseDto
import com.pasitos.shared.api.vaccination.OtherVaccineResponse
import com.pasitos.shared.api.vaccination.RetroactiveDosesRequest
import com.pasitos.shared.api.vaccination.RetroactiveDosesResponse
import com.pasitos.shared.api.vaccination.VaccinationCalendarResponse
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Vacunación sobre Room y `/api/v1` (I1-A7). */
@RunWith(RobolectricTestRunner::class)
class RoomVaccinationRepositoryTest {

    private lateinit var database: PasitosDatabase
    private val server = FakeServer(validAccessToken = "access-1")
    private val scheduler = RecordingSyncScheduler()
    private val dao get() = database.vaccinationDao()

    private val repository by lazy {
        val httpClient = createHttpClient(server.engine, "http://localhost", FixedSessionTokens())
        val remote = VaccinationRemoteDataSource(httpClient)
        RoomVaccinationRepository(
            dao = dao,
            remote = remote,
            calendarData = VaccinationCalendarReferenceData(dao, remote),
            syncEngine = SyncEngine(
                httpClient = httpClient,
                database = database,
                syncStateDao = database.syncStateDao(),
                pendingMutationDao = database.pendingMutationDao(),
                entities = setOf(AppliedDoseSyncedEntity(dao), OtherVaccineSyncedEntity(dao)),
                outboxEntities = emptySet(),
                clock = ChildTestData.clock,
            ),
            syncScheduler = scheduler,
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
    fun `el calendario se guarda con su ETag y con 304 no se vuelve a bajar`() = runTest {
        var downloads = 0
        server.routes[CALENDAR] = { request ->
            if (request.headers[HttpHeaders.IfNoneMatch] == "\"v1\"") {
                respond("", HttpStatusCode.NotModified)
            } else {
                downloads++
                respond(calendarJson(), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType to listOf("application/json"), HttpHeaders.ETag to listOf("\"v1\"")))
            }
        }

        repository.refreshCalendar().getOrThrow()
        repository.refreshCalendar().getOrThrow()

        assertEquals(1, downloads)
        val calendar = repository.calendar.first { !it.isEmpty }
        assertEquals(LocalDate.of(2026, 1, 1), calendar.effectiveFrom)
        assertEquals(listOf("BCG", "SRP-2"), calendar.doses.map { it.dose.doseCode })
        assertEquals(LocalDate.of(2024, 7, 1), calendar.doses.last().dose.bornFrom)
        assertEquals("Fiebre amarilla", calendar.former("FA-1")?.vaccineName)
    }

    @Test
    fun `una respuesta que no se entiende no reemplaza el calendario guardado`() = runTest {
        server.routes[CALENDAR] = { json(calendarJson()) }
        repository.refreshCalendar().getOrThrow()
        server.routes[CALENDAR] = { json("{\"esto\": \"no es un calendario\"}") }

        val result = repository.refreshCalendar()

        assertTrue(result.isFailure)
        assertEquals(2, repository.calendar.first { !it.isEmpty }.doses.size)
    }

    @Test
    fun `RF-VAC-02 registrar guarda la respuesta en Room, sin lote ni lugar vacíos`() = runTest {
        server.routes["/api/v1/children/$SOFIA/applied-doses"] = { json(Json.encodeToString(doseResponse("BCG")), HttpStatusCode.Created) }

        val dose = repository.registerDose(SOFIA, "BCG", DoseApplicationInput(LocalDate.of(2025, 10, 17), lot = "  ", place = " Hospital ")).getOrThrow()

        val sent = Json.decodeFromString<CreateAppliedDoseRequest>(server.requestsTo("/api/v1/children/$SOFIA/applied-doses").single().bodyText())
        assertEquals(CreateAppliedDoseRequest("BCG", "2025-10-17", lot = null, place = "Hospital"), sent)
        assertEquals("dose-BCG", dose.id)
        assertEquals(listOf("BCG"), dao.observeAppliedDoses(SOFIA).first().map { it.doseCode })
        assertEquals(listOf(SyncTrigger.LOCAL_CHANGE), scheduler.requests)
    }

    @Test
    fun `RN-VAC-09 si otro tutor ya la registró, falla y pide sincronizar para traer su registro`() = runTest {
        server.routes["/api/v1/children/$SOFIA/applied-doses"] = {
            error(HttpStatusCode.UnprocessableEntity, ErrorCode.BUSINESS_RULE, "Esa dosis ya está registrada")
        }

        val result = repository.registerDose(SOFIA, "BCG", DoseApplicationInput(LocalDate.of(2025, 10, 17), null, null))

        assertEquals("Esa dosis ya está registrada", (result.exceptionOrNull() as ApiException).message)
        assertEquals(listOf(SyncTrigger.LOCAL_CHANGE), scheduler.requests)
        assertTrue(dao.observeAppliedDoses(SOFIA).first().isEmpty())
    }

    @Test
    fun `sin conexión no guarda nada`() = runTest {
        server.routes["/api/v1/children/$SOFIA/applied-doses"] = { throw IOException("sin red") }

        val result = repository.registerDose(SOFIA, "BCG", DoseApplicationInput(LocalDate.of(2025, 10, 17), null, null))

        assertTrue(result.exceptionOrNull() is NoConnectionException)
        assertTrue(dao.observeAppliedDoses(SOFIA).first().isEmpty())
    }

    @Test
    fun `RF-VAC-04 la carga retroactiva manda los códigos y guarda las dosis creadas`() = runTest {
        server.routes["/api/v1/children/$SOFIA/applied-doses/retroactive"] = {
            json(Json.encodeToString(RetroactiveDosesResponse(listOf(doseResponse("BCG", null), doseResponse("HB-NEO", null)))), HttpStatusCode.Created)
        }

        val created = repository.registerPreviousDoses(SOFIA, listOf("BCG", "HB-NEO")).getOrThrow()

        val sent = Json.decodeFromString<RetroactiveDosesRequest>(server.requestsTo("/api/v1/children/$SOFIA/applied-doses/retroactive").single().bodyText())
        assertEquals(listOf("BCG", "HB-NEO"), sent.doseCodes)
        assertEquals(listOf(null, null), created.map { it.appliedOn })
        assertEquals(setOf("BCG", "HB-NEO"), dao.observeAppliedDoses(SOFIA).first().map { it.doseCode }.toSet())
    }

    @Test
    fun `RF-VAC-03 editar y eliminar actualizan Room`() = runTest {
        dao.upsertAppliedDoses(listOf(entity("BCG")))
        server.routes["/api/v1/applied-doses/dose-BCG"] = { request ->
            if (request.method.value == "DELETE") respond("", HttpStatusCode.NoContent) else json(Json.encodeToString(doseResponse("BCG").copy(lot = "B2")))
        }

        repository.updateDose("dose-BCG", DoseApplicationInput(LocalDate.of(2025, 10, 17), "B2", null)).getOrThrow()
        assertEquals("B2", dao.observeAppliedDoses(SOFIA).first().single().lot)

        repository.deleteDose("dose-BCG").getOrThrow()
        assertTrue(dao.observeAppliedDoses(SOFIA).first().isEmpty())
    }

    @Test
    fun `RF-VAC-05 crear y editar otra vacuna guardan la respuesta`() = runTest {
        server.routes["/api/v1/children/$SOFIA/other-vaccines"] = { json(Json.encodeToString(otherResponse("Antigripal")), HttpStatusCode.Created) }
        server.routes["/api/v1/other-vaccines/other-1"] = { json(Json.encodeToString(otherResponse("Antigripal trivalente"))) }

        repository.saveOtherVaccine(SOFIA, null, OtherVaccineInput("Antigripal", LocalDate.of(2026, 5, 1), null, null)).getOrThrow()
        repository.saveOtherVaccine(SOFIA, "other-1", OtherVaccineInput("Antigripal trivalente", LocalDate.of(2026, 5, 1), null, null)).getOrThrow()

        assertEquals(listOf("Antigripal trivalente"), dao.observeOtherVaccines(SOFIA).first().map { it.name })
    }

    @Test
    fun `por sincronización llegan dosis y otras vacunas, las lápidas las borran y deleteChild limpia al niño`() = runTest {
        val doses = AppliedDoseSyncedEntity(dao)
        val others = OtherVaccineSyncedEntity(dao)

        doses.apply(doseRow("dose-1", SOFIA, "BCG"))
        doses.apply(doseRow("dose-2", SOFIA, "HB-NEO", appliedOn = null))
        doses.apply(doseRow("dose-3", TOMAS, "BCG"))
        others.apply(otherRow("other-1", SOFIA))
        doses.apply(doseRow("dose-2", SOFIA, "HB-NEO", deleted = true))

        assertEquals(listOf("BCG"), dao.observeAppliedDoses(SOFIA).first().map { it.doseCode })
        assertEquals(LocalDate.of(2025, 10, 17), dao.observeAppliedDoses(SOFIA).first().single().appliedOn)
        assertEquals(1, dao.observeOtherVaccines(SOFIA).first().size)

        doses.deleteChild(SOFIA)
        others.deleteChild(SOFIA)
        assertTrue(dao.observeAppliedDoses(SOFIA).first().isEmpty())
        assertTrue(dao.observeOtherVaccines(SOFIA).first().isEmpty())
        assertEquals("Los datos de otro niño no se tocan", 1, dao.observeAppliedDoses(TOMAS).first().size)
    }

    private fun calendarJson() = Json.encodeToString(
        VaccinationCalendarResponse(
            effectiveFrom = "2026-01-01",
            doses = listOf(
                CalendarDoseDto("BCG", "BCG", "Tuberculosis", VaccinationTestData.SOURCE, "Única dosis", 0, 7, 30, null, null, 1),
                CalendarDoseDto("SRP-2", "Triple viral (SRP)", "Sarampión", VaccinationTestData.SOURCE, "2ª dosis", 15, 7, 120, "2024-07-01", null, 20),
            ),
            formerDoses = listOf(FormerDoseDto("FA-1", "Fiebre amarilla", "1ª dosis")),
        ),
    )

    private fun doseResponse(doseCode: String, appliedOn: String? = "2025-10-17") = AppliedDoseResponse(
        id = "dose-$doseCode",
        childId = SOFIA,
        doseCode = doseCode,
        appliedOn = appliedOn,
        lot = null,
        place = null,
        authorAccountId = ACCOUNT_ID,
        authorRole = AuthorRole.GUARDIAN,
    )

    private fun otherResponse(name: String) = OtherVaccineResponse(
        id = "other-1",
        childId = SOFIA,
        name = name,
        appliedOn = "2026-05-01",
        lot = null,
        note = null,
        authorAccountId = ACCOUNT_ID,
        authorRole = AuthorRole.GUARDIAN,
    )

    private fun entity(doseCode: String) = AppliedDoseEntity(
        id = "dose-$doseCode",
        childId = SOFIA,
        doseCode = doseCode,
        appliedOn = LocalDate.of(2025, 10, 17),
        lot = null,
        place = null,
        authorAccountId = ACCOUNT_ID,
        authorRole = AuthorRole.GUARDIAN,
    )

    /** Fila `APPLIED_DOSE` como la arma el backend en `/sync/changes`. */
    private fun doseRow(id: String, childId: String, doseCode: String, appliedOn: String? = "2025-10-17", deleted: Boolean = false) =
        buildJsonObject {
            put("id", id)
            put("childId", childId)
            put("doseCode", doseCode)
            appliedOn?.let { put("appliedOn", it) }
            put("authorAccountId", ACCOUNT_ID)
            put("authorRole", "GUARDIAN")
            put("deleted", deleted)
        }

    private fun otherRow(id: String, childId: String) = buildJsonObject {
        put("id", id)
        put("childId", childId)
        put("name", "Antigripal")
        put("appliedOn", "2026-05-01")
        put("authorAccountId", ACCOUNT_ID)
        put("authorRole", "GUARDIAN")
        put("deleted", false)
    }

    private companion object {
        const val CALENDAR = "/api/v1/vaccination-calendar"
    }
}
