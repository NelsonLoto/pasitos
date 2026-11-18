package com.pasitos.backend.common

import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.coroutines.CancellationException

/** Error de dominio que se responde tal cual; ErrorResponse y ErrorCode viven en shared/api (I0-02). */
class ApiException(val status: HttpStatusCode, val error: ErrorResponse) : RuntimeException(error.code.wireName)

fun Application.configureErrors() {
    val errorLogger = org.slf4j.LoggerFactory.getLogger("com.pasitos.backend.Errors")
    install(StatusPages) {
        exception<ApiException> { call, cause -> call.respond(cause.status, cause.error) }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(ErrorCode.INVALID_INPUT, "El formato de la solicitud no es válido"))
        }
        exception<Throwable> { call, cause ->
            if (cause is CancellationException) throw cause
            // No incluir mensajes ni stacktraces que puedan contener datos del request o credenciales.
            errorLogger.error("Error inesperado: {}", cause.javaClass.simpleName)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(ErrorCode.INTERNAL_ERROR, "Ocurrió un error inesperado"))
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ErrorResponse(ErrorCode.NOT_FOUND, "No se encontró ese recurso"))
        }
    }
}
