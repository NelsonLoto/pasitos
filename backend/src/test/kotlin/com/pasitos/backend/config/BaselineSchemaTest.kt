package com.pasitos.backend.config

import com.pasitos.backend.support.TestDatabase
import java.sql.Connection
import java.sql.SQLException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Restricciones de la línea base que la API todavía no ejercita (13 · Base de datos). */
class BaselineSchemaTest {

    @BeforeTest
    fun cleanDatabase() {
        TestDatabase.reset()
    }

    @Test
    fun `la línea base crea las tablas de I0 e I1`() {
        val tables = TestDatabase.column<String>("SELECT tablename FROM pg_tables WHERE schemaname = 'public'")

        val expected = setOf(
            "accounts", "consents", "sessions", "email_tokens", "children", "guardianships", "invitations",
            "measurements", "vaccines", "calendar_versions", "calendar_doses", "applied_doses",
            "other_vaccines", "access_revocations",
        )
        assertEquals(expected, tables.toSet() - "flyway_schema_history")
    }

    @Test
    fun `cada cambio en una tabla sincronizable recibe una versión mayor`() = withDb { db ->
        val accountId = db.insertAccount("ana@example.com")
        val childId = db.insertChild(accountId)
        val created = db.syncVersionOf(childId)

        db.exec("UPDATE children SET avatar_key = 'oso' WHERE id = '$childId'")

        assertTrue(db.syncVersionOf(childId) > created)
        assertEquals(listOf(true), TestDatabase.column<Boolean>("SELECT updated_at IS NOT NULL FROM children"))
    }

    @Test
    fun `RN-HIJ-06 un niño no puede tener dos tutores principales`() = withDb { db ->
        val ana = db.insertAccount("ana@example.com")
        val beto = db.insertAccount("beto@example.com")
        val childId = db.insertChild(ana)
        db.exec("INSERT INTO guardianships (child_id, account_id, role) VALUES ('$childId', '$ana', 'PRINCIPAL')")

        assertFailsWith<SQLException> {
            db.exec("INSERT INTO guardianships (child_id, account_id, role) VALUES ('$childId', '$beto', 'PRINCIPAL')")
        }
        db.exec("INSERT INTO guardianships (child_id, account_id, role) VALUES ('$childId', '$beto', 'SECONDARY')")
    }

    @Test
    fun `RN-CRE-12 la base rechaza dos mediciones del mismo autor y fecha, salvo que una esté borrada`() =
        withDb { db ->
            val ana = db.insertAccount("ana@example.com")
            val childId = db.insertChild(ana)
            val insert = "INSERT INTO measurements (child_id, measured_on, weight_kg, author_account_id, author_role) " +
                "VALUES ('$childId', '2026-10-01', 6.4, '$ana', 'GUARDIAN')"
            db.exec(insert)

            assertFailsWith<SQLException> { db.exec(insert) }

            db.exec("UPDATE measurements SET deleted_at = now()")
            db.exec(insert)
        }

    @Test
    fun `una medición sin valores o con longitud sin forma de medición se rechaza`() = withDb { db ->
        val ana = db.insertAccount("ana@example.com")
        val childId = db.insertChild(ana)

        assertFailsWith<SQLException> {
            db.exec(
                "INSERT INTO measurements (child_id, measured_on, author_account_id, author_role) " +
                    "VALUES ('$childId', '2026-10-01', '$ana', 'GUARDIAN')",
            )
        }
        assertFailsWith<SQLException> {
            db.exec(
                "INSERT INTO measurements (child_id, measured_on, length_cm, author_account_id, author_role) " +
                    "VALUES ('$childId', '2026-10-01', 62.5, '$ana', 'GUARDIAN')",
            )
        }
    }

    @Test
    fun `el email de una cuenta se guarda en minúsculas`() = withDb { db ->
        assertFailsWith<SQLException> { db.insertAccount("Ana@Example.com") }
    }

    /** JUnit exige que las pruebas no devuelvan nada: el bloque se adapta a Unit. */
    private fun withDb(block: (Connection) -> Unit) = TestDatabase.connect().use(block)

    private fun Connection.exec(sql: String) {
        createStatement().use { it.execute(sql) }
    }

    private fun Connection.insertAccount(email: String): String = createStatement().use { statement ->
        statement.executeQuery(
            "INSERT INTO accounts (email, first_name, last_name, password_hash) VALUES ('$email', 'Ana', 'Pérez', 'hash') RETURNING id",
        ).use { rows ->
            rows.next()
            rows.getString(1)
        }
    }

    private fun Connection.insertChild(createdBy: String): String = createStatement().use { statement ->
        statement.executeQuery(
            "INSERT INTO children (first_name, last_name, sex, birth_date, gestational_weeks, created_by_account_id) " +
                "VALUES ('Sofi', 'Pérez', 'FEMALE', '2026-06-15', 39, '$createdBy') RETURNING id",
        ).use { rows ->
            rows.next()
            rows.getString(1)
        }
    }

    private fun Connection.syncVersionOf(childId: String): Long = createStatement().use { statement ->
        statement.executeQuery("SELECT sync_version FROM children WHERE id = '$childId'").use { rows ->
            rows.next()
            rows.getLong(1)
        }
    }
}
