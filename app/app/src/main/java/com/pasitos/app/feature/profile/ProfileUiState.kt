package com.pasitos.app.feature.profile

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val avatarInitial: String = "",
    /** Cambios sin sincronizar que se perderían al cerrar sesión: si no es nulo, se pide confirmación (RF-CTA-07, CA2). */
    val pendingChangesToConfirm: Int? = null,
    val isLoggingOut: Boolean = false,
    val logoutError: String? = null,
)
