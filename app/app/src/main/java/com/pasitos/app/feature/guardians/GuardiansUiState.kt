package com.pasitos.app.feature.guardians

import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.app.data.child.model.GuardianRole
import com.pasitos.app.data.guardian.Guardian
import com.pasitos.app.data.guardian.SentInvitation
import java.time.format.DateTimeFormatter

/** Tutores de un niño (RF-HIJ-05, 07, 08). Todo es solo en línea. */
data class GuardiansUiState(
    /** Nombre de pila del niño, para los textos. Vacío si ya no está en el teléfono. */
    val childFirstName: String = "",
    val guardians: List<Guardian> = emptyList(),
    /** Las invitaciones pendientes que mandó el principal (RF-HIJ-05, CA3). Vacía para un secundario. */
    val sentInvitations: List<SentInvitation> = emptyList(),
    val myAccountId: String? = null,
    val isLoading: Boolean = true,
    /** No se pudo traer la lista: se muestra con "Reintentar". */
    val loadError: String? = null,
    val isOnline: Boolean = true,
    val dialog: GuardiansDialog = GuardiansDialog.None,
    val inviteEmail: String = "",
    /** Error dentro del diálogo abierto. */
    val dialogError: String? = null,
    val isWorking: Boolean = false,
    /** Resultado de la última acción, por ejemplo "Le mandamos la invitación a …". */
    val message: String? = null,
) {
    /** Solo el principal invita, quita y cede su rol (01 · Actores y permisos). */
    val isPrincipal: Boolean
        get() = guardians.any { it.accountId == myAccountId && it.role == GuardianRole.PRINCIPAL }

    val canAct: Boolean get() = isOnline && !isWorking && !isLoading && loadError == null

    fun isMe(guardian: Guardian): Boolean = guardian.accountId == myAccountId
}

private val ExpiryFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** "Vence el 08/11/2026" o "Venció el 08/11/2026", con el día en hora argentina (ADR-26). */
val SentInvitation.expiryText: String
    get() {
        val date = expiresAt.atZone(ArgentinaClock.ZONE).toLocalDate().format(ExpiryFormatter)
        return if (expired) "Venció el $date" else "Vence el $date"
    }

/** El diálogo abierto, si hay uno. Cada acción se confirma antes de enviarse. */
sealed interface GuardiansDialog {
    data object None : GuardiansDialog

    /** RF-HIJ-05: el email de la persona invitada. */
    data object Invite : GuardiansDialog

    /** RF-HIJ-07, CA1. */
    data class Remove(val guardian: Guardian) : GuardiansDialog

    /** RF-HIJ-08. */
    data class Transfer(val guardian: Guardian) : GuardiansDialog

    /** RF-HIJ-07, CA2: el secundario deja de ser tutor. */
    data object Leave : GuardiansDialog
}
