package com.pasitos.app.data.account

import com.pasitos.app.core.network.toApiException
import com.pasitos.app.core.network.withConnection
import com.pasitos.app.core.network.withoutSession
import com.pasitos.app.data.auth.SessionRepository
import com.pasitos.shared.api.account.ChangePasswordRequest
import com.pasitos.shared.api.account.EmailChangeRequest
import com.pasitos.shared.api.account.PatchMeRequest
import com.pasitos.shared.api.auth.AccountDto
import com.pasitos.shared.api.auth.MeResponse
import com.pasitos.shared.api.auth.PasswordResetRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [AccountRepository] contra `/api/v1/me` (12 · API). */
@Singleton
class AccountApiRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionRepository: SessionRepository,
) : AccountRepository {

    private val _pendingTermsVersion = MutableStateFlow<String?>(null)
    override val pendingTermsVersion: StateFlow<String?> = _pendingTermsVersion.asStateFlow()

    override suspend fun refresh(): Result<Unit> = runCatching {
        val response = withConnection { httpClient.get("/api/v1/me") }
        response.expect(HttpStatusCode.OK)
        val me = response.body<MeResponse>()
        sessionRepository.updateAccount(me.account)
        _pendingTermsVersion.value = me.pendingTermsVersion
    }

    override suspend fun updateName(firstName: String, lastName: String): Result<Unit> = runCatching {
        val response = withConnection {
            httpClient.patch("/api/v1/me") {
                contentType(ContentType.Application.Json)
                setBody(PatchMeRequest(firstName = firstName.trim(), lastName = lastName.trim()))
            }
        }
        response.expect(HttpStatusCode.OK)
        sessionRepository.updateAccount(response.body<AccountDto>())
    }

    override suspend fun requestEmailChange(newEmail: String, password: String): Result<Unit> = runCatching {
        val response = withConnection {
            httpClient.post("/api/v1/me/email-change") {
                contentType(ContentType.Application.Json)
                setBody(EmailChangeRequest(newEmail = newEmail.trim(), password = password))
            }
        }
        response.expect(HttpStatusCode.NoContent)
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = runCatching {
        val response = withConnection {
            httpClient.post("/api/v1/me/password") {
                contentType(ContentType.Application.Json)
                setBody(ChangePasswordRequest(currentPassword = currentPassword, newPassword = newPassword))
            }
        }
        response.expect(HttpStatusCode.NoContent)
    }

    override suspend fun requestPasswordCreation(): Result<Unit> = runCatching {
        val email = checkNotNull(sessionRepository.session.value) { "No hay una sesión iniciada" }.email
        // El mismo pedido que "¿Olvidaste tu contraseña?": el link del email demuestra que la persona
        // controla la cuenta, no alcanza con tener la sesión abierta
        val response = withConnection {
            httpClient.post("/api/v1/auth/password-reset") {
                withoutSession()
                contentType(ContentType.Application.Json)
                setBody(PasswordResetRequest(email))
            }
        }
        response.expect(HttpStatusCode.NoContent)
    }

    override suspend fun acceptTerms(): Result<Unit> = runCatching {
        val response = withConnection { httpClient.post("/api/v1/me/consents") }
        response.expect(HttpStatusCode.NoContent)
        _pendingTermsVersion.value = null
    }

    private suspend fun HttpResponse.expect(status: HttpStatusCode) {
        if (this.status != status) throw toApiException()
    }
}
