package com.pasitos.shared.api.guardianship

import com.pasitos.shared.api.InstantSerializer
import java.time.Instant
import kotlinx.serialization.Serializable

// Contratos de tutores e invitaciones (RF-HIJ-05 a 08; 12 · API).

@Serializable
enum class GuardianshipRole { PRINCIPAL, SECONDARY }

/** `GET /children/{id}/guardians`. */
@Serializable
data class GuardianResponse(
    val accountId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: GuardianshipRole,
)

/** `POST /children/{id}/guardian-invitations` (RF-HIJ-05). */
@Serializable
data class InviteGuardianRequest(
    val email: String,
)

/** `GET /me/invitations` (RF-HIJ-06). */
@Serializable
data class InvitationResponse(
    val id: String,
    val childId: String,
    val childFirstName: String,
    val childLastName: String,
    val inviterFirstName: String,
    val inviterLastName: String,
    @Serializable(with = InstantSerializer::class)
    val sentAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val expiresAt: Instant,
)

/** `POST /invitations/{id}/accept` (RF-HIJ-06, CA1). */
@Serializable
data class AcceptInvitationRequest(
    /** "Soy madre, padre o representante legal de este niño o niña" (RF-HIJ-06, CA1). */
    val representationDeclared: Boolean,
)

/** `POST /children/{id}/principal-transfer` (RF-HIJ-08). */
@Serializable
data class TransferPrincipalRequest(
    val accountId: String,
)
