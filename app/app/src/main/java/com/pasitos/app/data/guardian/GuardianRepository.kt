package com.pasitos.app.data.guardian

import com.pasitos.app.data.child.model.GuardianRole
import java.time.Instant

/** Un tutor del niño, como lo ven todos sus tutores (`GET /children/{id}/guardians`). */
data class Guardian(
    val accountId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: GuardianRole,
) {
    val fullName: String get() = "$firstName $lastName"
}

/** Una invitación de tutor pendiente que mandó el principal (`GET /children/{id}/guardian-invitations`). */
data class SentInvitation(
    val id: String,
    val email: String,
    val sentAt: Instant,
    val expiresAt: Instant,
    /** Lo calcula el servidor (RN-HIJ-07), para no depender del reloj del teléfono. */
    val expired: Boolean,
)

/**
 * Tutores de un niño (RF-HIJ-05, 07, 08). Todo va por REST y solo funciona en línea (12 · API): sin
 * red devuelve `NoConnectionException`; si el servidor rechaza el pedido, `ApiException` con su
 * mensaje. Dejar de ser tutor está en `ChildRepository.leaveChild`, porque borra al niño del teléfono.
 */
interface GuardianRepository {

    /** Con el principal primero (RN-HIJ-06). */
    suspend fun guardians(childId: String): Result<List<Guardian>>

    /** RF-HIJ-05: solo el principal. El servidor avisa si ya es tutor (CA4) o si ya hay una invitación vigente. */
    suspend fun invite(childId: String, email: String): Result<Unit>

    /** RF-HIJ-05, CA3: las invitaciones pendientes del niño, vigentes y vencidas. Solo para el principal. */
    suspend fun sentInvitations(childId: String): Result<List<SentInvitation>>

    /** RF-HIJ-05, CA3; RN-HIJ-07: reemplaza la invitación por una nueva, con un plazo nuevo de 15 días. */
    suspend fun resend(invitationId: String): Result<Unit>

    /** RF-HIJ-07, CA1: el principal quita a un tutor secundario, que recibe un aviso. */
    suspend fun remove(childId: String, accountId: String): Result<Unit>

    /** RF-HIJ-08: el principal le cede su rol a un secundario y pasa a ser secundario. */
    suspend fun transferPrincipal(childId: String, accountId: String): Result<Unit>
}
