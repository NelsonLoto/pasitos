package com.pasitos.app.data.auth.model

data class AuthSession(
    val userId: String,
    val email: String,
    val displayName: String,
    val token: String,
)

class InvalidCredentialsException(message: String) : Exception(message)
class EmailAlreadyInUseException(message: String) : Exception(message)
