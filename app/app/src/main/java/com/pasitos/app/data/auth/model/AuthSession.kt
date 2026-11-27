package com.pasitos.app.data.auth.model

data class AuthSession(
    val userId: String,
    val email: String,
    val displayName: String,
) {
    override fun toString() = "AuthSession(userId=$userId)"
}

class InvalidCredentialsException(message: String) : Exception(message)
class EmailAlreadyInUseException(message: String) : Exception(message)
