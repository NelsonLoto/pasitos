package com.pasitos.backend

import com.pasitos.backend.account.AccountsTable
import com.pasitos.backend.account.ConsentsTable
import com.pasitos.backend.auth.SessionsTable
import com.pasitos.backend.common.AppConfig
import com.pasitos.backend.common.DatabaseFactory
import com.pasitos.backend.guardianship.GuardianshipsTable
import com.pasitos.backend.sync.AccessRevocationsTable
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.DriverManager
import java.time.Instant
import java.util.TimeZone
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Línea base del esquema (I0-11) contra PostgreSQL 16 real. Cada prueba deshace lo que inserta, y cada
 * rechazo se verifica con su código de PostgreSQL para que un error de tipeo en la prueba no la haga pasar.
 */
@Testcontainers
class SchemaBaselineTest {
    companion object {
        @Container @JvmStatic val postgres = PostgreSQLContainer("postgres:16-alpine")
        private lateinit var factory: DatabaseFactory

        private const val BASELINE_VERSION = "2026.11.18.1455"
        private val BASELINE_TABLES = listOf(
            "accounts", "consents", "sessions", "email_tokens", "children", "guardianships", "invitations",
            "measurements", "vaccines", "calendar_versions", "calendar_doses", "applied_doses",
            "other_vaccines", "access_revocations",
        )
        private const val UNIQUE = "23505"
        private const val CHECK = "23514"
        private const val FOREIGN_KEY = "23503"
        // Tiene el formato de bcrypt, pero no es el hash de ninguna contraseña.
        private const val BCRYPT_FORMAT = "\$2a\$12\$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0"

        private fun config(databaseName: String) = AppConfig.from(mapOf(
            "APP_ENV" to "ci", "DB_HOST" to postgres.host,
            "DB_PORT" to postgres.firstMappedPort.toString(), "DB_NAME" to databaseName,
            "DB_USER" to postgres.username, "DB_PASSWORD" to postgres.password,
        ))

        @BeforeAll @JvmStatic fun migrate() { factory = DatabaseFactory(config(postgres.databaseName).database) }
        @AfterAll @JvmStatic fun close() = factory.close()
    }

    @Test fun `I0-11 CA1 con una base vacia el backend arranca y Flyway crea el esquema`() {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use {
            it.createStatement().execute("CREATE DATABASE empty_ca1")
        }
        testApplication {
            application { module(config("empty_ca1")) }
            assertEquals(HttpStatusCode.OK, client.get("/health").status)
        }
        DatabaseFactory(config("empty_ca1").database).use { inTransaction(it.database) { assertBaseline() } }
    }

    @Test fun `I0-11 CA2 la linea base crea las tablas la secuencia y la funcion`() = inTransaction { assertBaseline() }

    @Test fun `I0-11 CA2 cada tabla de Exposed coincide con su tabla`() = inTransaction {
        // selectAll() falla si el objeto declara una columna que la tabla no tiene (13 · Migraciones).
        listOf(AccountsTable, ConsentsTable, SessionsTable, GuardianshipsTable, AccessRevocationsTable)
            .forEach { it.selectAll().limit(0).toList() }
    }

    @Test fun `ADR-26 un momento se guarda en UTC aunque la JVM este en hora argentina`() {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"))
        try {
            // Conexiones nuevas, para que también la sesión de PostgreSQL tome la zona argentina.
            DatabaseFactory(config(postgres.databaseName).database).use { argentina ->
                inTransaction(argentina.database) {
                    val sessionId = UUID.randomUUID()
                    val moment = Instant.parse("2026-12-18T02:30:00Z")
                    SessionsTable.insert {
                        it[id] = sessionId
                        it[accountId] = account()
                        it[refreshTokenHash] = ByteArray(32) { 7 }
                        it[expiresAt] = moment
                        it[lastUsedAt] = moment
                    }
                    assertTrue(bool("SELECT expires_at = timestamptz '2026-12-18 02:30:00+00' FROM sessions WHERE id = '$sessionId'"))
                    assertEquals(moment, SessionsTable.selectAll().where { SessionsTable.id eq sessionId }.single()[SessionsTable.expiresAt])
                }
            }
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test fun `RN-CTA-03 el email se guarda normalizado y es unico`() = inTransaction {
        account("ana@example.com")
        assertRejected(UNIQUE, accountSql("ana@example.com"))
        assertRejected(CHECK, accountSql("Ana2@example.com"))
        assertRejected(CHECK, accountSql(" ana3@example.com"))
        val owner = account()
        assertRejected(CHECK, invitationSql(owner, "Invitado@example.com"))
        assertRejected(CHECK, emailTokenSql(owner, bytes = 32, newEmail = "Nuevo@example.com"))
    }

    @Test fun `I0-11 D4 solo se guardan hashes SHA-256 de tokens y contrasenas bcrypt`() = inTransaction {
        val owner = account()
        exec(sessionSql(owner, bytes = 32))
        assertRejected(CHECK, sessionSql(owner, bytes = 31))
        assertRejected(CHECK, sessionSql(owner, bytes = 33))
        exec(emailTokenSql(owner, bytes = 32))
        assertRejected(CHECK, emailTokenSql(owner, bytes = 16))
        exec(accountSql("con-bcrypt@example.com", passwordHash = BCRYPT_FORMAT))
        assertRejected(CHECK, accountSql("sin-hash@example.com", passwordHash = "contrasena123"))
    }

    @Test fun `RN-HIJ-06 un solo tutor principal vigente por nino`() = inTransaction {
        val (first, second, third) = List(3) { account() }
        val child = child(first)
        exec(guardianSql(child, first, "PRINCIPAL"))
        assertRejected(UNIQUE, guardianSql(child, second, "PRINCIPAL"))
        exec(guardianSql(child, second, "SECONDARY"))
        exec("UPDATE guardianships SET deleted_at = now() WHERE account_id = '$first'")
        exec(guardianSql(child, third, "PRINCIPAL"))
    }

    @Test fun `RN-HIJ-03 semanas de gestacion entre 22 y 42 y No lo se vale 40`() = inTransaction {
        val owner = account()
        child(owner, weeks = 22)
        child(owner, weeks = 42)
        child(owner, weeks = 40, unknown = true)
        assertRejected(CHECK, childSql(owner, weeks = 21))
        assertRejected(CHECK, childSql(owner, weeks = 43))
        assertRejected(CHECK, childSql(owner, weeks = 38, unknown = true))
    }

    @Test fun `RN-CRE-12 una medicion por autor y fecha sin contar las borradas`() = inTransaction {
        val (owner, other) = List(2) { account() }
        val child = child(owner)
        exec(measurementSql(child, owner, "2026-11-10"))
        assertRejected(UNIQUE, measurementSql(child, owner, "2026-11-10"))
        exec(measurementSql(child, other, "2026-11-10"))
        exec("UPDATE measurements SET deleted_at = now() WHERE author_account_id = '$owner'")
        exec(measurementSql(child, owner, "2026-11-10"))
        assertRejected(CHECK, measurementSql(child, owner, "2026-11-11", weightKg = null))
    }

    @Test fun `RN-VAC-09 una dosis por nino y el calendario no repite una dosis sin born_from`() = inTransaction {
        val owner = account()
        val child = child(owner)
        exec(appliedDoseSql(child, owner, "'HEPB_1'"))
        assertRejected(UNIQUE, appliedDoseSql(child, owner, "'HEPB_1'"))
        exec("UPDATE applied_doses SET deleted_at = now() WHERE child_id = '$child'")
        exec(appliedDoseSql(child, owner, "'HEPB_1'"))
        assertRejected(CHECK, appliedDoseSql(child, owner, "NULL"))

        val version = UUID.randomUUID()
        val vaccine = UUID.randomUUID()
        exec("INSERT INTO calendar_versions (id, effective_from, status) VALUES ('$version', '2026-01-01', 'PUBLISHED')")
        exec("INSERT INTO vaccines (id, name) VALUES ('$vaccine', 'Hepatitis B')")
        exec(calendarDoseSql(version, vaccine, bornFrom = "NULL"))
        assertRejected(UNIQUE, calendarDoseSql(version, vaccine, bornFrom = "NULL"))
        exec(calendarDoseSql(version, vaccine, bornFrom = "'2025-01-01'"))
        assertRejected(CHECK, calendarDoseSql(version, vaccine, bornFrom = "'2025-02-01'", bornUntil = "'2025-01-01'"))
    }

    @Test fun `I0-11 D5 lo sincronizable no se borra en cascada y las revocaciones sobreviven`() = inTransaction {
        val owner = account()
        val withGuardian = child(owner).also { exec(guardianSql(it, owner, "PRINCIPAL")) }
        val withMeasurement = child(owner).also { exec(measurementSql(it, owner, "2026-11-10")) }
        assertRejected(FOREIGN_KEY, "DELETE FROM children WHERE id = '$withGuardian'")
        assertRejected(FOREIGN_KEY, "DELETE FROM children WHERE id = '$withMeasurement'")
        assertRejected(FOREIGN_KEY, "DELETE FROM accounts WHERE id = '$owner'")

        val withInvitation = child(owner).also { exec(invitationSql(owner, "invitado@example.com", it)) }
        exec("DELETE FROM children WHERE id = '$withInvitation'")
        assertEquals(0, long("SELECT count(*) FROM invitations WHERE child_id = '$withInvitation'"))

        val leaving = account()
        exec(sessionSql(leaving, bytes = 32))
        exec("INSERT INTO consents (id, account_id, type, version, accepted_at) VALUES (gen_random_uuid(), '$leaving', 'TERMS', '1', now())")
        exec("INSERT INTO access_revocations (id, account_id, child_id, sync_version) VALUES (gen_random_uuid(), '$leaving', '$withGuardian', nextval('sync_seq'))")
        exec("DELETE FROM accounts WHERE id = '$leaving'")
        assertEquals(0, long("SELECT count(*) FROM sessions WHERE account_id = '$leaving'"))
        assertEquals(0, long("SELECT count(*) FROM consents WHERE account_id = '$leaving'"))
        assertEquals(1, long("SELECT count(*) FROM access_revocations WHERE account_id = '$leaving'"))
    }

    @Test fun `I0-11 set_sync_version da versiones crecientes y actualiza updated_at`() = inTransaction {
        exec("CREATE TEMP TABLE sync_probe (id int PRIMARY KEY, sync_version bigint, updated_at timestamptz)")
        exec("CREATE TRIGGER sync_probe_version BEFORE INSERT OR UPDATE ON sync_probe FOR EACH ROW EXECUTE FUNCTION set_sync_version()")
        exec("INSERT INTO sync_probe (id) VALUES (1)")
        exec("INSERT INTO sync_probe (id) VALUES (2)")
        val inserted = longs("SELECT sync_version FROM sync_probe ORDER BY id")
        exec("UPDATE sync_probe SET id = id WHERE id = 1")
        val updated = long("SELECT sync_version FROM sync_probe WHERE id = 1")
        assertTrue(inserted[0] < inserted[1] && inserted[1] < updated, "versiones: $inserted, $updated")
        assertEquals(0, long("SELECT count(*) FROM sync_probe WHERE updated_at IS NULL"))
    }

    private fun Transaction.assertBaseline() {
        assertEquals(BASELINE_TABLES.sorted(), strings(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' " +
                "AND table_name <> 'flyway_schema_history' ORDER BY table_name",
        ))
        assertEquals(listOf("sync_seq"), strings("SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'public'"))
        assertEquals(listOf("set_sync_version"), strings(
            "SELECT p.proname FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace WHERE n.nspname = 'public'",
        ))
        assertEquals(listOf("$BASELINE_VERSION true"), strings(
            "SELECT version || ' ' || success::text FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank",
        ))
    }

    /** Ejecuta [block] en una transacción que siempre se deshace. */
    private fun inTransaction(database: Database = factory.database, block: Transaction.() -> Unit) {
        transaction(database) {
            try { block() } finally { rollback() }
        }
    }

    /** Verifica que PostgreSQL rechace [sql] con [sqlState], y sigue con la transacción como estaba. */
    private fun Transaction.assertRejected(sqlState: String, sql: String) {
        val savepoint = connection.setSavepoint("expected_rejection")
        val error = assertFailsWith<ExposedSQLException>(sql) { exec(sql) }
        connection.rollback(savepoint)
        assertEquals(sqlState, error.sqlState, error.cause?.message)
    }

    private fun Transaction.strings(sql: String): List<String> =
        exec(sql) { rows -> buildList { while (rows.next()) add(rows.getString(1)) } }.orEmpty()
    private fun Transaction.longs(sql: String): List<Long> =
        exec(sql) { rows -> buildList { while (rows.next()) add(rows.getLong(1)) } }.orEmpty()
    private fun Transaction.long(sql: String): Long = longs(sql).single()
    private fun Transaction.bool(sql: String): Boolean = exec(sql) { rows -> rows.next() && rows.getBoolean(1) } == true

    private fun Transaction.account(email: String = "tutor-${UUID.randomUUID()}@example.com"): UUID =
        UUID.randomUUID().also { exec(accountSql(email, id = it)) }
    private fun Transaction.child(owner: UUID, weeks: Int = 40, unknown: Boolean = false): UUID =
        UUID.randomUUID().also { exec(childSql(owner, weeks, unknown, id = it)) }

    private fun accountSql(email: String, passwordHash: String? = null, id: UUID = UUID.randomUUID()) =
        "INSERT INTO accounts (id, email, first_name, last_name, password_hash, is_guardian) " +
            "VALUES ('$id', '$email', 'Ana', 'Prueba', ${passwordHash?.let { "'$it'" } ?: "NULL"}, true)"
    private fun childSql(owner: UUID, weeks: Int = 40, unknown: Boolean = false, id: UUID = UUID.randomUUID()) =
        "INSERT INTO children (id, first_name, last_name, sex, birth_date, gestational_weeks, " +
            "gestational_weeks_unknown, created_by_account_id, sync_version) " +
            "VALUES ('$id', 'Luz', 'Prueba', 'FEMALE', '2026-01-10', $weeks, $unknown, '$owner', nextval('sync_seq'))"
    private fun guardianSql(child: UUID, account: UUID, role: String) =
        "INSERT INTO guardianships (child_id, account_id, role, sync_version) " +
            "VALUES ('$child', '$account', '$role', nextval('sync_seq'))"
    private fun invitationSql(inviter: UUID, email: String, child: UUID? = null) =
        "INSERT INTO invitations (id, type, child_id, inviter_account_id, email, sent_at, expires_at) " +
            "VALUES (gen_random_uuid(), 'GUARDIAN', ${child?.let { "'$it'" } ?: "NULL"}, '$inviter', '$email', " +
            "now(), now() + interval '15 days')"
    private fun sessionSql(account: UUID, bytes: Int) =
        "INSERT INTO sessions (id, account_id, refresh_token_hash, expires_at, last_used_at) " +
            "VALUES (gen_random_uuid(), '$account', decode(repeat('ab', $bytes), 'hex'), now() + interval '30 days', now())"
    private fun emailTokenSql(account: UUID, bytes: Int, newEmail: String? = null) =
        "INSERT INTO email_tokens (id, account_id, purpose, token_hash, new_email, expires_at) " +
            "VALUES (gen_random_uuid(), '$account', 'CHANGE_EMAIL', decode(repeat('cd', $bytes), 'hex'), " +
            "${newEmail?.let { "'$it'" } ?: "NULL"}, now() + interval '1 day')"
    private fun measurementSql(child: UUID, author: UUID, measuredOn: String, weightKg: String? = "7.250") =
        "INSERT INTO measurements (id, child_id, measured_on, weight_kg, author_account_id, author_role, sync_version) " +
            "VALUES (gen_random_uuid(), '$child', '$measuredOn', ${weightKg ?: "NULL"}, '$author', 'GUARDIAN', nextval('sync_seq'))"
    private fun appliedDoseSql(child: UUID, author: UUID, doseCode: String) =
        "INSERT INTO applied_doses (id, child_id, dose_code, author_account_id, author_role, sync_version) " +
            "VALUES (gen_random_uuid(), '$child', $doseCode, '$author', 'GUARDIAN', nextval('sync_seq'))"
    private fun calendarDoseSql(version: UUID, vaccine: UUID, bornFrom: String, bornUntil: String = "NULL") =
        "INSERT INTO calendar_doses (id, version_id, vaccine_id, dose_code, dose_label, age_months, lead_days, " +
            "tolerance_days, born_from, born_until, sort_order) VALUES (gen_random_uuid(), '$version', '$vaccine', " +
            "'HEPB_1', 'Primera dosis', 0, 0, 0, $bornFrom, $bornUntil, 1)"
}
