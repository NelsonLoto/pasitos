package com.ninosano.app.feature.measurement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ninosano.app.core.navigation.NsRoute
import com.ninosano.app.data.measurement.MeasurementRepository
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
class MeasurementFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val measurementRepository: MeasurementRepository,
) : ViewModel() {

    private val childId: String = savedStateHandle.toRoute<NsRoute.MeasurementForm>().childId

    private val _uiState = MutableStateFlow(MeasurementFormUiState())
    val uiState: StateFlow<MeasurementFormUiState> = _uiState.asStateFlow()

    private val _navigateBack = Channel<Unit>(Channel.BUFFERED)
    val navigateBack = _navigateBack.receiveAsFlow()

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(date = date) }
    }

    fun onWeightChange(value: String) {
        _uiState.update { it.copy(weightKg = value, weightError = null, generalError = null) }
    }

    fun onHeightChange(value: String) {
        _uiState.update { it.copy(heightCm = value, heightError = null, generalError = null) }
    }

    fun onHeadCircumferenceChange(value: String) {
        _uiState.update { it.copy(headCircumferenceCm = value, headCircumferenceError = null, generalError = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        val weight = parseDecimal(state.weightKg)
        val height = parseDecimal(state.heightCm)
        val headCircumference = parseDecimal(state.headCircumferenceCm)

        val weightError = if (state.weightKg.isNotBlank() && weight == null) "Ingresá un número válido" else null
        val heightError = if (state.heightCm.isNotBlank() && height == null) "Ingresá un número válido" else null
        val headCircumferenceError = if (state.headCircumferenceCm.isNotBlank() && headCircumference == null) "Ingresá un número válido" else null

        if (weightError != null || heightError != null || headCircumferenceError != null) {
            _uiState.update {
                it.copy(weightError = weightError, heightError = heightError, headCircumferenceError = headCircumferenceError)
            }
            return
        }
        if (weight == null && height == null && headCircumference == null) {
            _uiState.update { it.copy(generalError = "Cargá al menos un valor (peso, talla o perímetro cefálico)") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            measurementRepository.addMeasurement(childId, state.date, weight, height, headCircumference)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigateBack.send(Unit)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, generalError = error.message ?: "No pudimos guardar la medición. Probá de nuevo.")
                    }
                }
        }
    }

    private fun parseDecimal(value: String): Double? =
        value.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
}
