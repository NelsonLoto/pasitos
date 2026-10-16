package com.pasitos.app.core.network

import com.pasitos.shared.api.auth.RefreshRequest
import com.pasitos.shared.api.auth.TokenPair
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * Módulo `@Provides` (no `@Binds` como el resto de los módulos de dominio) porque
 * [HttpClient] no tiene una interfaz propia que bindear — es intencional, no un error
 * de copiar y pegar.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(sessionTokens: SessionTokens): HttpClient =
        createHttpClient(OkHttp.create(), NetworkConfig.BASE_URL, sessionTokens)
}

/**
 * Cliente de la API (15 · App Android §4). Agrega el token de acceso a cada pedido. Si el servidor
 * responde 401, renueva los tokens y repite el pedido sin que el usuario lo note; si varios
 * pedidos fallan a la vez, renueva una sola vez.
 */
fun createHttpClient(engine: HttpClientEngine, baseUrl: String, sessionTokens: SessionTokens): HttpClient =
    HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Auth) {
            bearer {
                // Los tokens viven en SessionTokens: iniciar o cerrar sesión no deja uno viejo en caché
                cacheTokens = false
                // Si se cancela el pedido que disparó la renovación (por ejemplo, porque el usuario
                // salió de la pantalla), igual se guardan los tokens nuevos: el servidor ya
                // invalidó los anteriores
                nonCancellableRefresh = true
                loadTokens { sessionTokens.current()?.toBearerTokens() }
                refreshTokens {
                    val rejected = response.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
                    sessionTokens.refresh(rejected) { refreshToken ->
                        val response = client.post("/api/v1/auth/refresh") {
                            markAsRefreshTokenRequest()
                            contentType(ContentType.Application.Json)
                            setBody(RefreshRequest(refreshToken))
                        }
                        when (response.status) {
                            HttpStatusCode.OK -> RefreshResult.Renewed(response.body())
                            HttpStatusCode.Unauthorized -> RefreshResult.Rejected
                            else -> RefreshResult.Unavailable
                        }
                    }?.toBearerTokens()
                }
            }
        }
        defaultRequest {
            url(baseUrl)
        }
    }

private fun TokenPair.toBearerTokens() = BearerTokens(accessToken, refreshToken)
