package com.pasitos.backend.vaccination

import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.support.TestClock
import com.pasitos.backend.support.TestDatabase
import com.pasitos.backend.support.apiTest
import com.pasitos.backend.support.createChild
import com.pasitos.backend.support.register
import com.pasitos.shared.api.AuthorRole
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import com.pasitos.shared.api.sync.SyncChangesResponse
import com.pasitos.shared.api.sync.SyncMutation
import com.pasitos.shared.api.sync.SyncMutationStatus
import com.pasitos.shared.api.sync.SyncOp
import com.pasitos.shared.api.sync.SyncPushRequest
import com.pasitos.shared.api.sync.SyncPushResponse
import com.pasitos.shared.api.vaccination.AppliedDoseResponse
import com.pasitos.shared.api.vaccination.CreateAppliedDoseRequest
import com.pasitos.shared.api.vaccination.FormerDoseDto
import com.pasitos.shared.api.vaccination.OtherVaccineRequest
import com.pasitos.shared.api.vaccination.OtherVaccineResponse
import com.pasitos.shared.api.vaccination.PatchAppliedDoseRequest
import com.pasitos.shared.api.vaccination.RetroactiveDosesRequest
import com.pasitos.shared.api.vaccination.RetroactiveDosesResponse
import com.pasitos.shared.api.vaccination.VaccinationCalendarResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Vacunación en el backend (I1-B7: RF-VAC-01 a 05, 07; RN-VAC-02, 03, 09; RN-HIJ-08). Hoy es el
 * 16/10/2026 en Argentina y el niño nació el 16/10/2025: tiene un año justo.
 */
class VaccinationRoutesTest {

    private val birthDate = "2025-10-16"
    private val source = "Calendario Nacional de Vacunación 2026, Ministerio de Salud de la Nación"

    // ---------------------------------------------------------------------------------------------
    // Calendario y carga de datos
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `RF-VAC-01 y RF-VAC-07 el calendario vigente trae todas sus filas con qué previene y la fuente`() = vaccinationTest { client, _ ->
        val token = client.register().tokens.accessToken

        val calendar = client.calendar(token).body<VaccinationCalendarResponse>()

        assertEquals("2026-01-01", calendar.effectiveFrom)
        assertEquals(28, calendar.doses.size)
        assertEquals((1..28).toList(), calendar.doses.map { it.sortOrder })
        val neumo = calendar.doses.first { it.doseCode == "NEUMO-1" }
        assertEquals("Neumococo conjugada", neumo.vaccineName)
        assertEquals("Meningitis, neumonía y sepsis por neumococo", neumo.prevents)
        assertEquals(source, neumo.source)
        assertEquals(listOf("2024-07-01" to null, null to "2024-06-30"), calendar.doses.filter { it.doseCode == "SRP-2" }.map { it.bornFrom to it.bornUntil })
        assertEquals(emptyList(), calendar.formerDoses)
    }

    @Test
    fun `sin calendario cargado responde con las listas vacías`() = apiTest(TestClock()) { client ->
        val token = client.register().tokens.accessToken

        val calendar = client.calendar(token).body<VaccinationCalendarResponse>()

        assertNull(calendar.effectiveFrom)
        assertEquals(emptyList(), calendar.doses)
    }

    @Test
    fun `con el mismo ETag en If-None-Match responde 304 sin cuerpo, y una versión nueva lo cambia`() = vaccinationTest { client, seeder ->
        val token = client.register().tokens.accessToken
        val first = client.calendar(token)
        val etag = requireNotNull(first.headers[HttpHeaders.ETag])
        assertEquals("private, no-cache", first.headers[HttpHeaders.CacheControl])

        val unchanged = client.calendar(token, ifNoneMatch = etag)
        assertEquals(HttpStatusCode.NotModified, unchanged.status)
        assertEquals("", unchanged.bodyAsText())

        seeder.seedFile(LocalDate.of(2026, 10, 1), calendarCsv().withoutDose("HA"), "calendario-nacional-2026-10-01.csv")
        val changed = client.calendar(token, ifNoneMatch = etag)
        assertEquals(HttpStatusCode.OK, changed.status)
        assertNotEquals(etag, changed.headers[HttpHeaders.ETag])
    }

    @Test
    fun `RN-VAC-03 una versión nueva rige desde su fecha, conserva las dosis aplicadas y trae los nombres de las que sacó`() = vaccinationTest { client, seeder ->
        val token = client.register().tokens.accessToken
        val child = client.createChild(token, birthDate = birthDate)
        client.postDose(token, child.id, CreateAppliedDoseRequest(doseCode = "HA", appliedOn = "2026-10-16"))

        seeder.seedFile(LocalDate.of(2026, 10, 1), calendarCsv().withoutDose("HA"), "calendario-nacional-2026-10-01.csv")
        seeder.seedFile(LocalDate.of(2026, 12, 1), calendarCsv(), "calendario-nacional-2026-12-01.csv")
        val calendar = client.calendar(token).body<VaccinationCalendarResponse>()

        assertEquals("2026-10-01", calendar.effectiveFrom, "La versión del 01/12 todavía no rige")
        assertTrue(calendar.doses.none { it.doseCode == "HA" })
        assertEquals(listOf(FormerDoseDto("HA", "Hepatitis A", "Única dosis")), calendar.formerDoses)
        assertEquals(listOf("HA"), client.syncChanges(token, cursor = 0).rows("APPLIED_DOSE").map { it.text("doseCode") })
        // Una dosis que el calendario vigente ya no tiene no se puede registrar de nuevo.
        assertEquals(HttpStatusCode.BadRequest, client.postDose(token, client.createChild(token, firstName = "Lu", birthDate = birthDate).id, CreateAppliedDoseRequest("HA", "2026-10-16")).status)
    }

    @Test
    fun `seedReferenceData se puede correr dos veces sin duplicar nada`() = vaccinationTest { _, seeder ->
        val second = seeder.seed(File("seed/calendar"))

        assertEquals(emptyList(), second)
        assertEquals(listOf(1L), TestDatabase.column<Long>("SELECT count(*) FROM calendar_versions WHERE status = 'PUBLISHED'"))
        assertEquals(listOf(28L), TestDatabase.column<Long>("SELECT count(*) FROM calendar_doses"))
        assertEquals(listOf(13L), TestDatabase.column<Long>("SELECT count(*) FROM vaccines"))
    }

    @Test
    fun `un archivo con problemas no carga nada de ese archivo`() = apiTest(TestClock()) { client ->
        startApplication()
        val seeder = CalendarSeeder(CalendarRepository(), ArgentinaClock(TestClock()))
        // La 2ª de triple viral con dos rangos de nacimiento superpuestos (RN-VAC-10)
        val overlapping = calendarCsv().replace(",,2024-06-30,23", ",,2024-12-31,23")

        val error = assertFailsWith<IllegalArgumentException> {
            seeder.seedFile(LocalDate.of(2026, 1, 1), overlapping, "calendario-nacional-2026-01-01.csv")
        }

        assertEquals("calendario-nacional-2026-01-01.csv: SRP-2: dos filas con rangos de nacimiento superpuestos", error.message)
        assertEquals(listOf(0L), TestDatabase.column<Long>("SELECT count(*) FROM calendar_versions"))
        assertEquals(listOf(0L), TestDatabase.column<Long>("SELECT count(*) FROM vaccines"))
    }

    // ---------------------------------------------------------------------------------------------
    // Dosis aplicadas
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `RF-VAC-02 CA1 registrar una dosis la deja aplicada con su autor y llega por sincronización`() = vaccinationTest { client, _ ->
        val auth = client.register()
        val token = auth.tokens.accessToken
        val child = client.createChild(token, birthDate = birthDate)

        val response = client.postDose(token, child.id, CreateAppliedDoseRequest("BCG", "2025-10-17", lot = " A123 ", place = "Hospital"))

        assertEquals(HttpStatusCode.Created, response.status)
        val dose = response.body<AppliedDoseResponse>()
        assertEquals("BCG", dose.doseCode)
        assertEquals("2025-10-17", dose.appliedOn)
        assertEquals("A123", dose.lot)
        assertEquals(auth.account.id, dose.authorAccountId)
        assertEquals(AuthorRole.GUARDIAN, dose.authorRole)
        val row = client.syncChanges(token, cursor = 0).rows("APPLIED_DOSE").single()
        assertEquals(dose.id, row.text("id"))
        assertEquals("GUARDIAN", row.text("authorRole"))
        assertEquals("false", row.text("deleted"))
    }

    @Test
    fun `RF-VAC-02 CA2 rechaza una fecha anterior al nacimiento o posterior a hoy`() = vaccinationTest { client, _ ->
        val token = client.register().tokens.accessToken
        val child = client.createChild(token, birthDate = birthDate)

        val beforeBirth = client.postDose(token, child.id, CreateAppliedDoseRequest("BCG", "2025-10-15"))
        val future = client.postDose(token, child.id, CreateAppliedDoseRequest("BCG", "2026-10-17"))
        val invalid = client.postDose(token, child.id, CreateAppliedDoseRequest("BCG", "17/10/2025"))

        assertEquals(HttpStatusCode.BadRequest, beforeBirth.status)
        assertEquals("La fecha de aplicación no puede ser anterior al nacimiento", beforeBirth.body<ErrorResponse>().message)
        assertEquals("La fecha de aplicación no puede ser posterior a hoy", future.body<ErrorResponse>().message)
        assertEquals(HttpStatusCode.BadRequest, invalid.status)
        assertEquals(listOf(0L), TestDatabase.column<Long>("SELECT count(*) FROM applied_doses"))
    }

    @Test
    fun `RF-VAC-02 CA3 sin lote ni lugar se guarda igual`() = vaccinationTest { client, _ ->
        val token = client.register().tokens.accessToken
        val child = client.createChild(token, birthDate = birthDate)

        val response = client.postDose(token, child.id, CreateAppliedDoseRequest("BCG", "2025-10-17", lot = "  ", place = null))

        assertEquals(HttpStatusCode.Created, response.status)
        val dose = response.body<AppliedDoseResponse>()
        assertNull(dose.lot)
        assertNull(dose.place)
    }

    @Test
    fun `RF-VAC-02 CA4 y RN-VAC-09 una dosis ya aplicada se rechaza con la regla, y vuelve a poder registrarse si se elimina`() = vaccinationTest { client, _ ->
        val token = client.register().tokens.accessToken
        val child = client.createChild(token, birthDate = birthDate)
        val first = client.postDose(token, child.id, CreateAppliedDoseRequest("BCG", "2025-10-17")).body<AppliedDoseResponse>()

        val again = client.postDose(token, child.id, CreateAppliedDoseRequest("BCG", "2025-10-20"))

        assertEquals(HttpStatusCode.UnprocessableEntity, again.status)
        val error = again.body<ErrorResponse>()
        assertEquals(ErrorCode.BUSINESS_RULE, error.code)
        assertEquals("RN-VAC-09", error.rule)

        client.delete("/api/v1/applied-doses/${first.id}") { bearerAuth(token) }
        assertEquals(HttpStatusCode.Created, client.postDose(token, child.id, CreateAppliedDoseRequest("BCG", "2025-10-20")).status)
    }

    @Test
    fun `solo se registran dosis del calendario vigente`() = vaccinationTest { client, _ ->
        val token = client.register().tokens.accessToken
        val child = client.createChild(token, birthDate = birthDate)

        val response = client.postDose(token, child.id, CreateAppliedDoseRequest("ANTIGRIPAL", "2026-05-01"))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Esa dosis no corresponde al calendario de este niño o niña", response.body<ErrorResponse>().message)
    }

    @Test
    fun `RF-VAC-03 CA1 el autor edita y elimina su dosis, y el borrado llega como lápida`() = vaccinationTest { client, _ ->
        val token = client.register().tokens.accessToken
        val child = client.createChild(token, birthDate = birthDate)
        val dose = client.postDose(token, child.id, CreateAppliedDoseRequest("BCG", "2025-10-17")).body<AppliedDoseResponse>()

        val edited = client.patchDose(token, dose.id, PatchAppliedDoseRequest(appliedOn = "2025-10-18", lot = "B9", place = "Centro de salud"))
        assertEquals(HttpStatusCode.OK, edited.status)
        assertEquals(listOf("2025-10-18", "B9"), edited.body<AppliedDoseResponse>().let { listOf(it.appliedOn, it.lot) })

        val cursor = client.syncChanges(token, cursor = 0).cursor
        assertEquals(HttpStatusCode.NoContent, client.delete("/api/v1/applied-doses/${dose.id}") { bearerAuth(token) }.status)

        val tombstone = client.syncChanges(token, cursor).rows("APPLIED_DOSE").single()
        assertEquals(dose.id, tombstone.text("id"))
        assertEquals("true", tombstone.text("deleted"))
    }

    @Test
    fun `RF-VAC-03 CA2 otro tutor ve la dosis por sincronización pero no puede editarla ni eliminarla`() = vaccinationTest { client, _ ->
        val principal = client.register(email = "ana@example.com").tokens.accessToken
        val child = client.createChild(principal, birthDate = birthDate)
        val dose = client.postDose(principal, child.id, CreateAppliedDoseRequest("BCG", "2025-10-17")).body<AppliedDoseResponse>()
        val secondary = client.register(email = "beto@example.com")
        seedSecondaryGuardianship(child.id, secondary.account.id)
        val token = secondary.tokens.accessToken

        val patch = client.patchDose(token, dose.id, PatchAppliedDoseRequest(appliedOn = "2025-10-18"))
        val delete = client.delete("/api/v1/applied-doses/${dose.id}") { bearerAuth(token) }

        assertEquals(HttpStatusCode.Forbidden, patch.status)
        assertEquals("Solo quien la registró puede modificarla o eliminarla", patch.body<ErrorResponse>().message)
        assertEquals(HttpStatusCode.Forbidden, delete.status)
        val row = client.syncChanges(token, cursor = 0).rows("APPLIED_DOSE").single()
        assertEquals(dose.authorAccountId, row.text("authorAccountId"))
    }

    @Test
    fun `matriz de permisos quien no es tutor del niño recibe 404 en cada acción`() = vaccinationTest { client, _ ->
        val owner = client.register(email = "ana@example.com").tokens.accessToken
        val child = client.createChild(owner, birthDate = birthDate)
        val dose = client.postDose(owner, child.id, CreateAppliedDoseRequest("BCG", "2025-10-17")).body<AppliedDoseResponse>()
        val other = client.postOther(owner, child.id, OtherVaccineRequest("Antigripal", "2026-05-01")).body<OtherVaccineResponse>()
        val stranger = client.register(email = "carla@example.com").tokens.accessToken

        val responses = listOf(
            client.postDose(stranger, child.id, CreateAppliedDoseRequest("HB-NEO", "2025-10-17")),
            client.retroactive(stranger, child.id, listOf("HB-NEO")),
            client.patchDose(stranger, dose.id, PatchAppliedDoseRequest(appliedOn = "2025-10-18")),
            client.delete("/api/v1/applied-doses/${dose.id}") { bearerAuth(stranger) },
            client.postOther(stranger, child.id, OtherVaccineRequest("Antigripal", "2026-05-01")),
            client.patchOther(stranger, other.id, OtherVaccineRequest("Antigripal", "2026-05-02")),
            client.delete("/api/v1/other-vaccines/${other.id}") { bearerAuth(stranger) },
        )

        assertEquals(List(7) { HttpStatusCode.NotFound }, responses.map { it.status })
        assertEquals(
            listOf(ErrorCode.CHILD_NOT_FOUND, ErrorCode.CHILD_NOT_FOUND, ErrorCode.APPLIED_DOSE_NOT_FOUND, ErrorCode.APPLIED_DOSE_NOT_FOUND, ErrorCode.CHILD_NOT_FOUND, ErrorCode.OTHER_VACCINE_NOT_FOUND, ErrorCode.OTHER_VACCINE_NOT_FOUND),
            responses.map { it.body<ErrorResponse>().code },
        )
        assertEquals(HttpStatusCode.NotFound, client.patchDose(owner, UUID.randomUUID().toString(), PatchAppliedDoseRequest("2025-10-18")).status)
    }

    // ---------------------------------------------------------------------------------------------
    // Carga retroactiva
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `RF-VAC-04 CA1 y CA2 las dosis anteriores a hoy quedan aplicadas sin fecha, y reenviar el pedido no repite nada`() = vaccinationTest { client, _ ->
        val token = client.register().tokens.accessToken
        val child = client.createChild(token, birthDate = birthDate)
        client.postDose(token, child.id, CreateAppliedDoseRequest("BCG", "2025-10-17"))
        // Hasta los 6 meses: las de los 12 meses caen hoy y todavía no son "anteriores"
        val previous = listOf(
            "BCG", "HB-NEO", "NEUMO-1", "PENTA-1", "IPV-1", "ROTA-1", "MEN-1", "NEUMO-2", "PENTA-2",
            "IPV-2", "ROTA-2", "MEN-2", "PENTA-3", "IPV-3",
        )

        val response = client.retroactive(token, child.id, previous)

        assertEquals(HttpStatusCode.Created, response.status)
        val created = response.body<RetroactiveDosesResponse>().created
        assertEquals(previous - "BCG", created.map { it.doseCode })
        assertTrue(created.all { it.appliedOn == null })
        assertEquals(emptyList(), client.retroactive(token, child.id, previous).body<RetroactiveDosesResponse>().created)
        assertEquals(listOf(14L), TestDatabase.column<Long>("SELECT count(*) FROM applied_doses"))
    }

    @Test
    fun `RF-VAC-04 una dosis que no es anterior a hoy rechaza todo el pedido`() = vaccinationTest { client, _ ->
        val token = client.register().tokens.accessToken
        val child = client.createChild(token, birthDate = birthDate)

        val response = client.retroactive(token, child.id, listOf("BCG", "NEUMO-R"))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(listOf(0L), TestDatabase.column<Long>("SELECT count(*) FROM applied_doses"))
        assertEquals(HttpStatusCode.BadRequest, client.retroactive(token, child.id, emptyList()).status)
        assertEquals(HttpStatusCode.BadRequest, client.retroactive(token, child.id, List(51) { "BCG-$it" }).status)
    }

    @Test
    fun `RF-VAC-04 CA3 el autor completa fecha, lote y lugar de una dosis sin fecha`() = vaccinationTest { client, _ ->
        val token = client.register().tokens.accessToken
        val child = client.createChild(token, birthDate = birthDate)
        val (bcg, hb) = client.retroactive(token, child.id, listOf("BCG", "HB-NEO")).body<RetroactiveDosesResponse>().created

        val lotOnly = client.patchDose(token, bcg.id, PatchAppliedDoseRequest(appliedOn = null, lot = "L1"))
        val withDate = client.patchDose(token, hb.id, PatchAppliedDoseRequest(appliedOn = "2025-10-16", place = "Maternidad"))
        val removeDate = client.patchDose(token, hb.id, PatchAppliedDoseRequest(appliedOn = null))

        assertEquals(HttpStatusCode.OK, lotOnly.status)
        assertEquals(listOf(null, "L1"), lotOnly.body<AppliedDoseResponse>().let { listOf(it.appliedOn, it.lot) })
        assertEquals("2025-10-16", withDate.body<AppliedDoseResponse>().appliedOn)
        assertEquals(HttpStatusCode.BadRequest, removeDate.status)
        assertEquals("Completá la fecha de aplicación", removeDate.body<ErrorResponse>().message)
    }

    // ---------------------------------------------------------------------------------------------
    // Otras vacunas
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `RF-VAC-05 CA1 otra vacuna se registra sin estado y llega por sincronización`() = vaccinationTest { client, _ ->
        val auth = client.register()
        val token = auth.tokens.accessToken
        val child = client.createChild(token, birthDate = birthDate)

        val response = client.postOther(token, child.id, OtherVaccineRequest(" Antigripal pediátrica 2026 ", "2026-05-01", lot = "X1", note = "Campaña de otoño"))

        assertEquals(HttpStatusCode.Created, response.status)
        val vaccine = response.body<OtherVaccineResponse>()
        assertEquals("Antigripal pediátrica 2026", vaccine.name)
        assertEquals(auth.account.id, vaccine.authorAccountId)
        val row = client.syncChanges(token, cursor = 0).rows("OTHER_VACCINE").single()
        assertEquals(listOf(vaccine.id, "2026-05-01", "Campaña de otoño"), listOf(row.text("id"), row.text("appliedOn"), row.text("note")))
    }

    @Test
    fun `RF-VAC-05 la fecha sigue RN-VAC-02 y solo el autor edita o elimina`() = vaccinationTest { client, _ ->
        val principal = client.register(email = "ana@example.com").tokens.accessToken
        val child = client.createChild(principal, birthDate = birthDate)
        val vaccine = client.postOther(principal, child.id, OtherVaccineRequest("Antigripal", "2026-05-01")).body<OtherVaccineResponse>()
        val secondary = client.register(email = "beto@example.com")
        seedSecondaryGuardianship(child.id, secondary.account.id)

        assertEquals(HttpStatusCode.BadRequest, client.postOther(principal, child.id, OtherVaccineRequest("Antigripal", "2025-10-15")).status)
        assertEquals(HttpStatusCode.BadRequest, client.postOther(principal, child.id, OtherVaccineRequest("Antigripal", "2026-10-17")).status)
        assertEquals(HttpStatusCode.Forbidden, client.patchOther(secondary.tokens.accessToken, vaccine.id, OtherVaccineRequest("Otra", "2026-05-01")).status)
        assertEquals(HttpStatusCode.Forbidden, client.delete("/api/v1/other-vaccines/${vaccine.id}") { bearerAuth(secondary.tokens.accessToken) }.status)

        val edited = client.patchOther(principal, vaccine.id, OtherVaccineRequest("Antigripal trivalente", "2026-05-02"))
        assertEquals("Antigripal trivalente", edited.body<OtherVaccineResponse>().name)
        assertEquals(HttpStatusCode.NoContent, client.delete("/api/v1/other-vaccines/${vaccine.id}") { bearerAuth(principal) }.status)
        assertEquals(HttpStatusCode.NotFound, client.patchOther(principal, vaccine.id, OtherVaccineRequest("Antigripal", "2026-05-01")).status)
    }

    @Test
    fun `SUP-24 lote, lugar, nombre y nota tienen un máximo, y el nombre es obligatorio`() = vaccinationTest { client, _ ->
        val token = client.register().tokens.accessToken
        val child = client.createChild(token, birthDate = birthDate)

        val invalid = listOf(
            client.postDose(token, child.id, CreateAppliedDoseRequest("BCG", "2025-10-17", lot = "L".repeat(41))),
            client.postDose(token, child.id, CreateAppliedDoseRequest("BCG", "2025-10-17", place = "P".repeat(121))),
            client.postOther(token, child.id, OtherVaccineRequest("N".repeat(121), "2026-05-01")),
            client.postOther(token, child.id, OtherVaccineRequest("Antigripal", "2026-05-01", note = "n".repeat(281))),
            client.postOther(token, child.id, OtherVaccineRequest("  ", "2026-05-01")),
        )

        assertEquals(List(5) { HttpStatusCode.BadRequest }, invalid.map { it.status })
        assertEquals("El lote tiene un máximo de 40 caracteres", invalid.first().body<ErrorResponse>().message)
        assertEquals(
            HttpStatusCode.Created,
            client.postDose(token, child.id, CreateAppliedDoseRequest("BCG", "2025-10-17", lot = "L".repeat(40), place = "P".repeat(120))).status,
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Sincronización y eliminación del niño
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `las vacunas no se escriben por sync push`() = vaccinationTest { client, _ ->
        val token = client.register().tokens.accessToken
        val mutations = listOf("APPLIED_DOSE", "OTHER_VACCINE").map { entity ->
            SyncMutation(mutationId = UUID.randomUUID().toString(), entity = entity, op = SyncOp.UPSERT, data = JsonObject(emptyMap()))
        }

        val results = client.post("/api/v1/sync/push") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(SyncPushRequest(mutations))
        }.body<SyncPushResponse>().results

        assertEquals(listOf(SyncMutationStatus.REJECTED, SyncMutationStatus.REJECTED), results.map { it.status })
    }

    @Test
    fun `RN-HIJ-08 al eliminar el perfil se borran sus vacunas, y si quedan otros tutores se conservan`() = vaccinationTest { client, _ ->
        val token = client.register(email = "ana@example.com").tokens.accessToken
        val alone = client.createChild(token, birthDate = birthDate)
        val shared = client.createChild(token, firstName = "Lu", birthDate = birthDate)
        for (child in listOf(alone, shared)) {
            client.postDose(token, child.id, CreateAppliedDoseRequest("BCG", "2025-10-17"))
            client.postOther(token, child.id, OtherVaccineRequest("Antigripal", "2026-05-01"))
        }
        val secondary = client.register(email = "beto@example.com")
        seedSecondaryGuardianship(shared.id, secondary.account.id)

        client.delete("/api/v1/children/${alone.id}") { bearerAuth(token) }
        client.delete("/api/v1/children/${shared.id}") { bearerAuth(token) }

        assertEquals(listOf(0L, 0L), countsFor(alone.id))
        assertEquals(listOf(1L, 1L), countsFor(shared.id), "El otro tutor sigue viendo las vacunas, con su autoría (RF-HIJ-07 CA3)")
    }

    // ---------------------------------------------------------------------------------------------

    /** Backend con el reloj fijo en el 16/10/2026 y el Calendario Nacional de `backend/seed/` cargado. */
    private fun vaccinationTest(block: suspend ApplicationTestBuilder.(HttpClient, CalendarSeeder) -> Unit) {
        val clock = TestClock()
        apiTest(clock) { client ->
            val seeder = seedCalendar(clock)
            block(client, seeder)
        }
    }

    private suspend fun ApplicationTestBuilder.seedCalendar(clock: Clock): CalendarSeeder {
        // La base se conecta al arrancar el backend.
        startApplication()
        return CalendarSeeder(CalendarRepository(), ArgentinaClock(clock)).also { it.seed(File("seed/calendar")) }
    }

    private fun calendarCsv() = File("seed/calendar/calendario-nacional-2026-01-01.csv").readText()

    private fun String.withoutDose(doseCode: String) = lines().filterNot { it.contains(",$doseCode,") }.joinToString("\n")

    private fun countsFor(childId: String) = listOf("applied_doses", "other_vaccines").map { table ->
        TestDatabase.column<Long>("SELECT count(*) FROM $table WHERE child_id = ?", UUID.fromString(childId)).single()
    }

    private suspend fun HttpClient.calendar(token: String, ifNoneMatch: String? = null): HttpResponse =
        get("/api/v1/vaccination-calendar") {
            bearerAuth(token)
            ifNoneMatch?.let { header(HttpHeaders.IfNoneMatch, it) }
        }

    private suspend fun HttpClient.postDose(token: String, childId: String, request: CreateAppliedDoseRequest): HttpResponse =
        post("/api/v1/children/$childId/applied-doses") { json(token, request) }

    private suspend fun HttpClient.patchDose(token: String, doseId: String, request: PatchAppliedDoseRequest): HttpResponse =
        patch("/api/v1/applied-doses/$doseId") { json(token, request) }

    private suspend fun HttpClient.retroactive(token: String, childId: String, codes: List<String>): HttpResponse =
        post("/api/v1/children/$childId/applied-doses/retroactive") { json(token, RetroactiveDosesRequest(codes)) }

    private suspend fun HttpClient.postOther(token: String, childId: String, request: OtherVaccineRequest): HttpResponse =
        post("/api/v1/children/$childId/other-vaccines") { json(token, request) }

    private suspend fun HttpClient.patchOther(token: String, vaccineId: String, request: OtherVaccineRequest): HttpResponse =
        patch("/api/v1/other-vaccines/$vaccineId") { json(token, request) }

    private inline fun <reified T> io.ktor.client.request.HttpRequestBuilder.json(token: String, body: T) {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend fun HttpClient.syncChanges(token: String, cursor: Long): SyncChangesResponse =
        get("/api/v1/sync/changes?cursor=$cursor") { bearerAuth(token) }.body()

    private fun SyncChangesResponse.rows(entity: String) = changes[entity].orEmpty().map { it.jsonObject }

    private fun JsonObject.text(field: String): String? = this[field]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content

    private fun seedSecondaryGuardianship(childId: String, accountId: String) {
        TestDatabase.connect().use {
            it.createStatement().execute(
                "INSERT INTO guardianships (child_id, account_id, role) VALUES ('$childId', '$accountId', 'SECONDARY')",
            )
        }
    }
}
