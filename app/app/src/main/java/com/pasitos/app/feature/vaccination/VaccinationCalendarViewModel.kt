package com.pasitos.app.feature.vaccination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.vaccination.VaccinationRepository
import com.pasitos.app.data.vaccination.model.AppliedDoseRecord
import com.pasitos.app.data.vaccination.model.OtherVaccine
import com.pasitos.app.data.vaccination.model.VaccinationCalendar
import com.pasitos.app.data.vaccination.model.ageMonthsLabel
import com.pasitos.app.data.vaccination.model.summary
import com.pasitos.shared.vaccination.DoseStatus
import com.pasitos.shared.vaccination.DoseWithStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Calendario del hijo activo (RF-VAC-01): dosis agrupadas por edad con su estado, resumen, filtro,
 * dosis de calendarios anteriores y otras vacunas. Lee la base local, también sin conexión.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VaccinationCalendarViewModel @Inject constructor(
    childRepository: ChildRepository,
    private val vaccinationRepository: VaccinationRepository,
    private val clock: ArgentinaClock,
) : ViewModel() {

    private val filter = MutableStateFlow(DoseFilter.All)

    private val activeChild = combine(childRepository.children, childRepository.activeChildId) { children, activeId ->
        children.firstOrNull { it.id == activeId }
    }.distinctUntilChanged()

    val uiState: StateFlow<VaccinationCalendarUiState> = activeChild.flatMapLatest { child ->
        if (child == null) {
            filter.map { VaccinationCalendarUiState(isLoaded = true, filter = it) }
        } else {
            combine(
                vaccinationRepository.calendar,
                vaccinationRepository.observeAppliedDoses(child.id),
                vaccinationRepository.observeOtherVaccines(child.id),
                filter,
            ) { calendar, applied, others, filter -> buildState(child, calendar, applied, others, filter) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaccinationCalendarUiState())

    fun onFilterSelected(value: DoseFilter) {
        filter.value = value
    }

    private fun buildState(
        child: Child,
        calendar: VaccinationCalendar,
        applied: List<AppliedDoseRecord>,
        others: List<OtherVaccine>,
        filter: DoseFilter,
    ): VaccinationCalendarUiState {
        val today = clock.today()
        val status = calendar.statusFor(child.birthDate, applied, today)
        val groups = status.doses
            .filter { filter.status == null || it.status == filter.status }
            .groupBy { it.dose.ageMonths }
            .toSortedMap()
            .map { (ageMonths, doses) -> AgeGroupUi(label = ageMonthsLabel(ageMonths), doses = doses.map { it.toRow() }) }
        return VaccinationCalendarUiState(
            childId = child.id,
            childName = child.firstName,
            isLoaded = true,
            hasCalendar = !calendar.isEmpty,
            summary = status.summary().takeUnless { calendar.isEmpty },
            filter = filter,
            groups = groups,
            formerDoses = status.otherApplied.mapNotNull { dose ->
                val record = applied.firstOrNull { it.doseCode == dose.doseCode } ?: return@mapNotNull null
                val former = calendar.former(dose.doseCode)
                RecordRowUi(
                    id = dose.doseCode,
                    title = former?.vaccineName ?: dose.doseCode,
                    detail = listOfNotNull(former?.doseLabel, appliedDateText(record)).joinToString(" · "),
                )
            },
            otherVaccines = others.map { vaccine ->
                RecordRowUi(id = vaccine.id, title = vaccine.name, detail = vaccine.appliedOn.format(VaccinationDateFormatter))
            },
            canMarkPreviousDoses = calendar.previousDoses(child.birthDate, applied.mapTo(mutableSetOf()) { it.doseCode }, today)
                .isNotEmpty(),
        )
    }

    private fun DoseWithStatus.toRow(): DoseRowUi {
        val indicated = indicatedDate.format(VaccinationDateFormatter)
        val dateText = when (status) {
            DoseStatus.APPLIED -> applied?.appliedOn?.format(VaccinationDateFormatter) ?: "Fecha no registrada"
            DoseStatus.OVERDUE, DoseStatus.DUE -> "Desde el $indicated"
            DoseStatus.UPCOMING, DoseStatus.SCHEDULED -> indicated
        }
        return DoseRowUi(
            doseCode = dose.doseCode,
            vaccineName = dose.vaccineName,
            detail = "${dose.doseLabel} · $dateText",
            status = status,
        )
    }

    private fun appliedDateText(record: AppliedDoseRecord): String =
        record.appliedOn?.format(VaccinationDateFormatter) ?: "Fecha no registrada"
}
