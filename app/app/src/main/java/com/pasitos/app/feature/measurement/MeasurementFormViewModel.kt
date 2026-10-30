package com.pasitos.app.feature.measurement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pasitos.app.core.navigation.PsRoute
import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.toDecimalText
import com.pasitos.app.data.measurement.DuplicateMeasurementException
import com.pasitos.app.data.measurement.MeasurementRepository
import com.pasitos.app.data.measurement.model.LengthPosition
import com.pasitos.app.data.measurement.model.Measurement
import com.pasitos.app.data.measurement.model.MeasurementInput
import com.pasitos.shared.api.AuthorRole
import com.pasitos.shared.growth.MeasurementValidation
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Cargar una medición (RF-CRE-01), editar o eliminar la propia y ver la de otra persona (RF-CRE-02),
 * con la forma de medición según la edad (RF-CRE-10). Funciona sin conexión (RNF-OFF-02).
 */
@HiltViewModel
class MeasurementFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val measurementRepository: MeasurementRepository,
    childRepository: ChildRepository,
    private val authRepository: AuthRepository,
    private val clock: ArgentinaClock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<PsRoute.MeasurementForm>()

    private val _uiState = MutableStateFlow(MeasurementFormUiState(maxDate = clock.today()))
    val uiState: StateFlow<MeasurementFormUiState> = _uiState.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)

    /** Se guardó, se eliminó o se descartó: la pantalla vuelve atrás. */
    val done = _done.receiveAsFlow()

    private val _openMeasurement = Channel<String>(Channel.BUFFERED)

    /** RF-CRE-01, CA4: abrir la medición que el tutor ya cargó en esa fecha. */
    val openMeasurement = _openMeasurement.receiveAsFlow()

    private var child: Child? = null
    private var prefilled = false

    /** Si el tutor eligió la forma de medición, cambiar la fecha ya no la cambia. */
    private var positionTouched = false

    /** La medición desaparece porque el tutor la eliminó o la descartó: no es "no encontrada". */
    private var leaving = false

    init {
        val measurement = route.measurementId?.let(measurementRepository::observeMeasurement) ?: flowOf(null)
        // Se observan (no se leen una sola vez) porque los hijos pueden no estar cargados todavía y
        // la medición cambia de estado cuando se sincroniza
        combine(childRepository.children.map { children -> children.firstOrNull { it.id == route.childId } }, measurement) { c, m -> c to m }
            .onEach { (child, measurement) -> onLoaded(child, measurement) }
            .launchIn(viewModelScope)
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(date = date, dateError = null, generalError = null).withAgeRules() }
    }

    fun onStandingSelected(standing: Boolean) {
        positionTouched = true
        _uiState.update { it.copy(standing = standing).withAgeRules() }
    }

    fun onWeightChange(value: String) {
        _uiState.update { it.copy(weightKg = value, weightError = null, generalError = null) }
    }

    fun onLengthChange(value: String) {
        _uiState.update { it.copy(lengthCm = value, lengthError = null, generalError = null) }
    }

    fun onHeadCircumferenceChange(value: String) {
        _uiState.update { it.copy(headCircumferenceCm = value, headCircumferenceError = null, generalError = null) }
    }

    fun onSave() {
        val state = _uiState.value
        val child = child ?: return
        if (!state.canEdit || state.isSaving) return
        val input = validate(state, child) ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, generalError = null) }
            // RF-CRE-01, CA4: antes de guardar, para ofrecer editar la que ya existe
            val existing = measurementRepository.findOwnOn(route.childId, input.date, excludeId = route.measurementId)
            if (existing != null) {
                showDuplicate(existing.id, input.date)
                return@launch
            }
            val measurementId = route.measurementId
            val result = if (measurementId == null) {
                measurementRepository.addMeasurement(route.childId, input).map { }
            } else {
                measurementRepository.updateMeasurement(measurementId, input).map { }
            }
            result
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                    _done.send(Unit)
                }
                .onFailure { error ->
                    if (error is DuplicateMeasurementException) {
                        showDuplicate(error.existingId, input.date)
                    } else {
                        _uiState.update {
                            it.copy(isSaving = false, generalError = error.message ?: "No pudimos guardar la medición. Probá de nuevo.")
                        }
                    }
                }
        }
    }

    fun onEditDuplicate() {
        val duplicate = _uiState.value.duplicate ?: return
        _uiState.update { it.copy(duplicate = null) }
        viewModelScope.launch { _openMeasurement.send(duplicate.existingId) }
    }

    fun onDismissDuplicate() {
        _uiState.update { it.copy(duplicate = null) }
    }

    fun onDeleteClick() {
        if (_uiState.value.mode == MeasurementFormMode.EDIT) _uiState.update { it.copy(confirmDelete = true, generalError = null) }
    }

    fun onDismissDelete() {
        _uiState.update { it.copy(confirmDelete = false) }
    }

    /** RF-CRE-02, CA1: con confirmación. */
    fun onConfirmDelete() {
        val id = route.measurementId ?: return
        leaving = true
        viewModelScope.launch {
            _uiState.update { it.copy(confirmDelete = false, isSaving = true) }
            measurementRepository.deleteMeasurement(id)
                .onSuccess { _done.send(Unit) }
                .onFailure(::showError)
        }
    }

    /** Decisión de I1-A6: un cambio rechazado se corrige guardando de nuevo, o se descarta. */
    fun onDiscardChange() {
        val id = route.measurementId ?: return
        leaving = true
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            measurementRepository.discardChange(id)
                .onSuccess { _done.send(Unit) }
                .onFailure(::showError)
        }
    }

    private fun showError(error: Throwable) {
        leaving = false
        _uiState.update { it.copy(isSaving = false, generalError = error.message ?: "No pudimos guardar el cambio. Probá de nuevo.") }
    }

    private fun showDuplicate(existingId: String, date: LocalDate) {
        _uiState.update {
            it.copy(isSaving = false, duplicate = DuplicateMeasurementUi(existingId, date.format(MeasurementDateFormatter)))
        }
    }

    private fun onLoaded(child: Child?, measurement: Measurement?) {
        if (child == null) return
        this.child = child
        if (route.measurementId != null && measurement == null) {
            if (!leaving) _uiState.update { it.copy(isLoaded = true, notFound = true) }
            return
        }
        val isOwn = measurement == null || measurement.authorAccountId == authRepository.currentSession.value?.userId
        _uiState.update { state ->
            val loaded = state.copy(
                isLoaded = true,
                notFound = false,
                mode = when {
                    measurement == null -> MeasurementFormMode.NEW
                    isOwn -> MeasurementFormMode.EDIT
                    else -> MeasurementFormMode.READ_ONLY
                },
                finalized = child.finalized,
                minDate = child.birthDate,
                authorText = measurement?.authorText(isOwn),
                syncState = measurement?.syncState,
                syncError = measurement?.syncError,
                readOnlyRows = measurement?.takeUnless { isOwn }?.readOnlyRows().orEmpty(),
            )
            if (prefilled) loaded.withAgeRules() else loaded.prefill(measurement).withAgeRules().also { prefilled = true }
        }
    }

    private fun MeasurementFormUiState.prefill(measurement: Measurement?): MeasurementFormUiState {
        val child = checkNotNull(child)
        if (measurement == null) {
            val today = clock.today()
            return copy(date = today, standing = MeasurementValidation.defaultStanding(child.birthDate, today))
        }
        // Al editar, la forma guardada se respeta aunque cambie la fecha
        positionTouched = true
        return copy(
            date = measurement.date,
            weightKg = measurement.weightKg?.toDecimalText().orEmpty(),
            lengthCm = measurement.lengthCm?.toDecimalText().orEmpty(),
            standing = measurement.lengthPosition?.let { it == LengthPosition.STANDING }
                ?: MeasurementValidation.defaultStanding(child.birthDate, measurement.date),
            headCircumferenceCm = measurement.headCircumferenceCm?.toDecimalText().orEmpty(),
        )
    }

    /** RF-CRE-10 y RN-CRE-01: lo que se ofrece depende de la edad a la fecha de la medición. */
    private fun MeasurementFormUiState.withAgeRules(): MeasurementFormUiState {
        val child = child ?: return this
        val date = date ?: return this
        val offersLying = MeasurementValidation.offersLying(child.birthDate, date)
        val standing = when {
            !offersLying -> true
            positionTouched -> standing
            else -> MeasurementValidation.defaultStanding(child.birthDate, date)
        }
        return copy(
            offersLying = offersLying,
            standing = standing,
            offersHeadCircumference = MeasurementValidation.offersHeadCircumference(child.birthDate, date),
        )
    }

    /** RF-CRE-01 (CA2 y rangos de RN-CRE-11) y RN-CRE-03. Devuelve lo que se guarda, o nulo si hay errores. */
    private fun validate(state: MeasurementFormUiState, child: Child): MeasurementInput? {
        val today = clock.today()
        val date = state.date ?: today
        val dateError = when {
            date.isBefore(child.birthDate) -> "La fecha no puede ser anterior al nacimiento"
            date.isAfter(today) -> "La fecha no puede ser posterior a hoy"
            else -> null
        }
        val weight = parse(state.weightKg, MeasurementValidation.WEIGHT_DECIMALS, MeasurementValidation.WEIGHT_KG_RANGE, "El peso", "kg")
        val length = parse(state.lengthCm, MeasurementValidation.LENGTH_DECIMALS, MeasurementValidation.LENGTH_CM_RANGE, "La longitud o talla", "cm")
        val headCircumference = if (state.offersHeadCircumference) {
            parse(state.headCircumferenceCm, MeasurementValidation.HEAD_CIRCUMFERENCE_DECIMALS, MeasurementValidation.HEAD_CIRCUMFERENCE_CM_RANGE, "El perímetro cefálico", "cm")
        } else {
            Parsed()
        }

        val noValue = weight.value == null && length.value == null && headCircumference.value == null &&
            weight.error == null && length.error == null && headCircumference.error == null
        _uiState.update {
            it.copy(
                dateError = dateError,
                weightError = weight.error,
                lengthError = length.error,
                headCircumferenceError = headCircumference.error,
                generalError = if (noValue) "Cargá al menos un valor: peso, longitud o talla, o perímetro cefálico" else null,
            )
        }
        if (dateError != null || weight.error != null || length.error != null || headCircumference.error != null || noValue) return null

        return MeasurementInput(
            date = date,
            weightKg = weight.value,
            lengthCm = length.value,
            lengthPosition = length.value?.let { if (state.standing || !state.offersLying) LengthPosition.STANDING else LengthPosition.LYING },
            headCircumferenceCm = headCircumference.value,
        )
    }

    private data class Parsed(val value: Double? = null, val error: String? = null)

    /** "7,25" → 7.25. Vacío es "no se cargó"; fuera del rango absoluto se rechaza con el motivo. */
    private fun parse(text: String, decimals: Int, range: ClosedFloatingPointRange<Double>, name: String, unit: String): Parsed {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Parsed()
        val number = trimmed.replace(',', '.').toBigDecimalOrNull() ?: return Parsed(error = "Ingresá un número válido")
        if (number.stripTrailingZeros().scale() > decimals) {
            return Parsed(error = if (decimals == 1) "Usá un solo decimal" else "Usá hasta $decimals decimales")
        }
        val value = number.toDouble()
        if (value !in range) {
            return Parsed(error = "$name tiene que estar entre ${range.start.toDecimalText()} y ${range.endInclusive.toDecimalText()} $unit")
        }
        return Parsed(value = value)
    }

    private fun Measurement.authorText(isOwn: Boolean): String = when {
        isOwn -> "La cargaste vos"
        authorRole == AuthorRole.PEDIATRICIAN -> "La cargó el pediatra"
        else -> "La cargó la familia"
    }

    private fun Measurement.readOnlyRows(): List<Pair<String, String>> = listOfNotNull(
        "Fecha" to date.format(MeasurementDateFormatter),
        weightText()?.let { "Peso" to it },
        lengthText()?.let { (if (lengthPosition == LengthPosition.STANDING) "Talla" else "Longitud") to it },
        headCircumferenceText()?.let { "Perímetro cefálico" to it },
    )
}
