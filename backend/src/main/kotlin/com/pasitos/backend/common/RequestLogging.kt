package com.pasitos.backend.common

import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import java.util.UUID
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

/** Logger de la línea de cada pedido. */
const val REQUEST_LOGGER = "com.pasitos.backend.Requests"

/** Clave del identificador del pedido en el MDC: `logback.xml` la imprime en cada línea. */
const val CALL_ID_MDC = "call-id"

/**
 * Parámetros de ruta que se pueden registrar tal cual: todos son UUID (11 · Backend §8). Una ruta
 * con otro parámetro, por ejemplo `{fcmToken}`, hace fallar `RequestLoggingTest` hasta que se decida
 * cómo registrarla.
 */
val LOGGABLE_PATH_PARAMETERS = setOf("id", "childId", "accountId")

/**
 * Una línea por pedido con método, ruta sin parámetros de consulta, estado y duración, y el
 * identificador del pedido en el MDC (11 · Backend §8). Nunca cuerpos, tokens, emails, nombres ni
 * datos de salud (RNF-PRI-03).
 */
fun Application.configureRequestLogging() {
    // Lo genera siempre el servidor: lo que mande el cliente no llega al log. Vuelve en X-Request-Id
    // para cruzar un error que ve la app con su línea del log
    install(CallId) {
        generate { UUID.randomUUID().toString() }
        replyToHeader(HttpHeaders.XRequestId)
    }
    install(CallLogging) {
        logger = LoggerFactory.getLogger(REQUEST_LOGGER)
        level = Level.INFO
        callIdMdc(CALL_ID_MDC)
        disableDefaultColors()
        // Docker la consulta seguido y no aporta nada
        filter { call -> call.request.path() != "/health" }
        format { call ->
            val status = call.response.status()?.value?.toString() ?: "-"
            "${call.request.httpMethod.value} ${loggablePath(call.request.path())} $status ${call.processingTimeMillis()} ms"
        }
    }
}

private val UuidSegment = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
private val WordSegment = Regex("^[a-z]+(-[a-z]+)*$")
private val VersionSegment = Regex("^v[0-9]+$")

/**
 * La ruta con cada tramo que no sea un UUID, una palabra en minúsculas con guiones o la versión de
 * la API cambiado por `*`. Cubre los pedidos a rutas que no existen, donde el cliente puede escribir
 * cualquier cosa: un email lleva `@` y un token, mayúsculas, números o `:`.
 */
fun loggablePath(path: String): String =
    path.split('/').joinToString("/") { segment ->
        val safe = segment.isEmpty() ||
            UuidSegment.matches(segment) ||
            (segment.length <= 40 && (WordSegment.matches(segment) || VersionSegment.matches(segment)))
        if (safe) segment else "*"
    }
