package com.pasitos.backend

import com.pasitos.backend.common.AppConfig
import com.pasitos.backend.common.DatabaseFactory
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Sin disabledWithoutDocker: la ausencia de PostgreSQL real hace fallar la suite.
@Testcontainers
class PostgresIntegrationTest {
    companion object {
        @Container @JvmStatic val postgres = PostgreSQLContainer("postgres:16-alpine")
    }
    private fun config() = AppConfig.from(mapOf(
        "APP_ENV" to "ci", "DB_HOST" to postgres.host, "JWT_SECRET" to "test-only-".repeat(8),
        "DB_PORT" to postgres.firstMappedPort.toString(), "DB_NAME" to postgres.databaseName,
        "DB_USER" to postgres.username, "DB_PASSWORD" to postgres.password,
    ))

    @Test fun `I0-04 CA1 health responde 200 con PostgreSQL 16 real sin autenticacion`() = testApplication {
        application { module(config()) }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("{\"status\":\"ok\",\"database\":\"connected\"}", response.bodyAsText())
    }

    @Test fun `I0-04 CA2 conecta Exposed ejecuta Flyway y libera el pool`() {
        DatabaseFactory(config().database).use { factory ->
            assertTrue(factory.isHealthy())
            transaction(factory.database) {
                assertTrue(exec("SHOW server_version") { it.next(); it.getString(1).startsWith("16.") } == true)
                assertTrue(exec("SELECT to_regclass('public.flyway_schema_history') IS NOT NULL") { it.next(); it.getBoolean(1) } == true)
            }
        }
        // Otra conexión después de cerrar la primera verifica que no queda estado global requerido.
        DatabaseFactory(config().database).use { assertTrue(it.isHealthy()) }
    }
}
