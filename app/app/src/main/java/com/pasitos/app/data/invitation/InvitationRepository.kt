package com.pasitos.app.data.invitation

import java.time.Instant
import kotlinx.coroutines.flow.StateFlow

/** Una invitación de tutor vigente para la cuenta (`GET /me/invitations`, RF-HIJ-06). */
data class ReceivedInvitation(
    val id: String,
    val childId: String,
    val childFirstName: String,
    val childLastName: String,
    val inviterFirstName: String,
    val inviterLastName: String,
    val sentAt: Instant,
    val expiresAt: Instant,
)

/**
 * Invitaciones de tutor recibidas (RF-HIJ-05 CA1 y CA2, RF-HIJ-06). Todo va por REST y solo funciona
 * en línea: sin red devuelve `NoConnectionException`; si el servidor rechaza el pedido,
 * `ApiException` con su mensaje (por ejemplo, la invitación venció, CA3).
 */
interface InvitationRepository {

    /** Las vigentes según la última consulta. Vacía hasta la primera, sin conexión o al cerrar sesión. */
    val pending: StateFlow<List<ReceivedInvitation>>

    /** `GET /me/invitations`. Se llama al abrir la app con sesión, al verificar el email y al abrir la pantalla. */
    suspend fun refresh(): Result<Unit>

    /**
     * RF-HIJ-06, CA1: queda como tutor secundario. La sincronización trae al niño, que llega en
     * `newChildIds`.
     */
    suspend fun accept(invitationId: String, representationDeclared: Boolean): Result<Unit>

    /** RF-HIJ-06, CA2: el principal recibe un aviso y la invitación queda cerrada. */
    suspend fun reject(invitationId: String): Result<Unit>

    /** Al cerrar sesión, para que la cuenta siguiente no vea las invitaciones de la anterior. */
    fun clear()
}
