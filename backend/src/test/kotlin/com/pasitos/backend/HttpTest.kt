package com.pasitos.backend

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.pasitos.backend.common.ApiException
import com.pasitos.backend.common.ErrorResponse
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HttpTest {
    @Test fun `I0-04 health informa indisponibilidad de base con 503`() = testApplication {
        application { configureHttp { false } }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertTrue(response.bodyAsText().contains("unavailable"))
    }

    @Test fun `I0-04 errores JSON no exponen excepciones y conservan errores de dominio`() = testApplication {
        application {
            configureHttp { true }
            routing {
                get("/invalid") { throw BadRequestException("synthetic-private-input") }
                get("/failure") { error("synthetic-private-input") }
                get("/domain") { throw ApiException(HttpStatusCode.Conflict, ErrorResponse("conflict", "Hay un conflicto")) }
            }
        }
        for ((path, status, code) in listOf(
            Triple("/missing", HttpStatusCode.NotFound, "not_found"),
            Triple("/invalid", HttpStatusCode.BadRequest, "invalid_input"),
            Triple("/failure", HttpStatusCode.InternalServerError, "internal_error"),
            Triple("/domain", HttpStatusCode.Conflict, "conflict"),
        )) {
            val response = client.get(path)
            assertEquals(status, response.status)
            val error = Json.decodeFromString<ErrorResponse>(response.bodyAsText())
            assertEquals(code, error.code)
            assertFalse(response.bodyAsText().contains("synthetic-private-input"))
        }
    }

    @Test fun `I0-04 logs omiten consulta email token y health y generan request id propio`() = testApplication {
        val logger = LoggerFactory.getLogger("com.pasitos.backend.Requests") as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            application { configureHttp { true } }
            val response = client.get("/synthetic@example.test?token=synthetic-private-input") {
                headers.append("X-Request-Id", "synthetic-client-input")
            }
            val id = response.headers["X-Request-Id"]!!
            UUID.fromString(id)
            assertNotEquals("synthetic-client-input", id)
            val requestLog = appender.list.single { it.formattedMessage.startsWith("GET ") }
            assertTrue(requestLog.formattedMessage.startsWith("GET /* 404"))
            assertEquals(id, requestLog.mdcPropertyMap["call-id"])
            val beforeHealth = appender.list.size
            client.get("/health")
            assertEquals(beforeHealth, appender.list.size)
            val logs = appender.list.joinToString { it.formattedMessage }
            for (secret in listOf("example.test", "synthetic-private-input", "synthetic-client-input")) assertFalse(logs.contains(secret))
        } finally { logger.detachAppender(appender); appender.stop() }
    }
}
