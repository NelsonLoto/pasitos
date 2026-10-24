package com.pasitos.app.feature.invitations

import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.app.data.invitation.ReceivedInvitation
import java.time.format.DateTimeFormatter

/** Invitaciones de tutor recibidas (RF-HIJ-06). Todo es solo en línea. */
data class InvitationsUiState(
    val invitations: List<ReceivedInvitation> = emptyList(),
    val isLoading: Boolean = true,
    /** No se pudo traer la lista: se muestra con "Reintentar". */
    val loadError: String? = null,
    val isOnline: Boolean = true,
    /**
     * Recién verificó el email (RF-HIJ-05, CA2): sin "atrás", y con "Ahora no" para seguir al alta
     * de hijo, como sin invitaciones.
     */
    val afterRegistration: Boolean = false,
    /** La invitación que está aceptando: el diálogo pide la declaración de representación (CA1). */
    val accepting: ReceivedInvitation? = null,
    val representationDeclared: Boolean = false,
    val rejecting: ReceivedInvitation? = null,
    val dialogError: String? = null,
    val isWorking: Boolean = false,
    /** Resultado de la última respuesta, por ejemplo "Ya sos tutor de …". */
    val message: String? = null,
    /** Aceptó al menos una: al seguir va a Inicio, donde llega el niño. */
    val acceptedAny: Boolean = false,
) {
    val canAct: Boolean get() = isOnline && !isWorking

    val canConfirmAccept: Boolean get() = canAct && representationDeclared
}

private val ExpiryFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** "Ana Pérez te invitó a ser tutor de Tomi García". */
val ReceivedInvitation.description: String
    get() = "$inviterFirstName $inviterLastName te invitó a ser tutor de $childFirstName $childLastName"

/** El día en hora argentina, como el resto de las fechas de la app (ADR-26). */
val ReceivedInvitation.expiryText: String
    get() = "Vence el ${expiresAt.atZone(ArgentinaClock.ZONE).toLocalDate().format(ExpiryFormatter)}"
