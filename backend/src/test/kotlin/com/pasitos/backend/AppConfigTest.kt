package com.pasitos.backend

import com.pasitos.backend.common.AppConfig
import com.pasitos.backend.common.AppEnvironment
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class AppConfigTest {
    private val values = mapOf("DB_HOST" to "localhost", "DB_NAME" to "pasitos_test",
        "DB_USER" to "test", "DB_PASSWORD" to "synthetic-password", "JWT_SECRET" to "test-only-".repeat(8))

    @Test fun `I0-04 configura local ci y staging sin valores secretos por defecto`() {
        for ((value, expected) in listOf("local" to AppEnvironment.LOCAL, "ci" to AppEnvironment.CI, "staging" to AppEnvironment.STAGING)) {
            val config = AppConfig.from(values + ("APP_ENV" to value))
            assertEquals(expected, config.environment)
            assertEquals("jdbc:postgresql://localhost:5432/pasitos_test", config.database.jdbcUrl)
            assertFalse(config.toString().contains("synthetic-password"))
            assertFalse(config.toString().contains(values.getValue("JWT_SECRET")))
        }
    }

    @Test fun `I0-05 exige clave larga y limite positivo y usa diez por defecto`() {
        assertEquals(10, AppConfig.from(values).auth.requestsPerMinute)
        assertEquals(20, AppConfig.from(values + ("AUTH_RATE_LIMIT" to "20")).auth.requestsPerMinute)
        for (extra in listOf("JWT_SECRET" to "short", "TERMS_VERSION" to " ",
            "AUTH_RATE_LIMIT" to "0", "AUTH_RATE_LIMIT" to "-1")) {
            assertFailsWith<IllegalArgumentException> { AppConfig.from(values + extra) }
        }
        assertFailsWith<IllegalStateException> { AppConfig.from(values + ("AUTH_RATE_LIMIT" to "text")) }
    }

    @Test fun `I0-04 rechaza configuracion incompleta entorno desconocido y puertos invalidos`() {
        for (key in listOf("DB_HOST", "DB_NAME", "DB_USER", "DB_PASSWORD", "JWT_SECRET")) {
            assertFailsWith<IllegalStateException> { AppConfig.from(values - key) }
        }
        for (extra in listOf("APP_ENV" to "production", "PORT" to "0", "DB_PORT" to "65536", "DB_PORT" to "texto")) {
            assertFailsWith<IllegalStateException> { AppConfig.from(values + extra) }
        }
        assertFailsWith<IllegalArgumentException> { AppConfig.from(values + ("DB_HOST" to "host/?password=secreto")) }
    }
}
