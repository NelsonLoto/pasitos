package com.ninosano.app.feature.childonboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ninosano.app.data.child.ChildRepository
import com.ninosano.app.data.child.model.ChildSex
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
class ChildOnboardingViewModel @Inject constructor(
    private val childRepository: ChildRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildOnboardingUiState())
    val uiState: StateFlow<ChildOnboardingUiState> = _uiState.asStateFlow()

    private val _navigateToHome = Channel<Unit>(Channel.BUFFERED)
    val navigateToHome = _navigateToHome.receiveAsFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null, generalError = null) }
    }

    fun onBirthDateSelected(date: LocalDate) {
        _uiState.update { it.copy(birthDate = date, birthDateError = null, generalError = null) }
    }

    fun onSexSelected(sex: ChildSex) {
        _uiState.update { it.copy(sex = sex) }
    }

    fun onSubmit() {
        val state = _uiState.value
        val nameError = validateName(state.name)
        val birthDateError = validateBirthDate(state.birthDate)
        if (nameError != null || birthDateError != null) {
            _uiState.update { it.copy(nameError = nameError, birthDateError = birthDateError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            childRepository.addChild(state.name, state.birthDate!!, state.sex)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigateToHome.send(Unit)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, generalError = error.message ?: "No pudimos guardar el perfil. Probá de nuevo.")
                    }
                }
        }
    }

    private fun validateName(value: String): String? = when {
        value.isBlank() -> "Ingresá el nombre de tu hijo/a"
        else -> null
    }

    private fun validateBirthDate(value: LocalDate?): String? = when {
        value == null -> "Seleccioná la fecha de nacimiento"
        value.isAfter(LocalDate.now()) -> "La fecha no puede ser futura"
        else -> null
    }
}
