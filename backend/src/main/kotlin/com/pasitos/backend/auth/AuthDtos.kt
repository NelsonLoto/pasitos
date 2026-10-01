package com.pasitos.backend.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val nombre: String,
    val email: String,
    val password: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val userId: String,
    val email: String,
    val nombre: String,
    val token: String,
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
)
