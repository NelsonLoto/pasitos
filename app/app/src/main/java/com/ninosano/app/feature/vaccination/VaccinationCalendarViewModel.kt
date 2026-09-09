package com.ninosano.app.feature.vaccination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ninosano.app.data.child.ChildRepository
import com.ninosano.app.data.child.model.Child
import com.ninosano.app.data.vaccination.VaccinationRepository
import com.ninosano.app.data.vaccination.model.AppliedDose
import com.ninosano.app.data.vaccination.model.DoseStatus
import com.ninosano.app.data.vaccination.model.VaccineDoseSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Días de gracia antes de marcar una dosis no aplicada como "Atrasada" en vez de "Pendiente". */
private const val OVERDUE_GRACE_DAYS = 30

@HiltViewModel
class VaccinationCalendarViewModel @Inject constructor(
    private val childRepository: ChildRepository,
    private val vaccinationRepository: VaccinationRepository,
) : ViewModel() {

    val uiState: StateFlow<VaccinationCalendarUiState> = combine(
        childRepository.children,
        childRepository.activeChildId,
        vaccinationRepository.appliedDoses,
    ) { children, activeChildId, appliedDoses ->
        val activeChild = children.firstOrNull { it.id == activeChildId }
        VaccinationCalendarUiState(
            childName = activeChild?.name.orEmpty(),
            doses = vaccinationRepository.schedule.map { dose ->
                toUiModel(dose, activeChild, activeChildId, appliedDoses)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaccinationCalendarUiState())

    private fun toUiModel(
        dose: VaccineDoseSchedule,
        activeChild: Child?,
        activeChildId: String?,
        appliedDoses: List<AppliedDose>,
    ): VaccineDoseUiModel {
        val isApplied = appliedDoses.any { it.childId == activeChildId && it.doseScheduleId == dose.id }
        val status = when {
            isApplied -> DoseStatus.Applied
            activeChild == null -> DoseStatus.Pending
            else -> {
                val ageInDays = ChronoUnit.DAYS.between(activeChild.birthDate, LocalDate.now())
                if (ageInDays > dose.recommendedAgeInDays + OVERDUE_GRACE_DAYS) DoseStatus.Overdue else DoseStatus.Pending
            }
        }
        val displayName = if (dose.doseLabel != null) "${dose.vaccineName} (${dose.doseLabel})" else dose.vaccineName
        return VaccineDoseUiModel(id = dose.id, displayName = displayName, ageLabel = dose.recommendedAgeLabel, status = status)
    }
}
