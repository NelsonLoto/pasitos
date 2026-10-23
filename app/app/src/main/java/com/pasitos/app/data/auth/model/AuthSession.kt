package com.pasitos.app.data.auth.model

import com.pasitos.shared.api.auth.AccountMode

/** Cuenta con sesión iniciada. Los tokens no salen de `SessionRepository` y el cliente de la API. */
data class AuthSession(
    val userId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val isGuardian: Boolean = true,
    val lastMode: AccountMode? = null,
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
