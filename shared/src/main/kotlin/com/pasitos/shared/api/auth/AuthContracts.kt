package com.pasitos.shared.api.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val acceptTerms: Boolean,
    val termsVersion: String = "1",
    val deviceLabel: String? = null,
) {
    override fun toString() = "RegisterRequest([redacted])"
}

@Serializable
data class LoginRequest(val email: String, val password: String, val deviceLabel: String? = null) {
    override fun toString() = "LoginRequest([redacted])"
}

@Serializable
data class RefreshRequest(val refreshToken: String) {
    override fun toString() = "RefreshRequest([redacted])"
}

@Serializable
enum class AccountMode { GUARDIAN, PEDIATRICIAN }

@Serializable
data class AccountResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val emailVerified: Boolean,
    val hasPassword: Boolean,
    val isGuardian: Boolean,
    val lastMode: AccountMode?,
    val termsVersion: String,
    val termsPending: Boolean,
) {
    override fun toString() = "AccountResponse(id=$id)"
}

@Serializable
data class AuthResponse(
    val account: AccountResponse,
    val accessToken: String,
    val refreshToken: String,
    /** Momento ISO-8601 en UTC. La sesión de renovación vence tras 30 días sin uso. */
    val accessExpiresAt: String,
) {
    override fun toString() = "AuthResponse(accountId=${account.id}, tokens=[redacted])"
}
