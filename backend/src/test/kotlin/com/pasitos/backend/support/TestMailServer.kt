package com.pasitos.backend.support

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

/**
 * Mailpit real en Docker (la misma imagen que `docker-compose.yml`, fijada a la misma versión),
 * uno solo para toda la corrida de pruebas: el servidor SMTP de prueba que pide el criterio de
 * aceptación de I1-B1 (17 · Notificaciones y emails §3). Se consulta por su API HTTP para
 * verificar lo que el backend efectivamente mandó.
 */
object TestMailServer {

    private const val SMTP_PORT = 1025
    private const val HTTP_PORT = 8025

    private val container = GenericContainer("axllent/mailpit:v1.31.4")
        .withExposedPorts(SMTP_PORT, HTTP_PORT)
        .waitingFor(Wait.forHttp("/").forPort(HTTP_PORT))
        .apply { start() }

    private val httpClient = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val apiBase: String get() = "http://${container.host}:${container.getMappedPort(HTTP_PORT)}/api/v1"

    /** Configuración del backend apuntando a este contenedor. */
    fun config(): Map<String, String> = mapOf(
        "mail.smtpHost" to container.host,
        "mail.smtpPort" to container.getMappedPort(SMTP_PORT).toString(),
        "mail.smtpUser" to "",
        "mail.smtpPassword" to "",
        "mail.from" to "no-responder@pasitos.ar",
        "public.baseUrl" to "http://pasitos-test.local",
    )

    /** Borra todos los emails capturados, para que cada prueba arranque sin mensajes previos. */
    fun reset() {
        send(HttpRequest.newBuilder(URI.create("$apiBase/messages")).DELETE())
    }

    /**
     * Espera (sondeando, hasta [timeout]) el último email mandado a [to] y devuelve su asunto y
     * ambos cuerpos. El envío es asíncrono respecto de la respuesta HTTP que lo dispara (17 ·
     * Notificaciones y emails §3), por eso la espera.
     */
    fun awaitMessageTo(to: String, timeout: Duration = Duration.ofSeconds(10)): MailpitMessage? {
        val deadline = System.currentTimeMillis() + timeout.toMillis()
        while (System.currentTimeMillis() < deadline) {
            val id = latestMessageIdTo(to)
            if (id != null) return fetchMessage(id)
            Thread.sleep(100)
        }
        return null
    }

    private fun latestMessageIdTo(to: String): String? {
        val response = send(HttpRequest.newBuilder(URI.create("$apiBase/messages?limit=50")).GET())
        val list = json.decodeFromString<MailpitListResponse>(response.body())
        return list.messages.firstOrNull { summary -> summary.to.any { it.address.equals(to, ignoreCase = true) } }?.id
    }

    private fun fetchMessage(id: String): MailpitMessage {
        val response = send(HttpRequest.newBuilder(URI.create("$apiBase/message/$id")).GET())
        return json.decodeFromString(response.body())
    }

    private fun send(builder: HttpRequest.Builder): HttpResponse<String> =
        httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
}

@Serializable
data class MailpitListResponse(val messages: List<MailpitSummary> = emptyList())

@Serializable
data class MailpitSummary(
    @SerialName("ID") val id: String,
    @SerialName("To") val to: List<MailpitAddress> = emptyList(),
)

@Serializable
data class MailpitAddress(@SerialName("Address") val address: String = "")

@Serializable
data class MailpitMessage(
    @SerialName("Subject") val subject: String = "",
    @SerialName("Text") val text: String = "",
    @SerialName("HTML") val html: String = "",
)
