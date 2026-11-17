package com.pasitos.backend.common

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

// Se mueve a shared/api con I0-02; no hay contratos duplicados mientras shared no existe.
@Serializable
data class ErrorResponse(val code: String, val message: String, val fields: Map<String, String>? = null)

class ApiException(val status: HttpStatusCode, val error: ErrorResponse) : RuntimeException(error.code)

fun Application.configureErrors() {
    val errorLogger = org.slf4j.LoggerFactory.getLogger("com.pasitos.backend.Errors")
    install(StatusPages) {
        exception<ApiException> { call, cause -> call.respond(cause.status, cause.error) }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_input", "El formato de la solicitud no es válido"))
        }
        exception<Throwable> { call, cause ->
            if (cause is CancellationException) throw cause
            // No incluir mensajes ni stacktraces que puedan contener datos del request o credenciales.
            errorLogger.error("Error inesperado: {}", cause.javaClass.simpleName)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error", "Ocurrió un error inesperado"))
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ErrorResponse("not_found", "No se encontró ese recurso"))
        }
    }
}
