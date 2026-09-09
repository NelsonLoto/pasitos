package com.ninosano.app.feature.vaccination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ninosano.app.data.child.ChildRepository
import com.ninosano.app.data.child.model.Child
import com.ninosano.app.data.vaccination.VaccinationRepository
import com.ninosano.app.data.vaccination.model.AppliedDose
import com.ninosano.app.data.vaccination.model.VaccineDoseSchedule
import com.ninosano.app.data.vaccination.model.displayName
import com.ninosano.app.data.vaccination.resolveDoseStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

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
        val status = resolveDoseStatus(dose, activeChild?.birthDate, isApplied)
        return VaccineDoseUiModel(id = dose.id, displayName = dose.displayName, ageLabel = dose.recommendedAgeLabel, status = status)
    }
}
