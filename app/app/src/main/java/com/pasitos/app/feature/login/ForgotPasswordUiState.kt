package com.pasitos.app.feature.login

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isLoading: Boolean = false,
    /** RF-CTA-05, CA1: mismo mensaje exista o no la cuenta, para no revelar qué emails están registrados. */
    val requestSent: Boolean = false,
    /** Solo por un problema real (ej. sin conexión): RF-CTA-05 no oculta eso, solo si la cuenta existe. */
    val generalError: String? = null,
)
