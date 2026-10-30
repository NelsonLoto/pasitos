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
    /** RF-CTA-03, CA1: primer ingreso con Google, antes de crear la cuenta. */
    val googleSignUp: GoogleSignUpUiState? = null,
)

/**
 * "Completá tu cuenta": nombre y apellido precargados con los de Google y la casilla de términos
 * sin marcar (RN-CTA-02). El ID token no está acá: lo guarda el ViewModel, solo en memoria.
 */
data class GoogleSignUpUiState(
    val email: String?,
    val firstName: String,
    val lastName: String,
    val acceptTerms: Boolean = false,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val termsError: String? = null,
)
