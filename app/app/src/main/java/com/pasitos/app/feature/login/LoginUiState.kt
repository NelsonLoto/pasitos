package com.pasitos.app.feature.login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    /** RF-CTA-02, CA1: credenciales correctas, pero hay que verificar el email antes de seguir. */
    val emailVerificationPending: Boolean = false,
    val isResendingVerification: Boolean = false,
    val verificationMessage: String? = null,
)
