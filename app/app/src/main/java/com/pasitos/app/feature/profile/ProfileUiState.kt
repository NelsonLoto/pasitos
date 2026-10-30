package com.pasitos.app.feature.profile

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val avatarInitial: String = "",
    /** Falso en cuentas creadas con Google: la fila dice "Crear una contraseña" (RF-CTA-11, CA4). */
    val hasPassword: Boolean = true,
    /** RNF-OFF-04: "Última sincronización: hoy a las 14:32 · 2 cambios sin sincronizar". */
    val lastSyncText: String = "",
    /** Cambios sin sincronizar que se perderían al cerrar sesión: si no es nulo, se pide confirmación (RF-CTA-07, CA2). */
    val pendingChangesToConfirm: Int? = null,
    val isLoggingOut: Boolean = false,
    val logoutError: String? = null,
)
