package com.pasitos.app.data.auth

import com.pasitos.app.data.auth.model.AuthSession
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de autenticación (RF01). Implementación real: [AuthApiRepository], contra el
 * backend Ktor (`POST /auth/login`, `POST /auth/register`). `loginWithGoogle()` sigue
 * mockeado — Google Sign-In real es Etapa 4.
 */
interface AuthRepository {
    val currentSession: StateFlow<AuthSession?>

    suspend fun login(email: String, password: String): Result<AuthSession>

    suspend fun loginWithGoogle(): Result<AuthSession>

    suspend fun register(displayName: String, email: String, password: String): Result<AuthSession>

    fun logout()
}
