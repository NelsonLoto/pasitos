package com.pasitos.app.feature.emailverification

data class EmailVerificationUiState(
    val email: String,
    val isChecking: Boolean = false,
    val isResending: Boolean = false,
    val message: String? = null,
)
