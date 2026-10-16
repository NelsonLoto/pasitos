package com.pasitos.app.core.network

import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.ErrorResponse
import com.pasitos.shared.api.auth.RefreshRequest
import com.pasitos.shared.api.auth.TokenPair
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.serialization.json.Json

/**
 * Backend falso para las pruebas del cliente. Imita la sesión de I0-05: un token de acceso vigente
 * y un token de renovación que sirve una sola vez.
 */
class FakeServer(
    /** Token de acceso que el servidor acepta. */
    var validAccessToken: String? = null,
    /** Token de renovación vigente y el par que entrega al usarlo. */
    var refreshable: Pair<String, TokenPair>? = null,
) {
    val requests = CopyOnWriteArrayList<HttpRequestData>()

    /** Respuestas propias por ruta; las demás rutas exigen el token de acceso y responden `[]`. */
    val routes = mutableMapOf<String, suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData>()

    /** Se ejecuta antes de responder una renovación, por ejemplo para demorarla. */
    var beforeRefresh: suspend () -> Unit = {}

    val engine = MockEngine { request ->
        requests += request
        val route = routes[request.url.encodedPath]
        when {
            route != null -> route(request)
            request.url.encodedPath == "/api/v1/auth/refresh" -> {
                beforeRefresh()
                val refreshToken = Json.decodeFromString<RefreshRequest>(request.bodyText()).refreshToken
                val (validRefresh, renewed) = refreshable ?: return@MockEngine unauthorized()
                if (refreshToken != validRefresh) return@MockEngine unauthorized()
                refreshable = null
                validAccessToken = renewed.accessToken
                json(Json.encodeToString(renewed))
            }
            request.headers[HttpHeaders.Authorization] == "Bearer $validAccessToken" -> json("[]")
            else -> unauthorized()
        }
    }

    fun requestsTo(path: String) = requests.filter { it.url.encodedPath == path }

    companion object {
        fun HttpRequestData.bodyText(): String = (body as TextContent).text

        fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

        fun MockRequestHandleScope.error(status: HttpStatusCode, code: ErrorCode, message: String) =
            json(Json.encodeToString(ErrorResponse(code, message)), status)

        fun MockRequestHandleScope.unauthorized() =
            error(HttpStatusCode.Unauthorized, ErrorCode.UNAUTHORIZED, "Token inválido o ausente")
    }
}
