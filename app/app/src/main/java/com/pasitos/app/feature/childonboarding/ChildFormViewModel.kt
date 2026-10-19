package com.pasitos.app.feature.childonboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pasitos.app.core.navigation.PsRoute
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildSex
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Alta (RF-HIJ-01) y edición (RF-HIJ-02, RF-HIJ-03) de un hijo. Antes de guardar puede pedir dos
 * confirmaciones, en este orden: medidas al nacer poco habituales (CA4) y, al editar, el recálculo
 * por cambiar la fecha de nacimiento, el sexo o las semanas de gestación.
 */
@HiltViewModel
class ChildFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val childRepository: ChildRepository,
    networkMonitor: NetworkMonitor,
    clock: ArgentinaClock,
) : ViewModel() {

    private val childId: String? = savedStateHandle.toRoute<PsRoute.ChildForm>().childId

    private val _uiState = MutableStateFlow(
        clock.today().let { today ->
            ChildFormUiState(childId = childId, today = today, minBirthDate = minBirthDate(today), isLoading = childId != null)
        },
    )
    val uiState: StateFlow<ChildFormUiState> = _uiState.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    /** El perfil como estaba antes de editarlo. */
    private var original: Child? = null
    private var measurementsConfirmed = false
    private var recalculationConfirmed = false

    init {
        networkMonitor.isOnline
            .onEach { online -> _uiState.update { it.copy(isOnline = online) } }
            .launchIn(viewModelScope)
        if (childId != null) {
            viewModelScope.launch {
                // Se espera a la base local: al abrir la app puede no haber terminado de leerla
                val child = childRepository.children.map { children -> children.firstOrNull { it.id == childId } }.filterNotNull().first()
                original = child
                _uiState.update { it.withChild(child) }
            }
        }
    }

    fun onFirstNameChange(value: String) = edit { copy(firstName = value, errors = errors.copy(firstName = null)) }

    fun onLastNameChange(value: String) = edit { copy(lastName = value, errors = errors.copy(lastName = null)) }

    fun onSexSelected(sex: ChildSex) = edit { copy(sex = sex, errors = errors.copy(sex = null)) }

    fun onBirthDateSelected(date: LocalDate) = edit { copy(birthDate = date, errors = errors.copy(birthDate = null)) }

    fun onGestationalWeeksChange(value: String) =
        edit { copy(gestationalWeeks = value.filter(Char::isDigit).take(2), errors = errors.copy(gestationalWeeks = null)) }

    fun onGestationalWeeksUnknownChange(unknown: Boolean) = edit {
        copy(
            gestationalWeeksUnknown = unknown,
            gestationalWeeks = if (unknown) "" else gestationalWeeks,
            errors = errors.copy(gestationalWeeks = null),
        )
    }

    fun onBirthWeightChange(value: String) = edit { copy(birthWeightKg = value, errors = errors.copy(birthWeightKg = null)) }

    fun onBirthLengthChange(value: String) = edit { copy(birthLengthCm = value, errors = errors.copy(birthLengthCm = null)) }

    fun onBirthHeadCircumferenceChange(value: String) =
        edit { copy(birthHeadCircumferenceCm = value, errors = errors.copy(birthHeadCircumferenceCm = null)) }

    fun onAvatarSelected(avatarKey: String?) = edit { copy(avatarKey = avatarKey) }

    fun onAllergiesChange(value: String) = edit { copy(allergies = value, errors = errors.copy(allergies = null)) }

    fun onRelevantHistoryChange(value: String) = edit { copy(relevantHistory = value, errors = errors.copy(relevantHistory = null)) }

    fun onRepresentationDeclaredChange(declared: Boolean) =
        edit { copy(representationDeclared = declared, errors = errors.copy(representation = null)) }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading || !state.isOnline) return
        val errors = validate(state)
        if (errors.any) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        val implausible = implausibleMeasurements(state)
        if (implausible.isNotEmpty() && !measurementsConfirmed) {
            _uiState.update { it.copy(implausibleMeasurements = implausible) }
            return
        }
        val original = original
        if (original != null && needsRecalculation(state, original) && !recalculationConfirmed) {
            _uiState.update { it.copy(confirmRecalculation = true) }
            return
        }
        save(state)
    }

    /** Las medidas poco habituales son correctas: se guardan igual (RF-HIJ-01, CA4). */
    fun onConfirmMeasurements() {
        measurementsConfirmed = true
        _uiState.update { it.copy(implausibleMeasurements = emptyList()) }
        onSubmit()
    }

    fun onReviewMeasurements() {
        _uiState.update { it.copy(implausibleMeasurements = emptyList()) }
    }

    /** Segunda confirmación después de la advertencia de recálculo (RF-HIJ-02, CA1). */
    fun onConfirmRecalculation() {
        recalculationConfirmed = true
        _uiState.update { it.copy(confirmRecalculation = false) }
        onSubmit()
    }

    fun onDismissRecalculation() {
        _uiState.update { it.copy(confirmRecalculation = false) }
    }

    private fun save(state: ChildFormUiState) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, generalError = null) }
            val input = state.toInput()
            val result = if (childId == null) {
                childRepository.addChild(input, state.representationDeclared)
            } else {
                childRepository.updateChild(childId, input)
            }
            result
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                    _saved.send(Unit)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSaving = false, generalError = error.message ?: "No pudimos guardar el perfil. Probá de nuevo.")
                    }
                }
        }
    }

    /** Cualquier cambio vuelve a pedir las confirmaciones, que eran sobre los datos anteriores. */
    private fun edit(change: ChildFormUiState.() -> ChildFormUiState) {
        measurementsConfirmed = false
        recalculationConfirmed = false
        _uiState.update { it.change().copy(generalError = null) }
    }
}
