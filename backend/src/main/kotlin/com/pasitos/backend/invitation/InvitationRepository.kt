package com.pasitos.backend.invitation

import com.pasitos.backend.common.toUtc
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

data class Invitation(
    val id: UUID,
    val type: InvitationType,
    val childId: UUID,
    val inviterAccountId: UUID,
    val email: String?,
    val status: InvitationStatus,
    val sentAt: Instant,
    val expiresAt: Instant,
)

/** Todas las funciones asumen una transacción abierta. */
class InvitationRepository {

    fun insert(type: InvitationType, childId: UUID, inviterAccountId: UUID, email: String, expiresAt: Instant): UUID {
        val id = UUID.randomUUID()
        Invitations.insert {
            it[Invitations.id] = id
            it[Invitations.type] = type
            it[Invitations.childId] = childId
            it[Invitations.inviterAccountId] = inviterAccountId
            it[Invitations.email] = email
            it[Invitations.expiresAt] = expiresAt.toUtc()
        }
        return id
    }

    fun findById(id: UUID): Invitation? =
        Invitations.selectAll().where { Invitations.id eq id }.firstOrNull()?.toInvitation()

    /** La invitación vigente (pendiente y sin vencer) de [type] para [childId] y [email], si hay. */
    fun findPending(childId: UUID, email: String, type: InvitationType, now: Instant): Invitation? =
        Invitations.selectAll()
            .where {
                (Invitations.childId eq childId) and (Invitations.email eq email) and (Invitations.type eq type) and
                    (Invitations.status eq InvitationStatus.PENDING) and (Invitations.expiresAt greater now.toUtc())
            }
            .firstOrNull()
            ?.toInvitation()

    /** Invitaciones de tutor vigentes para el email de quien pregunta (RF-HIJ-06). */
    fun listPendingGuardianInvitations(email: String, now: Instant): List<Invitation> =
        Invitations.selectAll()
            .where {
                (Invitations.email eq email) and (Invitations.type eq InvitationType.GUARDIAN) and
                    (Invitations.status eq InvitationStatus.PENDING) and (Invitations.expiresAt greater now.toUtc())
            }
            .map { it.toInvitation() }

    /** Invitaciones de [type] en `PENDING` de [childId], vigentes y vencidas, de la más nueva a la más vieja (RF-HIJ-05, CA3). */
    fun listPendingForChild(childId: UUID, type: InvitationType): List<Invitation> =
        Invitations.selectAll()
            .where { (Invitations.childId eq childId) and (Invitations.type eq type) and (Invitations.status eq InvitationStatus.PENDING) }
            .orderBy(Invitations.sentAt, SortOrder.DESC)
            .map { it.toInvitation() }

    /** Pasa a `CANCELLED` las invitaciones vencidas de [type] para [childId] y [email]. */
    fun cancelExpired(childId: UUID, email: String, type: InvitationType, now: Instant) {
        Invitations.update({
            (Invitations.childId eq childId) and (Invitations.email eq email) and (Invitations.type eq type) and
                (Invitations.status eq InvitationStatus.PENDING) and (Invitations.expiresAt lessEq now.toUtc())
        }) {
            it[Invitations.status] = InvitationStatus.CANCELLED
            it[respondedAt] = now.toUtc()
        }
    }

    fun markStatus(id: UUID, status: InvitationStatus, at: Instant) {
        Invitations.update({ Invitations.id eq id }) {
            it[Invitations.status] = status
            it[respondedAt] = at.toUtc()
        }
    }

    private fun ResultRow.toInvitation() = Invitation(
        id = this[Invitations.id],
        type = this[Invitations.type],
        childId = this[Invitations.childId],
        inviterAccountId = this[Invitations.inviterAccountId],
        email = this[Invitations.email],
        status = this[Invitations.status],
        sentAt = this[Invitations.sentAt].toInstant(),
        expiresAt = this[Invitations.expiresAt].toInstant(),
    )
}
