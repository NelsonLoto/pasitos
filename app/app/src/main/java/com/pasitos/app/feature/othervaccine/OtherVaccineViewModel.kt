package com.pasitos.app.feature.othervaccine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pasitos.app.core.navigation.PsRoute
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.vaccination.VaccinationRepository
import com.pasitos.app.data.vaccination.model.OtherVaccine
import com.pasitos.app.data.vaccination.model.OtherVaccineInput
import com.pasitos.shared.api.AuthorRole
import com.pasitos.shared.api.vaccination.MAX_LOT_LENGTH
import com.pasitos.shared.api.vaccination.MAX_OTHER_VACCINE_NAME_LENGTH
import com.pasitos.shared.api.vaccination.MAX_OTHER_VACCINE_NOTE_LENGTH
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lo que el usuario está editando, más el guardado y la confirmación. */
private data class LocalState(
    val name: String = "",
    val date: LocalDate? = null,
    val lot: String = "",
    val note: String = "",
    val nameError: String? = null,
    val dateError: String? = null,
    val lotError: String? = null,
    val noteError: String? = null,
    val isSaving: Boolean = false,
    val confirmDelete: Boolean = false,
    val error: String? = null,
)

/** Alta, edición y eliminación de otra vacuna (RF-VAC-05). Solo funcionan en línea. */
@HiltViewModel
class OtherVaccineViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    childRepository: ChildRepository,
    private val vaccinationRepository: VaccinationRepository,
    private val authRepository: AuthRepository,
    networkMonitor: NetworkMonitor,
    private val clock: ArgentinaClock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<PsRoute.OtherVaccine>()
    private val local = MutableStateFlow(LocalState())

    private val existing = vaccinationRepository.observeOtherVaccines(route.childId)
        .map { vaccines -> vaccines.firstOrNull { it.id == route.vaccineId } }

    val uiState: StateFlow<OtherVaccineUiState> = combine(
        childRepository.children.map { children -> children.firstOrNull { it.id == route.childId } },
        existing,
        networkMonitor.isOnline,
        local,
    ) { child, vaccine, online, local ->
        val isNew = route.vaccineId == null
        val isOwn = isNew || vaccine?.authorAccountId == authRepository.currentSession.value?.userId
        OtherVaccineUiState(
            isLoaded = true,
            notFound = child == null || (!isNew && vaccine == null && !local.isSaving),
            isNew = isNew,
            isOwn = isOwn,
            authorText = vaccine?.let { authorText(it, isOwn) },
            name = local.name,
            date = local.date,
            lot = local.lot,
            note = local.note,
            nameError = local.nameError,
            dateError = local.dateError,
            lotError = local.lotError,
            noteError = local.noteError,
            minDate = child?.birthDate,
            maxDate = clock.today(),
            isOnline = online,
            isSaving = local.isSaving,
            confirmDelete = local.confirmDelete,
            error = local.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OtherVaccineUiState())

    private val _done = Channel<Unit>(Channel.BUFFERED)

    /** Se guardó o se eliminó: la pantalla vuelve a Vacunas. */
    val done = _done.receiveAsFlow()

    init {
        // Una vacuna existente abre con sus datos
        if (route.vaccineId != null) {
            viewModelScope.launch {
                existing.first()?.let { vaccine ->
                    local.update {
                        it.copy(name = vaccine.name, date = vaccine.appliedOn, lot = vaccine.lot.orEmpty(), note = vaccine.note.orEmpty())
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = local.update { it.copy(name = value, nameError = null, error = null) }

    fun onSuggestion(value: String) = onNameChange(value)

    fun onDateSelected(date: LocalDate) = local.update { it.copy(date = date, dateError = null, error = null) }

    fun onLotChange(value: String) = local.update { it.copy(lot = value, lotError = null, error = null) }

    fun onNoteChange(value: String) = local.update { it.copy(note = value, noteError = null, error = null) }

    fun onSave() {
        val state = uiState.value
        if (state.isReadOnly || state.isSaving || !state.isOnline) return
        val input = validate(state) ?: return
        viewModelScope.launch {
            local.update { it.copy(isSaving = true, error = null) }
            vaccinationRepository.saveOtherVaccine(route.childId, route.vaccineId, input)
                .onSuccess {
                    local.update { it.copy(isSaving = false) }
                    _done.send(Unit)
                }
                .onFailure(::showError)
        }
    }

    fun onDeleteClick() {
        val state = uiState.value
        if (!state.isNew && state.isOwn && state.isOnline) local.update { it.copy(confirmDelete = true, error = null) }
    }

    fun onDismissDelete() = local.update { it.copy(confirmDelete = false) }

    fun onConfirmDelete() {
        val vaccineId = route.vaccineId ?: return
        viewModelScope.launch {
            local.update { it.copy(isSaving = true, confirmDelete = false, error = null) }
            vaccinationRepository.deleteOtherVaccine(vaccineId)
                .onSuccess { _done.send(Unit) }
                .onFailure(::showError)
        }
    }

    private fun showError(error: Throwable) {
        local.update { it.copy(isSaving = false, error = error.message ?: "No pudimos guardar la vacuna. Probá de nuevo.") }
    }

    /** RN-VAC-02 y los límites de SUP-24. */
    private fun validate(state: OtherVaccineUiState): OtherVaccineInput? {
        val name = state.name.trim()
        val lot = state.lot.trim()
        val note = state.note.trim()
        val date = state.date
        val nameError = when {
            name.isEmpty() -> "Completá el nombre de la vacuna"
            name.length > MAX_OTHER_VACCINE_NAME_LENGTH -> "El nombre tiene un máximo de $MAX_OTHER_VACCINE_NAME_LENGTH caracteres"
            else -> null
        }
        val dateError = when {
            date == null -> "Elegí la fecha de aplicación"
            state.minDate != null && date.isBefore(state.minDate) -> "La fecha no puede ser anterior al nacimiento"
            date.isAfter(state.maxDate) -> "La fecha no puede ser posterior a hoy"
            else -> null
        }
        val lotError = if (lot.length > MAX_LOT_LENGTH) "El lote tiene un máximo de $MAX_LOT_LENGTH caracteres" else null
        val noteError = if (note.length > MAX_OTHER_VACCINE_NOTE_LENGTH) "La nota tiene un máximo de $MAX_OTHER_VACCINE_NOTE_LENGTH caracteres" else null
        if (nameError != null || dateError != null || lotError != null || noteError != null || date == null) {
            local.update { it.copy(nameError = nameError, dateError = dateError, lotError = lotError, noteError = noteError) }
            return null
        }
        return OtherVaccineInput(name = name, appliedOn = date, lot = lot, note = note)
    }

    private fun authorText(vaccine: OtherVaccine, isOwn: Boolean): String = when {
        isOwn -> "La registraste vos"
        vaccine.authorRole == AuthorRole.PEDIATRICIAN -> "La registró el pediatra"
        else -> "La registró la familia"
    }
}
