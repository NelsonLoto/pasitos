package com.pasitos.app.data.auth

import com.pasitos.app.data.auth.model.AuthSession
import com.pasitos.app.data.auth.model.EmailAlreadyInUseException
import com.pasitos.app.data.auth.model.InvalidCredentialsException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Credenciales de prueba: demo@pasitos.app / 123456 (ver app/README.md).
 * Simula latencia de red con [delay] para que el estado de loading de la UI sea real.
 */
@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {

    private val _currentSession = MutableStateFlow<AuthSession?>(null)
    override val currentSession: StateFlow<AuthSession?> = _currentSession.asStateFlow()

    private val registeredEmails = mutableSetOf("demo@pasitos.app")

    override suspend fun login(email: String, password: String): Result<AuthSession> {
        delay(800)
        return if (email.trim().equals("demo@pasitos.app", ignoreCase = true) && password == "123456") {
            val session = AuthSession(
                userId = "mock-1",
                email = email.trim(),
                displayName = "Familia Demo",
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
            email = "demo.google@pasitos.app",
            displayName = "Familia Demo (Google)",
        )
        _currentSession.value = session
        return Result.success(session)
    }

    override suspend fun register(displayName: String, email: String, password: String, acceptTerms: Boolean): Result<AuthSession> {
        delay(800)
        val normalizedEmail = email.trim()
        return if (registeredEmails.any { it.equals(normalizedEmail, ignoreCase = true) }) {
            Result.failure(EmailAlreadyInUseException("Ya existe una cuenta con este email"))
        } else {
            registeredEmails += normalizedEmail
            val session = AuthSession(
                userId = "mock-${registeredEmails.size}",
                email = normalizedEmail,
                displayName = displayName.trim(),
            )
            _currentSession.value = session
            Result.success(session)
        }
    }

    override fun logout() {
        _currentSession.value = null
    }
}
