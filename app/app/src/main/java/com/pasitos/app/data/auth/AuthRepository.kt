package com.pasitos.app.data.auth

import com.pasitos.app.data.auth.model.AuthSession
import com.pasitos.app.data.auth.model.SessionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Cuenta y sesión (RF-CTA-01, 04, 06 y 07). Implementación: [AuthApiRepository], contra
 * `/api/v1/auth`. `loginWithGoogle()` todavía no está disponible: llega con I1-A2.
 */
interface AuthRepository {
    val sessionState: StateFlow<SessionState>

    /** La sesión iniciada, o nula. */
    val currentSession: StateFlow<AuthSession?>

    suspend fun login(email: String, password: String): Result<AuthSession>

    suspend fun loginWithGoogle(): Result<AuthSession>

    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        acceptTerms: Boolean,
    ): Result<AuthSession>

    /** Cambios guardados en el teléfono que el servidor todavía no recibió. Se pierden al cerrar sesión. */
    suspend fun pendingChanges(): Int

    /**
     * Cierra la sesión (RF-CTA-07): la revoca en el servidor, borra los tokens y la base local, y
     * reinicia la app. Necesita conexión; si falla, la sesión sigue abierta y devuelve el error.
     */
    suspend fun logout(): Result<Unit>
}
