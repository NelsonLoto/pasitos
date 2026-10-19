package com.pasitos.app.feature.previousdoses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pasitos.app.core.navigation.PsRoute
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.vaccination.VaccinationRepository
import com.pasitos.app.data.vaccination.model.ageMonthsLabel
import dagger.hilt.android.lifecycle.HiltViewModel
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

private data class LocalState(
    val step: PreviousDosesStep = PreviousDosesStep.Question,
    val checked: Set<String> = emptySet(),
    val isSaving: Boolean = false,
    val savedCount: Int = 0,
    val error: String? = null,
)

/**
 * Carga retroactiva (RF-VAC-04): después del alta de un hijo (RF-HIJ-01, CA2) o desde "Marcar dosis
 * anteriores" en Vacunas (CA4). Requiere conexión.
 */
@HiltViewModel
class PreviousDosesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    childRepository: ChildRepository,
    private val vaccinationRepository: VaccinationRepository,
    networkMonitor: NetworkMonitor,
    private val clock: ArgentinaClock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<PsRoute.PreviousDoses>()
    private val local = MutableStateFlow(LocalState())

    val uiState: StateFlow<PreviousDosesUiState> = combine(
        childRepository.children.map { children -> children.firstOrNull { it.id == route.childId } },
        vaccinationRepository.calendar,
        vaccinationRepository.observeAppliedDoses(route.childId),
        networkMonitor.isOnline,
        local,
    ) { child, calendar, applied, online, local ->
        val doses = if (child == null) {
            emptyList()
        } else {
            calendar.previousDoses(child.birthDate, applied.mapTo(mutableSetOf()) { it.doseCode }, clock.today())
        }
        PreviousDosesUiState(
            isLoaded = true,
            childName = child?.firstName.orEmpty(),
            groups = doses.groupBy { it.ageMonths }.toSortedMap().map { (ageMonths, group) ->
                PreviousDoseGroupUi(
                    label = ageMonthsLabel(ageMonths),
                    doses = group.map { PreviousDoseUi(it.doseCode, it.vaccineName, it.doseLabel) },
                )
            },
            step = local.step,
            // Si otro teléfono registra una dosis mientras tanto, deja de estar en la lista
            checked = local.checked.filterTo(mutableSetOf()) { code -> doses.any { it.doseCode == code } },
            afterChildCreated = route.afterChildCreated,
            isOnline = online,
            isSaving = local.isSaving,
            savedCount = local.savedCount,
            error = local.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PreviousDosesUiState(afterChildCreated = route.afterChildCreated))

    private val _finished = Channel<Unit>(Channel.BUFFERED)

    /** Terminó o se salteó: después del alta va a Inicio; desde Vacunas, vuelve. */
    val finished = _finished.receiveAsFlow()

    init {
        // Justo después de registrarse, el calendario puede no haberse bajado todavía
        viewModelScope.launch {
            if (vaccinationRepository.calendar.value.isEmpty) vaccinationRepository.refreshCalendar()
        }
    }

    /** "Sí" marca todas; "No" y "No sé", ninguna (RF-VAC-04, CA1 y CA2). */
    fun onAnswer(upToDate: Boolean) {
        val all = uiState.value.groups.flatMap { group -> group.doses.map { it.doseCode } }.toSet()
        local.update { it.copy(step = PreviousDosesStep.Checklist, checked = if (upToDate) all else emptySet(), error = null) }
    }

    fun onToggle(doseCode: String, checked: Boolean) {
        local.update { it.copy(checked = if (checked) it.checked + doseCode else it.checked - doseCode, error = null) }
    }

    fun onSave() {
        val state = uiState.value
        if (!state.canSave) return
        val codes = state.groups.flatMap { group -> group.doses.map { it.doseCode } }.filter { it in state.checked }
        viewModelScope.launch {
            local.update { it.copy(isSaving = true, error = null) }
            vaccinationRepository.registerPreviousDoses(route.childId, codes)
                .onSuccess { created ->
                    local.update { it.copy(isSaving = false, step = PreviousDosesStep.Done, savedCount = created.size, checked = emptySet()) }
                }
                .onFailure { error ->
                    local.update { it.copy(isSaving = false, error = error.message ?: "No pudimos guardar las dosis. Probá de nuevo.") }
                }
        }
    }

    fun onFinish() {
        viewModelScope.launch { _finished.send(Unit) }
    }
}
