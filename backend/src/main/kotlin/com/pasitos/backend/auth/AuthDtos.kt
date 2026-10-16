package com.pasitos.backend.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Contrato heredado que usa la app actual, con claves JSON en español. Los endpoints nuevos bajo
// /api/v1 (I0-05) usan los DTOs de `shared`, en inglés (ADR-21).

@Serializable
data class RegisterRequest(
    @SerialName("nombre") val name: String,
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
    @SerialName("nombre") val name: String,
    val token: String,
)
