package com.pasitos.app.data.auth

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
import com.pasitos.shared.api.auth.LoginRequest
import com.pasitos.shared.api.auth.RegisterRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton
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
                    )
                )
            }
        }

    override suspend fun pendingChanges(): Int = pendingMutationDao.count()

    override suspend fun logout(): Result<Unit> = runCatching {
        val response = withConnection { httpClient.post("/api/v1/auth/logout") }
        // 401: la sesión ya había vencido o estaba revocada en el servidor
        if (response.status != HttpStatusCode.NoContent && response.status != HttpStatusCode.Unauthorized) {
            throw response.toApiException()
        }
        // Revocada la sesión, la limpieza termina aunque el usuario salga de la pantalla
        withContext(NonCancellable) {
            // Antes de borrar la base, para que ninguna sincronización escriba en ella
            syncScheduler.cancelAll()
            sessionRepository.clear()
            localDatabase.wipe()
            processRestarter.restart()
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
}
