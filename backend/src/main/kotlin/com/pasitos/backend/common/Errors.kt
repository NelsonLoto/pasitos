package com.pasitos.backend.common

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

/**
 * Formato único de error (11 · Backend §5). La clave es `error` porque es la que lee la app
 * actual; I0-02 mueve este DTO a `shared`.
 */
@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
)

/**
 * Error que la API devuelve con un código y un mensaje en español listo para mostrar. Los
 * servicios lo lanzan sin conocer HTTP; [configureErrorHandling] elige el estado según el tipo.
 */
open class ApiException(val code: String, override val message: String) : RuntimeException(message)

open class InvalidInputException(message: String) : ApiException("invalid_input", message)

open class UnauthorizedException(code: String, message: String) : ApiException(code, message)

open class NotFoundException(code: String, message: String) : ApiException(code, message)

open class ConflictException(code: String, message: String) : ApiException(code, message)

/** Regla de negocio incumplida; [rule] es su ID en la SPEC, por ejemplo `RN-CRE-12`. */
class BusinessRuleException(val rule: String, message: String) : ApiException("business_rule", message)

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respond(cause.httpStatus(), ErrorResponse(cause.code, cause.message))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_input", "El formato del pedido no es válido"))
        }
        exception<Throwable> { call, cause ->
            // Solo la clase y la ruta: el mensaje puede traer datos personales (RNF-PRI-03).
            call.application.log.error("Error inesperado en ${call.request.path()}: ${cause::class.qualifiedName}")
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error", "Ocurrió un error, intentá de nuevo"))
        }
    }
}

private fun ApiException.httpStatus(): HttpStatusCode = when (this) {
    is InvalidInputException -> HttpStatusCode.BadRequest
    is UnauthorizedException -> HttpStatusCode.Unauthorized
    is NotFoundException -> HttpStatusCode.NotFound
    is ConflictException -> HttpStatusCode.Conflict
    is BusinessRuleException -> HttpStatusCode.UnprocessableEntity
    else -> HttpStatusCode.InternalServerError
}
