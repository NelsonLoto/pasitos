package com.pasitos.app.feature.home

import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.invitation.ReceivedInvitation
import com.pasitos.app.data.measurement.model.Measurement
import com.pasitos.app.data.vaccination.model.VaccinationSummary

data class HomeUiState(
    val greetingName: String = "",
    val activeChild: Child? = null,
    val lastMeasurement: Measurement? = null,
    /** Nulo sin hijo activo o sin calendario cargado. */
    val vaccinationSummary: VaccinationSummary? = null,
    /** Invitaciones de tutor vigentes (RF-HIJ-06): la tarjeta se muestra mientras haya alguna. */
    val invitations: List<ReceivedInvitation> = emptyList(),
) {
    /** "Te invitaron a ser tutor de Tomi" con una; con más, la cantidad. */
    val invitationsText: String?
        get() = when (invitations.size) {
            0 -> null
            1 -> "Te invitaron a ser tutor de ${invitations.single().childFirstName}"
            else -> "Tenés ${invitations.size} invitaciones para ser tutor"
        }
}
