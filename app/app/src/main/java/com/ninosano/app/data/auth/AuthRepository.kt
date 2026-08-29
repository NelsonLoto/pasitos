package com.ninosano.app.data.auth

import com.ninosano.app.data.auth.model.AuthSession
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de autenticación (RF01). La implementación actual (ver [FakeAuthRepository])
 * es mock/local: el backend todavía no existe. El día que exista, se agrega
 * RemoteAuthRepository y solo cambia el binding en AuthModule — ni los
 * ViewModels ni las pantallas que dependen de esta interfaz se tocan.
 */
interface AuthRepository {
    val currentSession: StateFlow<AuthSession?>

    suspend fun login(email: String, password: String): Result<AuthSession>

    suspend fun loginWithGoogle(): Result<AuthSession>

    fun logout()
}
