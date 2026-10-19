package com.pasitos.app.feature.vaccinedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pasitos.app.core.navigation.PsRoute
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.vaccination.VaccinationRepository
import com.pasitos.app.data.vaccination.model.AppliedDoseRecord
import com.pasitos.app.data.vaccination.model.DoseApplicationInput
import com.pasitos.app.data.vaccination.model.VaccinationCalendar
import com.pasitos.app.data.vaccination.model.ageMonthsLabel
import com.pasitos.app.feature.vaccination.VaccinationDateFormatter
import com.pasitos.shared.api.AuthorRole
import com.pasitos.shared.api.vaccination.MAX_LOT_LENGTH
import com.pasitos.shared.api.vaccination.MAX_PLACE_LENGTH
import com.pasitos.shared.vaccination.DoseStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lo que cambia en la pantalla sin venir de la base: el formulario, el guardado y la confirmación. */
private data class LocalState(
    val isEditing: Boolean = false,
    val form: DoseFormUi = DoseFormUi(),
    val isSaving: Boolean = false,
    val confirmDelete: Boolean = false,
    val error: String? = null,
)

/** Detalle de una dosis del hijo (RF-VAC-07) con su registro, edición y eliminación (RF-VAC-02 y 03). */
@HiltViewModel
class VaccineDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    childRepository: ChildRepository,
    private val vaccinationRepository: VaccinationRepository,
    private val authRepository: AuthRepository,
    networkMonitor: NetworkMonitor,
    private val clock: ArgentinaClock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<PsRoute.VaccineDetail>()
    private val local = MutableStateFlow(LocalState())

    /** El registro de esta dosis, si está aplicada. */
    private var record: AppliedDoseRecord? = null

    val uiState: StateFlow<VaccineDetailUiState> = combine(
        childRepository.children.map { children -> children.firstOrNull { it.id == route.childId } },
        vaccinationRepository.calendar,
        vaccinationRepository.observeAppliedDoses(route.childId),
        networkMonitor.isOnline,
        local,
    ) { child, calendar, applied, online, local ->
        this.record = applied.firstOrNull { it.doseCode == route.doseCode }
        buildState(child, calendar, applied, online, local)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaccineDetailUiState())

    private val _done = Channel<Unit>(Channel.BUFFERED)

    /** Se registró o se eliminó la dosis: la pantalla vuelve al calendario. */
    val done = _done.receiveAsFlow()

    fun onDateSelected(date: LocalDate) = editForm { copy(date = date, dateError = null) }

    fun onLotChange(value: String) = editForm { copy(lot = value, lotError = null) }

    fun onPlaceChange(value: String) = editForm { copy(place = value, placeError = null) }

    /** RF-VAC-03 y RF-VAC-04 CA3: el formulario con los datos actuales. */
    fun onEditClick() {
        val record = record ?: return
        if (uiState.value.applied?.isOwn != true) return
        local.update {
            it.copy(
                isEditing = true,
                form = DoseFormUi(date = record.appliedOn, lot = record.lot.orEmpty(), place = record.place.orEmpty()),
                error = null,
            )
        }
    }

    fun onCancelEdit() {
        local.update { it.copy(isEditing = false, form = DoseFormUi(), error = null) }
    }

    fun onSave() {
        val state = uiState.value
        if (!state.showForm || state.isSaving || !state.isOnline) return
        val form = validate(state) ?: return
        val input = DoseApplicationInput(appliedOn = form.date, lot = form.lot, place = form.place)
        val record = record

        viewModelScope.launch {
            local.update { it.copy(isSaving = true, error = null) }
            if (record == null) {
                vaccinationRepository.registerDose(route.childId, route.doseCode, input)
                    .onSuccess {
                        local.update { it.copy(isSaving = false) }
                        _done.send(Unit)
                    }
                    .onFailure(::showError)
            } else {
                vaccinationRepository.updateDose(record.id, input)
                    .onSuccess { local.update { it.copy(isSaving = false, isEditing = false, form = DoseFormUi()) } }
                    .onFailure(::showError)
            }
        }
    }

    fun onDeleteClick() {
        if (uiState.value.applied?.isOwn == true && uiState.value.isOnline) local.update { it.copy(confirmDelete = true, error = null) }
    }

    fun onDismissDelete() {
        local.update { it.copy(confirmDelete = false) }
    }

    /** RF-VAC-03 CA1: con confirmación; el estado se recalcula en el calendario. */
    fun onConfirmDelete() {
        val record = record ?: return
        viewModelScope.launch {
            local.update { it.copy(isSaving = true, confirmDelete = false, error = null) }
            vaccinationRepository.deleteDose(record.id)
                .onSuccess {
                    local.update { it.copy(isSaving = false) }
                    _done.send(Unit)
                }
                .onFailure(::showError)
        }
    }

    private fun showError(error: Throwable) {
        local.update { it.copy(isSaving = false, error = error.message ?: "No pudimos guardar la dosis. Probá de nuevo.") }
    }

    /** RN-VAC-02 y los límites de SUP-24. Devuelve el formulario limpio, o nulo si hay errores. */
    private fun validate(state: VaccineDetailUiState): DoseFormUi? {
        val form = state.form.copy(lot = state.form.lot.trim(), place = state.form.place.trim())
        val date = form.date
        val dateError = when {
            date == null -> if (state.dateRequired) "Elegí la fecha de aplicación" else null
            state.minDate != null && date.isBefore(state.minDate) -> "La fecha no puede ser anterior al nacimiento"
            date.isAfter(state.maxDate) -> "La fecha no puede ser posterior a hoy"
            else -> null
        }
        val lotError = if (form.lot.length > MAX_LOT_LENGTH) "El lote tiene un máximo de $MAX_LOT_LENGTH caracteres" else null
        val placeError = if (form.place.length > MAX_PLACE_LENGTH) "El lugar tiene un máximo de $MAX_PLACE_LENGTH caracteres" else null
        if (dateError != null || lotError != null || placeError != null) {
            local.update { it.copy(form = it.form.copy(dateError = dateError, lotError = lotError, placeError = placeError)) }
            return null
        }
        return form
    }

    private fun editForm(change: DoseFormUi.() -> DoseFormUi) {
        local.update { it.copy(form = it.form.change(), error = null) }
    }

    private fun buildState(
        child: Child?,
        calendar: VaccinationCalendar,
        applied: List<AppliedDoseRecord>,
        online: Boolean,
        local: LocalState,
    ): VaccineDetailUiState {
        if (child == null) return VaccineDetailUiState(isLoaded = true, notFound = true)
        val today = clock.today()
        val record = applied.firstOrNull { it.doseCode == route.doseCode }
        val info = calendar.info(route.doseCode, child.birthDate)
        val withStatus = calendar.statusFor(child.birthDate, applied, today).doses.firstOrNull { it.dose.doseCode == route.doseCode }
        val former = calendar.former(route.doseCode)
        if (withStatus == null && record == null) return VaccineDetailUiState(isLoaded = true, notFound = true)

        return VaccineDetailUiState(
            isLoaded = true,
            vaccineName = info?.dose?.vaccineName ?: former?.vaccineName ?: route.doseCode,
            doseLabel = info?.dose?.doseLabel ?: former?.doseLabel.orEmpty(),
            status = withStatus?.status ?: DoseStatus.APPLIED,
            indicatedText = withStatus?.let { "${ageMonthsLabel(it.dose.ageMonths)} · ${it.indicatedDate.format(VaccinationDateFormatter)}" },
            prevents = info?.prevents,
            source = info?.source,
            applied = record?.toUi(),
            isEditing = local.isEditing && record != null,
            form = local.form,
            dateRequired = !(local.isEditing && record?.appliedOn == null),
            minDate = child.birthDate,
            maxDate = today,
            isOnline = online,
            isSaving = local.isSaving,
            confirmDelete = local.confirmDelete,
            error = local.error,
        )
    }

    private fun AppliedDoseRecord.toUi(): AppliedInfoUi {
        val isOwn = authorAccountId == authRepository.currentSession.value?.userId
        return AppliedInfoUi(
            dateText = appliedOn?.format(VaccinationDateFormatter) ?: "Fecha no registrada",
            lot = lot,
            place = place,
            authorText = when {
                isOwn -> "La registraste vos"
                authorRole == AuthorRole.PEDIATRICIAN -> "La registró el pediatra"
                else -> "La registró la familia"
            },
            isOwn = isOwn,
        )
    }
}

