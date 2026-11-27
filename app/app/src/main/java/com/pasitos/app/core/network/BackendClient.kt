package com.pasitos.app.core.network

import com.pasitos.app.BuildConfig
import com.pasitos.app.data.auth.SessionRepository
import com.pasitos.app.data.auth.remote.AuthApi
import com.pasitos.app.data.auth.remote.HttpAuthApi
import com.pasitos.app.core.security.SessionStorage
import com.pasitos.app.core.security.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

fun HttpRequestBuilder.withoutSession() { attributes.put(AuthCircuitBreaker, Unit) }

fun HttpClientConfig<*>.backendConfiguration(baseUrl: String, sessions: SessionRepository? = null) {
    val origin = Url(baseUrl)
    expectSuccess = false
    followRedirects = false
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 10_000 }
    defaultRequest { url(baseUrl); accept(ContentType.Application.Json) }
    if (sessions != null) install(Auth) {
        reAuthorizeOnResponse { response -> response.status == HttpStatusCode.Unauthorized &&
            response.call.request.url.let { it.protocol == origin.protocol && it.host == origin.host && it.port == origin.port } }
        bearer {
            cacheTokens = false
            loadTokens { sessions.tokens()?.let { BearerTokens(it.first, it.second) } }
            refreshTokens {
                if (response.call.request.url.let { it.protocol != origin.protocol || it.host != origin.host || it.port != origin.port }) null
                else sessions.refresh(response.call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer "))?.let { BearerTokens(it.first, it.second) }
            }
            sendWithoutRequest { request -> !request.attributes.contains(AuthCircuitBreaker) && request.url.let { it.protocol == origin.protocol && it.host == origin.host && it.port == origin.port } }
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton @Named("sessionScope") fun sessionScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Provides @Singleton @Named("public") fun publicClient(): HttpClient = HttpClient(OkHttp) { backendConfiguration(BuildConfig.API_BASE_URL) }
    @Provides @Singleton fun client(sessions: SessionRepository): HttpClient = HttpClient(OkHttp) { backendConfiguration(BuildConfig.API_BASE_URL, sessions) }
    @Provides @Singleton fun api(implementation: HttpAuthApi): AuthApi = implementation
    @Provides @Singleton fun store(implementation: SessionStore): SessionStorage = implementation
}
