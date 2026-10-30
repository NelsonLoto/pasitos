package com.pasitos.app.feature.vaccination

import com.pasitos.app.data.vaccination.model.VaccinationSummary
import com.pasitos.shared.vaccination.DoseStatus

/** Filtro del calendario (RF-VAC-01, CA3): un estado a la vez, o todas. */
enum class DoseFilter(val label: String, val status: DoseStatus?) {
    All("Todas", null),
    Overdue("Atrasadas", DoseStatus.OVERDUE),
    Due("Pendientes", DoseStatus.DUE),
    Upcoming("Próximas", DoseStatus.UPCOMING),
    Applied("Aplicadas", DoseStatus.APPLIED),
    Scheduled("Programadas", DoseStatus.SCHEDULED),
}

/** Una dosis del calendario: "Neumococo conjugada", "1ª dosis · desde el 16/12/2025". */
data class DoseRowUi(
    val doseCode: String,
    val vaccineName: String,
    val detail: String,
    val status: DoseStatus,
)

/** Las dosis de una misma edad indicada: "Al nacer", "2 meses"… (RF-VAC-01, CA1). */
data class AgeGroupUi(val label: String, val doses: List<DoseRowUi>)

/** Una dosis aplicada que el calendario vigente ya no tiene (RN-VAC-03), o una otra vacuna. */
data class RecordRowUi(val id: String, val title: String, val detail: String)

data class VaccinationCalendarUiState(
    val childId: String? = null,
    val childName: String = "",
    val isLoaded: Boolean = false,
    /** Falso si el teléfono todavía no tiene el calendario, o el servidor no tiene uno publicado. */
    val hasCalendar: Boolean = false,
    val summary: VaccinationSummary? = null,
    val filter: DoseFilter = DoseFilter.All,
    val groups: List<AgeGroupUi> = emptyList(),
    val formerDoses: List<RecordRowUi> = emptyList(),
    val otherVaccines: List<RecordRowUi> = emptyList(),
    /** Si hay dosis anteriores a hoy sin registrar (RF-VAC-04, CA4). */
    val canMarkPreviousDoses: Boolean = false,
) {
    /** Las otras vacunas no tienen estado: solo se ven sin filtro (RN-VAC-06). */
    val showOtherVaccines: Boolean get() = filter == DoseFilter.All

    /** Las de calendarios anteriores son aplicadas. */
    val showFormerDoses: Boolean get() = filter == DoseFilter.All || filter == DoseFilter.Applied
}
