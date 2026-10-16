package com.pasitos.backend.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Contrato heredado de /auth/* (sin /api/v1) que usa la app actual, con claves JSON en español.
// Se borra en I0-06, cuando la app pase a /api/v1/auth con los DTOs de `shared`.

@Serializable
data class LegacyRegisterRequest(
    @SerialName("nombre") val name: String,
    val email: String,
    val password: String,
)

@Serializable
data class LegacyLoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class LegacyAuthResponse(
    val userId: String,
    val email: String,
    @SerialName("nombre") val name: String,
    val token: String,
)
