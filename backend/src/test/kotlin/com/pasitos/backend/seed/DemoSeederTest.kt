package com.pasitos.backend.seed

import com.pasitos.backend.auth.AccountRepository
import com.pasitos.backend.auth.Accounts
import com.pasitos.backend.auth.PasswordHasher
import com.pasitos.backend.child.ChildRepository
import com.pasitos.backend.child.Children
import com.pasitos.backend.child.NewChild
import com.pasitos.backend.child.Sex
import com.pasitos.backend.common.AuthorRole
import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.config.DatabaseFactory
import com.pasitos.backend.guardianship.GuardianshipRepository
import com.pasitos.backend.guardianship.GuardianshipRole
import com.pasitos.backend.measurement.MeasurementRepository
import com.pasitos.backend.measurement.MeasurementEdits
import com.pasitos.backend.measurement.Measurements
import com.pasitos.backend.support.TestDatabase
import com.pasitos.backend.support.apiTest
import com.pasitos.shared.api.auth.AuthResponse
import com.pasitos.shared.api.auth.LoginRequest
import com.pasitos.shared.api.sync.SyncChangesResponse
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import com.pasitos.backend.vaccination.AppliedDoseRepository
import com.pasitos.backend.vaccination.CalendarRepository
import com.pasitos.backend.vaccination.parseCalendarCsv
import com.pasitos.shared.vaccination.AppliedDose
import com.pasitos.shared.vaccination.DoseStatus
import com.pasitos.shared.vaccination.doseStatuses
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.jetbrains.exposed.exceptions.ExposedSQLException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/** Usa solo PostgreSQL descartable; nunca la base del desarrollador ni backend/.env. */
class DemoSeederTest {
    private val now = Instant.parse("2026-11-25T15:00:00Z")
    private val clock = ArgentinaClock(Clock.fixed(now, ZoneOffset.UTC))
    private val data = readDemoData()
    private val password = "Prueba-demo-12345"
    private lateinit var source: HikariDataSource
    private fun seeder() = DemoSeeder(clock, TestDatabase.TERMS_VERSION, TestDatabase.DECLARATION_VERSION)

    @BeforeTest
    fun prepare() {
        TestDatabase.reset()
        source = DatabaseFactory.init(TestDatabase.config())
    }

    @AfterTest
    fun close() { source.close() }

    private fun calendar() = parseCalendarCsv(File("seed/calendar/calendario-nacional-2026-01-01.csv").readText())
    private fun loadCalendar() = transaction {
        CalendarRepository().insertPublishedVersion(LocalDate.of(2026, 1, 1), calendar(), now)
    }

    @Test
    fun `carga cinco escenarios con credenciales verificadas, consentimientos y permisos aislados`() {
        loadCalendar()
        assertTrue(seeder().seed(data, password))
        transaction {
            val accounts = AccountRepository()
            data.guardians.forEach { guardian ->
                val account = assertNotNull(accounts.findByEmail(guardian.email))
                assertEquals(data.id("account", guardian.key), account.id)
                assertNotNull(account.emailVerifiedAt)
                assertNull(account.googleSubject)
                assertNotEquals(password, account.passwordHash)
                assertTrue(PasswordHasher.verify(password, assertNotNull(account.passwordHash)))
            }
            val principal = data.id("account", "principal")
            val secondary = data.id("account", "secundario")
            val isolated = data.id("account", "aislado")
            assertEquals(4, ChildRepository().listForGuardian(principal).size)
            assertEquals(4, ChildRepository().listForGuardian(secondary).size)
            assertEquals(1, ChildRepository().listForGuardian(isolated).size)
            assertNull(ChildRepository().findForGuardian(data.id("child", "escolar"), principal))
            assertNull(ChildRepository().findForGuardian(data.id("child", "esperado"), isolated))
            assertEquals(GuardianshipRole.PRINCIPAL, GuardianshipRepository().findRole(data.id("child", "esperado"), principal))
            assertEquals(GuardianshipRole.SECONDARY, GuardianshipRepository().findRole(data.id("child", "esperado"), secondary))
            assertEquals(5L, Children.selectAll().count())
            assertEquals(15L, Measurements.selectAll().count())
            assertTrue(Children.selectAll().all { it[Children.syncVersion] > 0 })
            assertTrue(Measurements.selectAll().all { it[Measurements.syncVersion] > 0 })
        }
        assertEquals(3L, TestDatabase.column<Long>("SELECT count(*) FROM consents WHERE type = 'TERMS' AND version = ?", TestDatabase.TERMS_VERSION).single())
        assertEquals(9L, TestDatabase.column<Long>("SELECT count(*) FROM consents WHERE type = 'GUARDIAN_DECLARATION' AND version = ?", TestDatabase.DECLARATION_VERSION).single())
        assertEquals(0L, TestDatabase.column<Long>("SELECT count(*) FROM sessions").single())
    }

    @Test
    fun `las cuentas cargadas pueden iniciar sesion y sincronizar solo sus hijos`() = apiTest(Clock.fixed(now, ZoneOffset.UTC)) { client ->
        startApplication()
        loadCalendar()
        seeder().seed(data, password)
        data.guardians.forEach { guardian ->
            val login = client.post("/api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(guardian.email, password))
            }
            assertEquals(HttpStatusCode.OK, login.status)
            val auth = login.body<AuthResponse>()
            assertTrue(auth.account.emailVerified)
            val response = client.get("/api/v1/sync/changes?cursor=0") { bearerAuth(auth.tokens.accessToken) }
            assertEquals(HttpStatusCode.OK, response.status)
            val changes = response.body<SyncChangesResponse>()
            val expected = data.children.filter { guardian.key in it.guardians }.map { data.id("child", it.key).toString() }.toSet()
            assertEquals(expected, changes.newChildIds.toSet())
            assertTrue(changes.changes.getValue("MEASUREMENT").isNotEmpty())
        }
    }

    @Test
    fun `calendario real produce vacunas al dia, atrasadas y retroactivas sin fechas futuras`() {
        loadCalendar()
        seeder().seed(data, password)
        transaction {
            data.children.forEach { child ->
                val records = AppliedDoseRepository().listActiveForChild(data.id("child", child.key))
                val status = doseStatuses(child.birthDate(clock.today()), calendar().map { it.toCalendarDose() }, records.map { AppliedDose(it.doseCode, it.appliedOn) }, clock.today())
                if (child.vaccination == DemoVaccination.OVERDUE) {
                    assertTrue(status.doses.any { it.status == DoseStatus.OVERDUE })
                } else {
                    assertFalse(status.doses.any { it.status == DoseStatus.OVERDUE || it.status == DoseStatus.DUE })
                }
                if (child.vaccination == DemoVaccination.RETROACTIVE) {
                    assertTrue(records.any { it.appliedOn == null })
                    assertTrue(records.any { it.appliedOn?.isBefore(clock.today()) == true })
                }
                assertTrue(records.all { it.appliedOn == null || (!it.appliedOn.isBefore(child.birthDate(clock.today())) && !it.appliedOn.isAfter(clock.today())) })
                assertEquals(records.size, records.map { it.doseCode }.distinct().size)
            }
        }
    }

    @Test
    fun `repetir incluso otro dia no duplica ni sobrescribe una demo editada`() {
        loadCalendar()
        seeder().seed(data, password)
        val id = data.id("measurement", "esperado:0")
        transaction { Measurements.update({ Measurements.id eq id }) { it[weightKg] = "4.555".toBigDecimal() } }
        val versions = TestDatabase.column<Long>("SELECT sync_version FROM measurements ORDER BY id")
        val counts = TestDatabase.column<Long>("SELECT count(*) FROM applied_doses")
        val later = ArgentinaClock(Clock.fixed(now.plusSeconds(86400), ZoneOffset.UTC))
        assertFalse(DemoSeeder(later, "otra-version", "otra-version").seed(data, "Otra-clave-demo-123"))
        assertEquals(versions, TestDatabase.column<Long>("SELECT sync_version FROM measurements ORDER BY id"))
        assertEquals(counts, TestDatabase.column<Long>("SELECT count(*) FROM applied_doses"))
        transaction {
            assertEquals("4.555".toBigDecimal(), MeasurementRepository().findById(id)?.weightKg)
            assertTrue(PasswordHasher.verify(password, assertNotNull(AccountRepository().findById(data.id("account", "principal"))?.passwordHash)))
        }
    }

    @Test
    fun `sin calendario vigente no crea cuentas ni ninos`() {
        assertFailsWith<IllegalArgumentException> { seeder().seed(data, password) }
        transaction {
            assertEquals(0L, Accounts.selectAll().count())
            assertEquals(0L, Children.selectAll().count())
        }
    }

    @Test
    fun `una colision de email no reemplaza ni vincula la cuenta existente`() {
        loadCalendar()
        val original = UUID.randomUUID()
        transaction {
            Accounts.insert {
                it[id] = original
                it[email] = data.guardians.first().email
                it[firstName] = "Cuenta existente"
                it[lastName] = "Prueba"
                it[passwordHash] = PasswordHasher.hash(password)
            }
        }
        assertFailsWith<IllegalArgumentException> { seeder().seed(data, password) }
        transaction {
            assertEquals(1L, Accounts.selectAll().count())
            assertEquals(original, AccountRepository().findByEmail(data.guardians.first().email)?.id)
            assertEquals(0L, Children.selectAll().count())
        }
    }

    @Test
    fun `un conjunto parcial se rechaza sin repararlo ni crear mas datos`() {
        loadCalendar()
        transaction {
            Accounts.insert {
                it[id] = data.id("account", "principal")
                it[email] = data.guardians.first().email
                it[firstName] = "Demo"
                it[lastName] = "Parcial"
                it[passwordHash] = PasswordHasher.hash(password)
            }
        }
        assertFailsWith<IllegalArgumentException> { seeder().seed(data, password) }
        transaction {
            assertEquals(1L, Accounts.selectAll().count())
            assertEquals(0L, Children.selectAll().count())
        }
    }

    @Test
    fun `un fallo despues de insertar revierte toda la carga y conserva datos ajenos`() {
        loadCalendar()
        transaction {
            val account = AccountRepository().insertGuardian("ajena@example.invalid", "Otra", "Prueba", PasswordHasher.hash(password))
            val child = ChildRepository().insert(NewChild("Otro", "Prueba", Sex.FEMALE, clock.today().minusMonths(6), 40, null, null, null, null), account.id)
            // Fuerza una colisión al insertar la primera medición, después de crear los tutores.
            MeasurementRepository().insert(
                data.id("measurement", "esperado:0"), child.id, account.id, AuthorRole.GUARDIAN,
                MeasurementEdits(clock.today(), "7.0".toBigDecimal(), null, null, null, false),
            )
        }
        assertFailsWith<ExposedSQLException> { seeder().seed(data, password) }
        transaction {
            assertEquals(1L, Accounts.selectAll().count())
            assertEquals(1L, Children.selectAll().count())
            assertEquals(1L, Measurements.selectAll().count())
            assertNull(AccountRepository().findById(data.id("account", "principal")))
            assertNull(ChildRepository().findById(data.id("child", "esperado")))
        }
        assertEquals(0L, TestDatabase.column<Long>("SELECT count(*) FROM consents").single())
    }

    @Test
    fun `dos cargas concurrentes solo crean un conjunto`() {
        loadCalendar()
        Executors.newFixedThreadPool(2).use { executor ->
            val tasks = List(2) { Callable { seeder().seed(data, password) } }
            val results = executor.invokeAll(tasks).map { it.get(30, TimeUnit.SECONDS) }
            assertEquals(listOf(false, true), results.sorted())
        }
        transaction {
            assertEquals(3L, Accounts.selectAll().count())
            assertEquals(5L, Children.selectAll().count())
            assertEquals(15L, Measurements.selectAll().count())
        }
    }

    @Test
    fun `fechas de fin de mes y bisiestos mantienen mediciones dentro de la vida del nino`() {
        listOf("2026-01-31", "2028-02-29", "2026-11-25").forEach { data.validate(LocalDate.parse(it)) }
        val birth = data.children.single { it.key == "prematuro" }.birthDate(clock.today())
        val first = data.children.single { it.key == "prematuro" }.measurements.first().date(birth)
        assertTrue(first.isBefore(birth.plusWeeks(8)))
    }

    @Test
    fun `valida forma de medicion desde cinco anos, precision, autores y fechas`() {
        val birth = LocalDate.of(2020, 1, 1)
        assertFailsWith<IllegalArgumentException> { DemoValues(lengthCm = "110.0", lengthPosition = "LYING").edits(birth, birth.plusYears(5)) }
        assertFailsWith<IllegalArgumentException> { DemoValues(headCircumferenceCm = "50.0").edits(birth, birth.plusYears(5)) }
        assertFailsWith<IllegalArgumentException> { DemoValues(weightKg = "4.1234").edits(birth, birth.plusMonths(1)) }
        val child = data.children.first()
        val point = child.measurements.first()
        assertFailsWith<IllegalArgumentException> { data.copy(children = listOf(child.copy(measurements = listOf(point.copy(author = "aislado"))))).validate(clock.today()) }
        assertFailsWith<IllegalArgumentException> { data.copy(children = listOf(child.copy(measurements = listOf(point.copy(ageMonths = 7))))).validate(clock.today()) }
    }

    @Test
    fun `el destino del proceso prevalece sobre dotenv y conserva valores por defecto`() {
        val environment = mapOf("DB_HOST" to "host-del-proceso", "DB_PORT" to "15432", "DB_NAME" to "demo_aislada", "DB_USER" to "usuario-proceso", "DB_PASSWORD" to "clave-proceso")
        val file = mapOf("DB_HOST" to "otro-host", "DB_PORT" to "5432", "DB_NAME" to "otra-base", "DB_USER" to "otro-usuario", "DB_PASSWORD" to "otra-clave", "TERMS_VERSION" to "terminos-archivo")
        val config = demoConfig(TestDatabase.config(), environment::get, file::get)
        assertEquals("host-del-proceso", config.property("db.host").getString())
        assertEquals("15432", config.property("db.port").getString())
        assertEquals("demo_aislada", config.property("db.name").getString())
        assertEquals("usuario-proceso", config.property("db.user").getString())
        assertEquals("clave-proceso", config.property("db.password").getString())
        assertEquals("terminos-archivo", config.property("terms.version").getString())
        assertEquals(TestDatabase.DECLARATION_VERSION, config.property("declaration.version").getString())
        requireDemoTarget(config.property("db.name").getString(), "demo_aislada")
    }

    @Test
    fun `exige confirmar el destino y rechaza credenciales inseguras antes de insertar`() {
        assertFailsWith<IllegalArgumentException> { requireDemoTarget("piloto", null) }
        assertFailsWith<IllegalArgumentException> { requireDemoTarget("piloto", "local") }
        requireDemoTarget("demo_local", "demo_local")
        assertFailsWith<IllegalArgumentException> { seeder().seed(data, "corta1") }
        assertFailsWith<IllegalArgumentException> { seeder().seed(data, "A1" + "á".repeat(36)) }
        transaction { assertEquals(0L, Accounts.selectAll().count()) }
    }
}
