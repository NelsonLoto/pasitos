package com.pasitos.shared.api.account

import kotlinx.serialization.Serializable

// Contratos de /api/v1/me más allá del login (RF-CTA-09, RF-CTA-11, RN-CTA-02; 12 · API).

/** `PATCH /me`. */
@Serializable
data class PatchMeRequest(
    val firstName: String,
    val lastName: String,
)

/** `POST /me/password` (RF-CTA-11, CA2). Pide la contraseña actual; revoca las demás sesiones. */
@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

/** `POST /me/email-change` (RF-CTA-11, CA1). El email nuevo se confirma por `/web/confirm-email-change`. */
@Serializable
data class EmailChangeRequest(
    val newEmail: String,
    val password: String,
)
