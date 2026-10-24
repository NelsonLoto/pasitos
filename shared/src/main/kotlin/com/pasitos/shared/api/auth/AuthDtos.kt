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
    val accountType: RegistrationAccountType = RegistrationAccountType.FAMILY,
    val dni: String? = null,
    val licenseNumber: String? = null,
    val specialty: String? = null,
)

@Serializable
enum class RegistrationAccountType { FAMILY, PROFESSIONAL }

/** `POST /auth/login` (RF-CTA-04). */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

/**
 * `POST /auth/google` (RF-CTA-03). Si la cuenta de Google ya ingresó antes, o su email coincide con
 * una cuenta existente (RN-CTA-04), solo se usa [idToken]. Si es el primer ingreso, el backend
 * responde 409 `terms_required` hasta que llegue [acceptTerms] en `true`, con el nombre y el
 * apellido que la persona confirmó: recién ahí crea la cuenta.
 */
@Serializable
data class GoogleAuthRequest(
    /** ID token que entrega Credential Manager. El email se toma de él, nunca del pedido. */
    val idToken: String,
    /** Aceptación expresa de la versión vigente de los términos (RN-CTA-02). */
    val acceptTerms: Boolean = false,
    /** Precargado en la app con el de Google; la persona puede editarlo. Solo para el primer ingreso. */
    val firstName: String? = null,
    val lastName: String? = null,
)

/** `POST /auth/refresh` (RF-CTA-06). */
@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

/** `POST /auth/email-verification` (RF-CTA-02). Responde 204 exista o no la cuenta. */
@Serializable
data class EmailVerificationRequest(
    val email: String,
)

/** `POST /auth/password-reset` (RF-CTA-05, CA1). Responde 204 exista o no la cuenta. */
@Serializable
data class PasswordResetRequest(
    val email: String,
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
    /**
     * Falso en las cuentas creadas con Google y en las que perdieron la contraseña por RN-CTA-04:
     * ven "Crear una contraseña" en lugar de "Cambiar contraseña" (RF-CTA-11, CA4). Verdadero por
     * defecto, para leer las sesiones guardadas antes de que existiera.
     */
    val hasPassword: Boolean = true,
)

/** `GET /me` (RF-CTA-09). */
@Serializable
data class MeResponse(
    val account: AccountDto,
    /** Versión de los términos que el usuario todavía no aceptó, o nulo si está al día (RN-CTA-02). */
    val pendingTermsVersion: String?,
)
