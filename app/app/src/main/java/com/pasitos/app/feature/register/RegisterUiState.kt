package com.pasitos.app.feature.register

data class RegisterUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    /** Sin marcar al empezar: la aceptación tiene que ser expresa (RN-CTA-02). */
    val acceptTerms: Boolean = false,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val termsError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
)
