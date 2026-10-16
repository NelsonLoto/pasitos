package com.pasitos.shared.api.auth

import com.pasitos.shared.api.InstantSerializer
import java.time.Instant
import kotlinx.serialization.Serializable

// Contratos de /api/v1/auth y /api/v1/me (12 · API). La app y el backend usan estas mismas clases.

/** `POST /auth/register` (RF-CTA-01). La cuenta se crea con perfil de tutor. */
@Serializable
data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    /** Aceptación expresa de la versión vigente de los términos (RN-CTA-02). */
    val acceptTerms: Boolean,
)

/** `POST /auth/login` (RF-CTA-04). */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

/** `POST /auth/refresh` (RF-CTA-06). */
@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

/**
 * Tokens de una sesión (ADR-11). El de acceso dura 15 minutos y va en `Authorization: Bearer`. El
 * de renovación sirve una sola vez: cada `POST /auth/refresh` devuelve un par nuevo.
 */
@Serializable
data class TokenPair(
    val accessToken: String,
    @Serializable(with = InstantSerializer::class)
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
)

/** Respuesta de registro e inicio de sesión. */
@Serializable
data class AuthResponse(
    val account: AccountDto,
    val tokens: TokenPair,
)

/** Modo de uso de la app en una cuenta con perfil de tutor, de pediatra o ambos (RF-CTA-09). */
@Serializable
enum class AccountMode { GUARDIAN, PEDIATRICIAN }

@Serializable
data class AccountDto(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val emailVerified: Boolean,
    val isGuardian: Boolean,
    /** Último modo usado, para abrir la app en él (RF-CTA-04, CA1). Nulo si nunca eligió. */
    val lastMode: AccountMode?,
)

/** `GET /me` (RF-CTA-09). */
@Serializable
data class MeResponse(
    val account: AccountDto,
    /** Versión de los términos que el usuario todavía no aceptó, o nulo si está al día (RN-CTA-02). */
    val pendingTermsVersion: String?,
)
