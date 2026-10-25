package com.pasitos.backend.common

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.pasitos.backend.support.apiTest
import com.pasitos.backend.support.register
import com.pasitos.shared.api.auth.LoginRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.plugin
import io.ktor.server.routing.RoutingRoot
import io.ktor.server.routing.getAllRoutes
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.slf4j.LoggerFactory

/** Una línea por pedido, sin datos personales (11 · Backend §8, RNF-PRI-03; #122). */
class RequestLoggingTest {

    private val root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
    private val appender = ListAppender<ILoggingEvent>()
    private var previousLevel: Level? = null

    /** Captura todo lo que se registra en INFO o más, de cualquier logger: `logback-test.xml` deja solo WARN. */
    @BeforeTest
    fun captureLogs() {
        previousLevel = root.level
        root.level = Level.INFO
        appender.start()
        root.addAppender(appender)
    }

    @AfterTest
    fun restoreLogs() {
        root.detachAppender(appender)
        appender.stop()
        root.level = previousLevel
    }

    @Test
    fun `cada pedido deja una línea con método, ruta sin consulta, estado, duración e identificador`() = apiTest { client ->
        val account = client.register()
        val childId = UUID.randomUUID()

        val response = client.get("/api/v1/children/$childId?nombre=Lucia") { bearerAuth(account.tokens.accessToken) }

        assertEquals(HttpStatusCode.NotFound, response.status)
        val line = awaitRequestLine { it.formattedMessage.startsWith("GET /api/v1/children/") }
        assertTrue(Regex("^GET /api/v1/children/$childId 404 \\d+ ms$").matches(line.formattedMessage), line.formattedMessage)
        val callId = assertNotNull(response.headers[HttpHeaders.XRequestId])
        assertEquals(callId, line.mdcPropertyMap[CALL_ID_MDC])
        assertFalse(allLogText().contains("Lucia"))
    }

    @Test
    fun `un pedido con email y contraseña en el cuerpo no deja esos datos en el log`() = apiTest { client ->
        client.register(email = "privada@example.com", password = "secreta123")

        client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email = "privada@example.com", password = "secreta123"))
        }

        awaitRequestLine { it.formattedMessage.startsWith("POST /api/v1/auth/login ") }
        val logged = allLogText()
        assertFalse(logged.contains("privada@example.com"), "El email quedó en el log")
        assertFalse(logged.contains("secreta123"), "La contraseña quedó en el log")
        assertFalse(logged.contains("Pérez"), "El apellido quedó en el log")
    }

    @Test
    fun `el identificador lo genera el servidor aunque el cliente mande uno`() = apiTest { client ->
        val response = client.get("/api/v1/me") { header(HttpHeaders.XRequestId, "inventado-por-el-cliente") }

        val callId = assertNotNull(response.headers[HttpHeaders.XRequestId])
        assertNotEquals("inventado-por-el-cliente", callId)
        UUID.fromString(callId)
        val line = awaitRequestLine { it.formattedMessage.startsWith("GET /api/v1/me ") }
        assertEquals(callId, line.mdcPropertyMap[CALL_ID_MDC])
        assertFalse(allLogText().contains("inventado-por-el-cliente"))
    }

    @Test
    fun `health no se registra`() = apiTest { client ->
        client.get("/health")
        client.get("/api/v1/me")

        awaitRequestLine { it.formattedMessage.startsWith("GET /api/v1/me ") }
        assertTrue(requestLines().none { it.formattedMessage.contains("/health") })
    }

    @Test
    fun `una ruta inexistente no deja en el log el email ni el token que escribió el cliente`() = apiTest { client ->
        client.get("/api/v1/children/ana@example.com/guardians")
        client.get("/api/v1/me/devices/fGx1:APA91bH_token")

        awaitRequestLine { it.formattedMessage.startsWith("GET /api/v1/me/devices/") }
        val messages = requestLines().map { it.formattedMessage }
        assertTrue(messages.any { it.startsWith("GET /api/v1/children/*/guardians ") }, messages.toString())
        assertTrue(messages.any { it.startsWith("GET /api/v1/me/devices/* ") }, messages.toString())
        assertFalse(allLogText().contains("ana@example.com"))
        assertFalse(allLogText().contains("APA91bH"))
    }

    @Test
    fun `la ruta conserva los UUID y las palabras, y tapa lo demás`() {
        val id = "0f8fad5b-d9cb-469f-a165-70867728950e"
        assertEquals("/api/v1/children/$id/guardian-invitations", loggablePath("/api/v1/children/$id/guardian-invitations"))
        assertEquals("/api/v1/children/*", loggablePath("/api/v1/children/ana%40example.com"))
        assertEquals("/api/v1/me/devices/*", loggablePath("/api/v1/me/devices/fGx1:APA91bH"))
        assertEquals("/web/*", loggablePath("/web/3f2a9c0d1e4b5a6978"))
        assertEquals("/", loggablePath("/"))
    }

    @Test
    fun `toda ruta registrada solo tiene parámetros que se pueden registrar`() = apiTest {
        startApplication()

        val routes = application.plugin(RoutingRoot).getAllRoutes().map { it.toString() }
        val parameters = routes.flatMap { route -> PathParameter.findAll(route).map { it.groupValues[1] } }.toSet()

        // Que la prueba no pase por no encontrar nada
        assertTrue("id" in parameters, routes.toString())
        val unexpected = parameters - LOGGABLE_PATH_PARAMETERS
        assertTrue(
            unexpected.isEmpty(),
            "Parámetros de ruta que irían al log tal cual: $unexpected. Si no son UUID, enmascaralos en loggablePath " +
                "antes de agregarlos a LOGGABLE_PATH_PARAMETERS (11 · Backend §8).",
        )
    }

    private fun events(): List<ILoggingEvent> = synchronized(appender) { appender.list.toList() }

    private fun requestLines(): List<ILoggingEvent> = events().filter { it.loggerName == REQUEST_LOGGER }

    /** Todo lo registrado, con el MDC, para buscar datos que no deberían estar. */
    private fun allLogText(): String =
        events().joinToString("\n") { "${it.loggerName} ${it.formattedMessage} ${it.mdcPropertyMap} ${it.throwableProxy?.message.orEmpty()}" }

    /** CallLogging escribe después de mandar la respuesta: la línea puede llegar un poco más tarde. */
    private fun awaitRequestLine(predicate: (ILoggingEvent) -> Boolean): ILoggingEvent {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            requestLines().firstOrNull(predicate)?.let { return it }
            Thread.sleep(20)
        }
        error("No apareció la línea esperada. Hay: ${requestLines().map { it.formattedMessage }}")
    }

    private companion object {
        val PathParameter = Regex("\\{([A-Za-z0-9_]+)\\??\\}")
    }
}
