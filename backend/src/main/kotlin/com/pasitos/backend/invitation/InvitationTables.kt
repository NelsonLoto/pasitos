package com.pasitos.backend.invitation

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

enum class InvitationType { GUARDIAN, PEDIATRICIAN, CLAIM }

enum class InvitationStatus { PENDING, ACCEPTED, REJECTED, EXPIRED, CANCELLED }

/**
 * Invitaciones a tutores, a pediatras no registrados y a reclamar un perfil (RN-HIJ-07). I1-B5
 * solo usa `GUARDIAN`; `PEDIATRICIAN` y `CLAIM` son de I3. El vencimiento se calcula al leer
 * (`expires_at < now()`), no con una tarea programada: esa llega en I6.
 */
object Invitations : Table("invitations") {
    val id = uuid("id")
    val type = enumerationByName<InvitationType>("type", 32)
    val childId = uuid("child_id")
    val inviterAccountId = uuid("inviter_account_id")
    val email = text("email").nullable()
    val firstName = text("first_name").nullable()
    val lastName = text("last_name").nullable()
    val status = enumerationByName<InvitationStatus>("status", 16).default(InvitationStatus.PENDING)
    val sentAt = timestampWithTimeZone("sent_at").databaseGenerated()
    val expiresAt = timestampWithTimeZone("expires_at")
    val respondedAt = timestampWithTimeZone("responded_at").nullable()
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
