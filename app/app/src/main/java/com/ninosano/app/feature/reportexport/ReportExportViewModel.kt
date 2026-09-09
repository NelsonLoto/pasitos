package com.ninosano.app.feature.reportexport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ninosano.app.data.child.ChildRepository
import com.ninosano.app.data.report.ReportRepository
import com.ninosano.app.data.report.model.ReportSection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ReportExportViewModel @Inject constructor(
    private val childRepository: ChildRepository,
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildInitialState())
    val uiState: StateFlow<ReportExportUiState> = _uiState.asStateFlow()

    private fun buildInitialState(): ReportExportUiState {
        val activeChildId = childRepository.activeChildId.value
        val activeChild = childRepository.children.value.firstOrNull { it.id == activeChildId }
        return ReportExportUiState(childName = activeChild?.name.orEmpty())
    }

    fun onToggleGrowthChart(checked: Boolean) {
        _uiState.update { it.copy(includeGrowthChart = checked, generalError = null, successMessage = null) }
    }

    fun onToggleMeasurements(checked: Boolean) {
        _uiState.update { it.copy(includeMeasurements = checked, generalError = null, successMessage = null) }
    }

    fun onToggleVaccinationStatus(checked: Boolean) {
        _uiState.update { it.copy(includeVaccinationStatus = checked, generalError = null, successMessage = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        val sections = buildSet {
            if (state.includeGrowthChart) add(ReportSection.GrowthChart)
            if (state.includeMeasurements) add(ReportSection.Measurements)
            if (state.includeVaccinationStatus) add(ReportSection.VaccinationStatus)
        }
        if (sections.isEmpty()) {
            _uiState.update { it.copy(generalError = "Seleccioná al menos una sección para incluir en el reporte") }
            return
        }
        val activeChildId = childRepository.activeChildId.value
        if (activeChildId == null) {
            _uiState.update { it.copy(generalError = "No hay un hijo/a seleccionado/a") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null, successMessage = null) }
            reportRepository.generateReport(activeChildId, sections)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Reporte generado y listo para compartir.") }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, generalError = error.message ?: "No pudimos generar el reporte. Probá de nuevo.")
                    }
                }
        }
    }
}
