package com.ninosano.app.feature.vaccinedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ninosano.app.core.designsystem.component.NsBadgeTone
import com.ninosano.app.core.navigation.NsRoute
import com.ninosano.app.data.child.ChildRepository
import com.ninosano.app.data.vaccination.VaccinationRepository
import com.ninosano.app.data.vaccination.model.DoseStatus
import com.ninosano.app.data.vaccination.model.VaccineDoseSchedule
import com.ninosano.app.data.vaccination.model.displayName
import com.ninosano.app.data.vaccination.resolveDoseStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class VaccineDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val childRepository: ChildRepository,
    private val vaccinationRepository: VaccinationRepository,
) : ViewModel() {

    private val doseScheduleId: String = savedStateHandle.toRoute<NsRoute.VaccineDetail>().vaccineId
    private val dose: VaccineDoseSchedule = vaccinationRepository.schedule.first { it.id == doseScheduleId }

    private val _uiState = MutableStateFlow(buildInitialState())
    val uiState: StateFlow<VaccineDetailUiState> = _uiState.asStateFlow()

    private val _navigateBack = Channel<Unit>(Channel.BUFFERED)
    val navigateBack = _navigateBack.receiveAsFlow()

    private fun buildInitialState(): VaccineDetailUiState {
        val activeChildId = childRepository.activeChildId.value
        val activeChild = childRepository.children.value.firstOrNull { it.id == activeChildId }
        val appliedDose = vaccinationRepository.appliedDoses.value
            .firstOrNull { it.childId == activeChildId && it.doseScheduleId == doseScheduleId }
        val status = resolveDoseStatus(dose, activeChild?.birthDate, isApplied = appliedDose != null)

        return VaccineDetailUiState(
            vaccineDisplayName = dose.displayName,
            statusText = statusText(status, dose.recommendedAgeLabel, appliedDose?.appliedDate),
            statusTone = statusTone(status),
            recommendedAgeLabel = dose.recommendedAgeLabel,
            description = dose.description,
            date = appliedDose?.appliedDate ?: LocalDate.now(),
            lot = appliedDose?.lot.orEmpty(),
        )
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(date = date, dateError = null, generalError = null) }
    }

    fun onLotChange(value: String) {
        _uiState.update { it.copy(lot = value, generalError = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        val date = state.date
        if (date == null) {
            _uiState.update { it.copy(dateError = "Seleccioná la fecha de aplicación") }
            return
        }
        val activeChildId = childRepository.activeChildId.value
        if (activeChildId == null) {
            _uiState.update { it.copy(generalError = "No hay un hijo/a seleccionado/a") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            vaccinationRepository.markApplied(activeChildId, doseScheduleId, date, state.lot.trim().ifBlank { null })
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigateBack.send(Unit)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, generalError = error.message ?: "No pudimos registrar la aplicación. Probá de nuevo.")
                    }
                }
        }
    }

    private fun statusText(status: DoseStatus, ageLabel: String, appliedDate: LocalDate?): String = when (status) {
        DoseStatus.Overdue -> "Atrasada · Correspondía a los $ageLabel"
        DoseStatus.Pending -> "Pendiente · Recomendada a los $ageLabel"
        DoseStatus.Applied -> "Aplicada" + (appliedDate?.let { " · ${DateDisplayFormatter.format(it)}" } ?: "")
    }

    private fun statusTone(status: DoseStatus): NsBadgeTone = when (status) {
        DoseStatus.Applied -> NsBadgeTone.Success
        DoseStatus.Overdue -> NsBadgeTone.Error
        DoseStatus.Pending -> NsBadgeTone.Warning
    }

    private companion object {
        val DateDisplayFormatter: java.time.format.DateTimeFormatter =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}
