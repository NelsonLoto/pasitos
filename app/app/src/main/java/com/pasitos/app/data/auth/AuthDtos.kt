package com.pasitos.app.data.auth

import kotlinx.serialization.Serializable

@Serializable
internal data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
internal data class RegisterRequestDto(
    val nombre: String,
    val email: String,
    val password: String,
)

@Serializable
internal data class AuthResponseDto(
    val userId: String,
    val email: String,
    val nombre: String,
    val token: String,
)
