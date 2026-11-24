package com.pasitos.app.data.auth.remote

import com.pasitos.app.core.network.withoutSession
import com.pasitos.shared.api.ErrorResponse
import com.pasitos.shared.api.auth.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Named

class AuthApiException(val status: Int, message: String) : Exception(message)

interface AuthApi {
    suspend fun login(email: String, password: String): AuthResponse
    suspend fun register(request: RegisterRequest): AuthResponse
    suspend fun refresh(token: String): AuthResponse
    suspend fun logout(access: String)
}

class HttpAuthApi @Inject constructor(@Named("public") private val client: HttpClient) : AuthApi {
    override suspend fun login(email: String, password: String) = post("login", LoginRequest(email, password))
    override suspend fun register(request: RegisterRequest) = post("register", request)
    override suspend fun refresh(token: String) = post("refresh", RefreshRequest(token))
    private suspend inline fun <reified T> post(path: String, value: T): AuthResponse {
        val response = client.post("api/v1/auth/$path") { withoutSession(); contentType(ContentType.Application.Json); setBody(value) }
        if (response.status.value !in 200..299) throw failure(response)
        return response.body()
    }
    override suspend fun logout(access: String) {
        val response = client.post("api/v1/auth/logout") { withoutSession(); bearerAuth(access) }
        if (response.status.value !in 200..299) throw failure(response)
    }
    private suspend fun failure(response: io.ktor.client.statement.HttpResponse): AuthApiException {
        val message = try { response.body<ErrorResponse>().message } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { "No pudimos conectar con el servidor. Volvé a intentar." }
        return AuthApiException(response.status.value, message)
    }
}
