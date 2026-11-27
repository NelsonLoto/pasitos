package com.pasitos.app.data.auth

import com.pasitos.app.data.auth.model.AuthSession
import com.pasitos.app.data.auth.model.SessionState
import com.pasitos.shared.api.auth.RegisterRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RemoteAuthRepository @Inject constructor(
    private val sessions: SessionRepository,
    private val googleMock: FakeAuthRepository,
    @Named("sessionScope") scope: CoroutineScope,
) : AuthRepository {
    override val currentSession = sessions.state.map { state -> (state as? SessionState.SignedIn)?.account?.let {
        AuthSession(it.id, it.email, "${it.firstName} ${it.lastName}".trim())
    } }.stateIn(scope, SharingStarted.Eagerly, null)
    override suspend fun login(email: String, password: String) = safe {
        sessions.login(email, password).let { AuthSession(it.id, it.email, "${it.firstName} ${it.lastName}".trim()) }
    }
    override suspend fun register(displayName: String, email: String, password: String, acceptTerms: Boolean) = safe {
        val parts = displayName.trim().split(Regex("\\s+"), limit = 2)
        sessions.register(RegisterRequest(parts.first(), parts.getOrElse(1) { "" }, email, password, acceptTerms))
            .let { AuthSession(it.id, it.email, "${it.firstName} ${it.lastName}".trim()) }
    }
    override suspend fun loginWithGoogle(): Result<AuthSession> = googleMock.loginWithGoogle().onSuccess { sessions.simulateGoogle(it) }
    override fun logout() = sessions.logout()
    private suspend fun <T> safe(action: suspend () -> T): Result<T> = try { Result.success(action()) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: java.io.IOException) { Result.failure(Exception("No pudimos conectar. Revisá tu conexión y volvé a intentar.")) }
        catch (failure: com.pasitos.app.data.auth.remote.AuthApiException) { Result.failure(Exception(failure.message)) }
        catch (_: Exception) { Result.failure(Exception("No pudimos guardar la sesión. Volvé a intentar.")) }
}
