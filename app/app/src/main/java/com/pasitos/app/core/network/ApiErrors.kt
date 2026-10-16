package com.pasitos.app.core.network

import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** Error con el formato de la API ([ErrorResponse]). El mensaje viene del servidor, listo para mostrar. */
class ApiException(val code: ErrorCode, message: String) : Exception(message)

/** El pedido no llegó al servidor o la respuesta no volvió. */
class NoConnectionException(cause: IOException) :
    Exception("No pudimos conectarnos. Revisá tu conexión y probá de nuevo.", cause)

/** Convierte una respuesta de error en [ApiException]. */
suspend fun HttpResponse.toApiException(): ApiException {
    val error = try {
        body<ErrorResponse>()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
    return ApiException(error?.code ?: ErrorCode.UNKNOWN, error?.message ?: "Ocurrió un error, intentá de nuevo")
}

/** Ejecuta un pedido y cambia los errores de red por [NoConnectionException]. */
suspend fun <T> withConnection(request: suspend () -> T): T =
    try {
        request()
    } catch (e: IOException) {
        throw NoConnectionException(e)
    }

/**
 * Para los pedidos que no usan la sesión: registro e inicio de sesión. No llevan el token y un 401
 * no dispara una renovación. Sin esto, si hubiera tokens guardados, un login con la contraseña
 * equivocada renovaría la sesión y se reenviaría: el error contaría doble para el bloqueo por
 * intentos (RN-CTA-05).
 */
fun HttpRequestBuilder.withoutSession() {
    attributes.put(AuthCircuitBreaker, Unit)
}
