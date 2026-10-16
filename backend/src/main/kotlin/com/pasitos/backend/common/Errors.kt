package com.pasitos.backend.common

import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond

/**
 * Error que la API devuelve como [ErrorResponse] (11 · Backend §5), con un mensaje en español
 * listo para mostrar. Los servicios lo lanzan sin conocer HTTP; [configureErrorHandling] elige el
 * estado según el tipo.
 */
open class ApiException(val code: ErrorCode, override val message: String) : RuntimeException(message)

open class InvalidInputException(message: String) : ApiException(ErrorCode.INVALID_INPUT, message)

open class UnauthorizedException(code: ErrorCode, message: String) : ApiException(code, message)

open class NotFoundException(code: ErrorCode, message: String) : ApiException(code, message)

open class ConflictException(code: ErrorCode, message: String) : ApiException(code, message)

/** Recurso bloqueado por un tiempo, por ejemplo la cuenta tras varios intentos fallidos (RN-CTA-05). */
open class LockedException(code: ErrorCode, message: String) : ApiException(code, message)

/** Regla de negocio incumplida; [rule] es su ID en la SPEC, por ejemplo `RN-CRE-12`. */
class BusinessRuleException(val rule: String, message: String) : ApiException(ErrorCode.BUSINESS_RULE, message)

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            val rule = (cause as? BusinessRuleException)?.rule
            call.respond(cause.httpStatus(), ErrorResponse(cause.code, cause.message, rule = rule))
        }
        // El plugin RateLimit responde 429 sin cuerpo: se le agrega el formato de error de la API.
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respond(status, ErrorResponse(ErrorCode.RATE_LIMITED, "Hiciste demasiados pedidos seguidos. Esperá un minuto y probá de nuevo"))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(ErrorCode.INVALID_INPUT, "El formato del pedido no es válido"),
            )
        }
        exception<Throwable> { call, cause ->
            // Solo la clase y la ruta: el mensaje puede traer datos personales (RNF-PRI-03).
            call.application.log.error("Error inesperado en ${call.request.path()}: ${cause::class.qualifiedName}")
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(ErrorCode.INTERNAL_ERROR, "Ocurrió un error, intentá de nuevo"),
            )
        }
    }
}

private fun ApiException.httpStatus(): HttpStatusCode = when (this) {
    is InvalidInputException -> HttpStatusCode.BadRequest
    is UnauthorizedException -> HttpStatusCode.Unauthorized
    is NotFoundException -> HttpStatusCode.NotFound
    is ConflictException -> HttpStatusCode.Conflict
    is LockedException -> HttpStatusCode.Locked
    is BusinessRuleException -> HttpStatusCode.UnprocessableEntity
    else -> HttpStatusCode.InternalServerError
}
