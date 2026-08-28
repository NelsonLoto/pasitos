package com.ninosano.app.data.auth

import com.ninosano.app.data.auth.model.AuthSession
import com.ninosano.app.data.auth.model.InvalidCredentialsException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Credenciales de prueba: demo@ninosano.app / 123456 (ver app/README.md).
 * Simula latencia de red con [delay] para que el estado de loading de la UI sea real.
 */
@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {

    private val _currentSession = MutableStateFlow<AuthSession?>(null)
    override val currentSession: StateFlow<AuthSession?> = _currentSession.asStateFlow()

    override suspend fun login(email: String, password: String): Result<AuthSession> {
        delay(800)
        return if (email.trim().equals("demo@ninosano.app", ignoreCase = true) && password == "123456") {
            val session = AuthSession(
                userId = "mock-1",
                email = email.trim(),
                displayName = "Familia Demo",
                token = "fake-jwt-token",
            )
            _currentSession.value = session
            Result.success(session)
        } else {
            Result.failure(InvalidCredentialsException("Email o contraseña incorrectos"))
        }
    }

    override suspend fun loginWithGoogle(): Result<AuthSession> {
        delay(600)
        val session = AuthSession(
            userId = "mock-google-1",
            email = "demo.google@ninosano.app",
            displayName = "Familia Demo (Google)",
            token = "fake-jwt-token",
        )
        _currentSession.value = session
        return Result.success(session)
    }

    override fun logout() {
        _currentSession.value = null
    }
}
