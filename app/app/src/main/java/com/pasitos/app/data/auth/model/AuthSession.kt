package com.pasitos.app.data.auth.model

/** Cuenta con sesión iniciada. Los tokens no salen de `SessionRepository` y el cliente de la API. */
data class AuthSession(
    val userId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    /** Falso en cuentas creadas con Google: ven "Crear una contraseña" (RF-CTA-11, CA4). */
    val hasPassword: Boolean = true,
) {
    val displayName: String get() = "$firstName $lastName".trim()
}

sealed interface SessionState {

    /** Al abrir la app, mientras se lee la sesión guardada. */
    data object Restoring : SessionState

    data class SignedIn(val session: AuthSession) : SessionState

    /** [expired]: la sesión venció o el servidor la revocó, y hay que volver a iniciarla (RF-CTA-06, CA2). */
    data class SignedOut(val expired: Boolean = false) : SessionState
}
