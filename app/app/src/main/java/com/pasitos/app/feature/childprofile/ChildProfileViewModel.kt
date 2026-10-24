package com.pasitos.app.feature.childprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pasitos.app.core.navigation.PsRoute
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.child.model.GuardianRole
import com.pasitos.app.data.child.model.ageLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Perfil de un hijo y su eliminación (RF-HIJ-02, 03, 09). La edición abre el formulario del alta. */
@HiltViewModel
class ChildProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val childRepository: ChildRepository,
    networkMonitor: NetworkMonitor,
    private val clock: ArgentinaClock,
) : ViewModel() {

    val childId: String = savedStateHandle.toRoute<PsRoute.ChildProfile>().childId

    /** El paso de la eliminación en curso: confirmación, nombre escrito, pedido y error. */
    private val deletion = MutableStateFlow(ChildProfileUiState())

    val uiState: StateFlow<ChildProfileUiState> = combine(
        childRepository.children.map { children -> children.firstOrNull { it.id == childId } },
        childRepository.observeRole(childId).onStart { emit(null) },
        networkMonitor.isOnline,
        deletion,
    ) { child, role, online, deletion ->
        deletion.copy(
            child = child,
            isLoaded = true,
            ageLabel = child?.ageLabel(clock.today()).orEmpty(),
            isPrincipal = role == GuardianRole.PRINCIPAL,
            isOnline = online,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChildProfileUiState())

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted = _deleted.receiveAsFlow()

    /** Pregunta al servidor si es el único tutor, porque la lista local podría estar desactualizada. */
    fun onDeleteClick() {
        if (deletion.value.isWorking || !uiState.value.isOnline) return
        viewModelScope.launch {
            deletion.update { it.copy(isWorking = true, error = null) }
            childRepository.isOnlyGuardian(childId)
                .onSuccess { onlyGuardian ->
                    val step = if (onlyGuardian) DeletionStep.ConfirmByName else DeletionStep.ConfirmLeave
                    deletion.update { it.copy(isWorking = false, deletion = step, typedName = "") }
                }
                .onFailure { error -> deletion.update { it.copy(isWorking = false, error = error.message) } }
        }
    }

    fun onTypedNameChange(value: String) {
        deletion.update { it.copy(typedName = value) }
    }

    fun onDismissDeletion() {
        deletion.update { it.copy(deletion = DeletionStep.None, typedName = "", error = null) }
    }

    fun onConfirmDeletion() {
        if (!uiState.value.canConfirmDeletion) return
        val leaving = deletion.value.deletion == DeletionStep.ConfirmLeave
        viewModelScope.launch {
            deletion.update { it.copy(isWorking = true, error = null) }
            // Con otros tutores, dejar de serlo avisa al principal (RF-HIJ-07, CA2)
            val result = if (leaving) childRepository.leaveChild(childId) else childRepository.deleteChild(childId)
            result
                .onSuccess {
                    deletion.update { it.copy(isWorking = false, deletion = DeletionStep.None) }
                    _deleted.send(Unit)
                }
                .onFailure { error -> deletion.update { it.copy(isWorking = false, error = error.message) } }
        }
    }
}
