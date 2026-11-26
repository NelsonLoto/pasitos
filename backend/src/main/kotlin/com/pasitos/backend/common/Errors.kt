package com.pasitos.backend.common

import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import com.pasitos.backend.auth.AuthFailure
import com.pasitos.backend.sync.SyncFailure
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
        exception<SyncFailure> { call, cause ->
            val status = if (cause.code == ErrorCode.NOT_FOUND) HttpStatusCode.NotFound else HttpStatusCode.BadRequest
            call.respond(status, ErrorResponse(cause.code, cause.message))
        }
        exception<AuthFailure> { call, cause ->
            val status = when (cause.code) {
                ErrorCode.INVALID_INPUT -> HttpStatusCode.BadRequest
                ErrorCode.EMAIL_ALREADY_IN_USE, ErrorCode.TERMS_REQUIRED -> HttpStatusCode.Conflict
                ErrorCode.ACCOUNT_LOCKED -> HttpStatusCode(423, "Locked")
                else -> HttpStatusCode.Unauthorized
            }
            call.respond(status, ErrorResponse(cause.code, cause.message, cause.fields))
        }
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
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respond(status, ErrorResponse(ErrorCode.RATE_LIMITED, "Demasiados pedidos. Esperá un momento y volvé a intentar"))
        }
    }
}
