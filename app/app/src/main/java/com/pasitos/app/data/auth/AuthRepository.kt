package com.pasitos.app.data.auth

import com.pasitos.app.data.auth.model.AuthSession
import com.pasitos.app.data.auth.model.SessionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Cuenta y sesión (RF-CTA-01, 03, 04, 06 y 07). Implementación: [AuthApiRepository], contra
 * `/api/v1/auth`.
 */
interface AuthRepository {
    val sessionState: StateFlow<SessionState>

    /** La sesión iniciada, o nula. */
    val currentSession: StateFlow<AuthSession?>

    suspend fun login(email: String, password: String): Result<AuthSession>

    /**
     * RF-CTA-03 con el ID token de Google. En el primer ingreso falla con `terms_required` hasta
     * que se reenvía el mismo token con [acceptTerms] y el nombre y el apellido confirmados.
     */
    suspend fun loginWithGoogle(
        idToken: String,
        acceptTerms: Boolean = false,
        firstName: String? = null,
        lastName: String? = null,
    ): Result<AuthSession>

    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        acceptTerms: Boolean,
    ): Result<AuthSession>

    /** RF-CTA-02: reenvía el email de verificación. Responde bien exista o no la cuenta. */
    suspend fun resendEmailVerification(email: String): Result<Unit>

    /** RF-CTA-05, CA1: responde lo mismo exista o no la cuenta, para no revelar qué emails están registrados. */
    suspend fun requestPasswordReset(email: String): Result<Unit>

    /** RF-CTA-02, CA2: si la cuenta con sesión iniciada ya verificó su email. */
    suspend fun isEmailVerified(): Result<Boolean>

    /** Cambios guardados en el teléfono que el servidor todavía no recibió. Se pierden al cerrar sesión. */
    suspend fun pendingChanges(): Int

    /**
     * Cierra la sesión (RF-CTA-07): la revoca en el servidor, borra los tokens y la base local, y
     * reinicia la app. Sin conexión cierra igual y la revocación queda pendiente hasta que haya red.
     */
    suspend fun logout(): Result<Unit>
}
