package com.pasitos.app.data.auth

import com.pasitos.app.data.auth.model.AuthSession
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de autenticación real. La interfaz entrega la cuenta y nunca los tokens.
 * Google continúa simulado hasta I1-A2.
 */
interface AuthRepository {
    val currentSession: StateFlow<AuthSession?>

    suspend fun login(email: String, password: String): Result<AuthSession>

    suspend fun loginWithGoogle(): Result<AuthSession>

    suspend fun register(displayName: String, email: String, password: String, acceptTerms: Boolean = false): Result<AuthSession>

    fun logout()
}
