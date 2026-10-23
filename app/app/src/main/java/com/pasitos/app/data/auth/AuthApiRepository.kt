package com.pasitos.app.data.auth

import android.util.Log
import com.pasitos.app.core.database.LocalDatabase
import com.pasitos.app.core.network.toApiException
import com.pasitos.app.core.network.withConnection
import com.pasitos.app.core.network.withoutSession
import com.pasitos.app.core.process.ProcessRestarter
import com.pasitos.app.core.sync.SyncScheduler
import com.pasitos.app.core.sync.local.PendingMutationDao
import com.pasitos.app.data.auth.model.AuthSession
import com.pasitos.app.data.auth.model.SessionState
import com.pasitos.shared.api.auth.AuthResponse
import com.pasitos.shared.api.auth.EmailVerificationRequest
import com.pasitos.shared.api.auth.LoginRequest
import com.pasitos.shared.api.auth.MeResponse
import com.pasitos.shared.api.auth.PasswordResetRequest
import com.pasitos.shared.api.auth.RegisterRequest
import com.pasitos.shared.api.auth.RegistrationAccountType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

@Singleton
class AuthApiRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionRepository: SessionRepository,
    private val localDatabase: LocalDatabase,
    private val pendingMutationDao: PendingMutationDao,
    private val syncScheduler: SyncScheduler,
    private val sessionRevocation: SessionRevocation,
    private val processRestarter: ProcessRestarter,
) : AuthRepository {

    override val sessionState: StateFlow<SessionState> = sessionRepository.state

    override val currentSession: StateFlow<AuthSession?> = sessionRepository.session

    override suspend fun login(email: String, password: String): Result<AuthSession> =
        signIn(HttpStatusCode.OK) {
            httpClient.post("/api/v1/auth/login") {
                withoutSession()
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email = email.trim(), password = password))
            }
        }

    override suspend fun loginWithGoogle(): Result<AuthSession> =
        Result.failure(Exception("Muy pronto vas a poder ingresar con Google. Por ahora, usá tu email."))

    override suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        acceptTerms: Boolean,
        accountType: String,
        dni: String?,
        licenseNumber: String?,
        specialty: String?,
    ): Result<AuthSession> =
        signIn(HttpStatusCode.Created) {
            httpClient.post("/api/v1/auth/register") {
                withoutSession()
                contentType(ContentType.Application.Json)
                setBody(
                    RegisterRequest(
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        email = email.trim(),
                        password = password,
                        acceptTerms = acceptTerms,
                        accountType = RegistrationAccountType.valueOf(accountType),
                        dni = dni?.trim()?.takeIf(String::isNotEmpty),
                        licenseNumber = licenseNumber?.trim()?.takeIf(String::isNotEmpty),
                        specialty = specialty?.trim()?.takeIf(String::isNotEmpty),
                    )
                )
            }
        }

    override suspend fun resendEmailVerification(email: String): Result<Unit> = runCatching {
        val response = withConnection {
            httpClient.post("/api/v1/auth/email-verification") {
                withoutSession()
                contentType(ContentType.Application.Json)
                setBody(EmailVerificationRequest(email.trim()))
            }
        }
        if (response.status != HttpStatusCode.NoContent) throw response.toApiException()
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> = runCatching {
        val response = withConnection {
            httpClient.post("/api/v1/auth/password-reset") {
                withoutSession()
                contentType(ContentType.Application.Json)
                setBody(PasswordResetRequest(email.trim()))
            }
        }
        if (response.status != HttpStatusCode.NoContent) throw response.toApiException()
    }

    override suspend fun isEmailVerified(): Result<Boolean> = runCatching {
        val response = withConnection { httpClient.get("/api/v1/me") }
        if (response.status != HttpStatusCode.OK) throw response.toApiException()
        response.body<MeResponse>().account.emailVerified
    }

    override suspend fun pendingChanges(): Int = pendingMutationDao.count()

    override suspend fun logout(): Result<Unit> = runCatching {
        val revoked = try {
            val response = httpClient.post("/api/v1/auth/logout")
            // 401: la sesión ya había vencido o estaba revocada en el servidor
            response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.Unauthorized
        } catch (_: IOException) {
            false
        }
        // Desde acá la limpieza termina aunque el usuario salga de la pantalla
        withContext(NonCancellable) {
            if (!revoked) scheduleRevocation()
            // Antes de borrar la base, para que ninguna sincronización escriba en ella
            syncScheduler.cancelAll()
            sessionRepository.clear()
            localDatabase.wipe()
            processRestarter.restart()
        }
    }

    /**
     * Sin conexión o con el servidor caído, la sesión se cierra igual en el teléfono y se revoca
     * cuando haya red (RF-CTA-07, CA3). Si ni siquiera se puede guardar el token para eso, se sigue:
     * borrar los datos del teléfono es lo más urgente, y sin el token nadie puede renovar la sesión.
     */
    private suspend fun scheduleRevocation() {
        val refreshToken = sessionRepository.current()?.refreshToken ?: return
        try {
            sessionRevocation.schedule(refreshToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo programar la revocación de la sesión", e)
        }
    }

    private suspend fun signIn(success: HttpStatusCode, request: suspend () -> HttpResponse): Result<AuthSession> =
        runCatching {
            val response = withConnection { request() }
            if (response.status != success) throw response.toApiException()
            val body = response.body<AuthResponse>()
            withContext(NonCancellable) {
                val previousOwner = sessionRepository.start(body.account, body.tokens)
                if (previousOwner != null) {
                    // Los datos locales son de otra cuenta, cuya sesión venció: se borran sin mostrarlos
                    syncScheduler.cancelAll()
                    localDatabase.wipe()
                    processRestarter.restart()
                }
            }
            checkNotNull(sessionRepository.session.value)
        }

    private companion object {
        const val TAG = "AuthApiRepository"
    }
}
