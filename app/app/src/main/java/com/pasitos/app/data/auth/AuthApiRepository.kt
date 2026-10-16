package com.pasitos.app.data.auth

import com.pasitos.app.data.auth.model.AuthSession
import com.pasitos.app.data.auth.model.EmailAlreadyInUseException
import com.pasitos.app.data.auth.model.InvalidCredentialsException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class AuthApiRepository @Inject constructor(
    private val httpClient: HttpClient,
) : AuthRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentSession = MutableStateFlow<AuthSession?>(null)
    override val currentSession: StateFlow<AuthSession?> = _currentSession.asStateFlow()

    override suspend fun login(email: String, password: String): Result<AuthSession> =
        runCatching {
            val response = httpClient.post("/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequestDto(email = email.trim(), password = password))
            }
            when (response.status) {
                HttpStatusCode.OK -> response.body<AuthResponseDto>().toSession()
                HttpStatusCode.Unauthorized -> throw InvalidCredentialsException(response.body<ErrorResponseDto>().message)
                else -> throw Exception("No se pudo conectar con el servidor")
            }
        }.onSuccess { _currentSession.value = it }

    override suspend fun loginWithGoogle(): Result<AuthSession> {
        // Google Sign-In real llega con I1-A2 (ver docs/plan/I1-base-de-tutores.md) — se
        // mantiene tal cual el comportamiento mockeado que ya tenía FakeAuthRepository.
        val session = AuthSession(
            userId = "mock-google-1",
            email = "demo.google@example.com",
            displayName = "Familia Demo (Google)",
            token = "fake-jwt-token",
        )
        _currentSession.value = session
        return Result.success(session)
    }

    override suspend fun register(displayName: String, email: String, password: String): Result<AuthSession> =
        runCatching {
            val response = httpClient.post("/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequestDto(nombre = displayName.trim(), email = email.trim(), password = password))
            }
            when (response.status) {
                HttpStatusCode.Created -> response.body<AuthResponseDto>().toSession()
                HttpStatusCode.Conflict -> throw EmailAlreadyInUseException(response.body<ErrorResponseDto>().message)
                else -> throw Exception("No se pudo conectar con el servidor")
            }
        }.onSuccess { _currentSession.value = it }

    override fun logout() {
        val token = _currentSession.value?.token
        _currentSession.value = null
        // Revoca la sesión en el servidor (borra la fila de `sesiones`). Best-effort y en
        // segundo plano: la sesión local ya se limpió, y si no hay red igual queda cerrada
        // acá — el token vence solo en el servidor.
        if (token != null) {
            repositoryScope.launch {
                runCatching { httpClient.post("/auth/logout") { bearerAuth(token) } }
            }
        }
    }

    private fun AuthResponseDto.toSession() = AuthSession(
        userId = userId,
        email = email,
        displayName = nombre,
        token = token,
    )
}
