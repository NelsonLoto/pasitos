package com.pasitos.app.data.vaccination.model

import com.pasitos.shared.vaccination.DoseStatus
import com.pasitos.shared.vaccination.VaccinationStatus

/** El resumen arriba del calendario y en Inicio (RF-VAC-01, CA2): "2 atrasadas · 1 próxima". */
data class VaccinationSummary(val overdue: Int, val due: Int, val upcoming: Int) {

    val isUpToDate: Boolean get() = overdue == 0 && due == 0 && upcoming == 0

    val text: String
        get() = if (isUpToDate) {
            "Vacunas al día"
        } else {
            listOfNotNull(
                count(overdue, "atrasada", "atrasadas"),
                count(due, "pendiente", "pendientes"),
                count(upcoming, "próxima", "próximas"),
            ).joinToString(" · ")
        }

    private fun count(value: Int, one: String, many: String): String? =
        when (value) {
            0 -> null
            1 -> "1 $one"
            else -> "$value $many"
        }
}

fun VaccinationStatus.summary() = VaccinationSummary(
    overdue = doses.count { it.status == DoseStatus.OVERDUE },
    due = doses.count { it.status == DoseStatus.DUE },
    upcoming = doses.count { it.status == DoseStatus.UPCOMING },
)
