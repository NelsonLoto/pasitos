package com.pasitos.app.feature.guardians

import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pasitos.app.core.navigation.PsRoute
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.guardian.Guardian
import com.pasitos.app.data.guardian.GuardianRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Tutores de un niño: invitar, quitar, ceder el rol principal y dejar de ser tutor (RF-HIJ-05, 07,
 * 08). La lista se pide al servidor al abrir la pantalla y después de cada cambio.
 */
@HiltViewModel
class GuardiansViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val guardianRepository: GuardianRepository,
    private val childRepository: ChildRepository,
    authRepository: AuthRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    val childId: String = savedStateHandle.toRoute<PsRoute.Guardians>().childId

    private val form = MutableStateFlow(GuardiansUiState(myAccountId = authRepository.currentSession.value?.userId))

    val uiState: StateFlow<GuardiansUiState> = combine(
        form,
        childRepository.children.map { children -> children.firstOrNull { it.id == childId }?.firstName.orEmpty() },
        networkMonitor.isOnline,
    ) { form, childFirstName, online ->
        form.copy(childFirstName = childFirstName, isOnline = online)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    private val _left = Channel<Unit>(Channel.BUFFERED)

    /** Dejó de ser tutor: el niño ya no está en el teléfono y la pantalla se cierra. */
    val left = _left.receiveAsFlow()

    init {
        load()
        // Si abrió la pantalla sin conexión, la lista se pide sola al volver la red
        networkMonitor.isOnline
            .distinctUntilChanged()
            .drop(1)
            .filter { it && form.value.loadError != null }
            .onEach { load() }
            .launchIn(viewModelScope)
    }

    fun onRetryClick() = load()

    fun onInviteClick() {
        if (!uiState.value.canAct) return
        form.update { it.copy(dialog = GuardiansDialog.Invite, inviteEmail = "", dialogError = null, message = null) }
    }

    fun onInviteEmailChange(value: String) {
        form.update { it.copy(inviteEmail = value, dialogError = null) }
    }

    fun onSendInvite() {
        val email = form.value.inviteEmail.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            form.update { it.copy(dialogError = "Ingresá un email válido") }
            return
        }
        act(
            request = { guardianRepository.invite(childId, email) },
            reload = false,
            message = "Le mandamos la invitación a $email. Vence en 15 días.",
        )
    }

    fun onRemoveClick(guardian: Guardian) = open(GuardiansDialog.Remove(guardian))

    fun onConfirmRemove() {
        val guardian = (form.value.dialog as? GuardiansDialog.Remove)?.guardian ?: return
        act(
            request = { guardianRepository.remove(childId, guardian.accountId) },
            message = "${guardian.firstName} ya no es tutor de ${uiState.value.childFirstName}.",
        )
    }

    fun onTransferClick(guardian: Guardian) = open(GuardiansDialog.Transfer(guardian))

    fun onConfirmTransfer() {
        val guardian = (form.value.dialog as? GuardiansDialog.Transfer)?.guardian ?: return
        act(
            request = { guardianRepository.transferPrincipal(childId, guardian.accountId) },
            message = "${guardian.firstName} ahora es el tutor principal.",
        )
    }

    /**
     * Antes de confirmar pregunta al servidor si sigue habiendo otros tutores: si el principal se
     * fue mientras tanto, dejar de ser tutor eliminaría el perfil entero (RF-HIJ-09, CA1).
     */
    fun onLeaveClick() {
        if (!uiState.value.canAct) return
        viewModelScope.launch {
            form.update { it.copy(isWorking = true, message = null) }
            childRepository.isOnlyGuardian(childId)
                .onSuccess { onlyGuardian ->
                    if (onlyGuardian) {
                        form.update {
                            it.copy(
                                isWorking = false,
                                message = "Ahora sos el único tutor. Si querés eliminar el perfil, hacelo desde el perfil del niño o niña.",
                            )
                        }
                        load()
                    } else {
                        form.update { it.copy(isWorking = false, dialog = GuardiansDialog.Leave, dialogError = null) }
                    }
                }
                .onFailure { error -> form.update { it.copy(isWorking = false, message = error.message) } }
        }
    }

    fun onConfirmLeave() {
        if (form.value.dialog != GuardiansDialog.Leave || form.value.isWorking) return
        viewModelScope.launch {
            form.update { it.copy(isWorking = true, dialogError = null) }
            childRepository.leaveChild(childId)
                .onSuccess {
                    form.update { it.copy(isWorking = false, dialog = GuardiansDialog.None) }
                    _left.send(Unit)
                }
                .onFailure { error -> form.update { it.copy(isWorking = false, dialogError = error.message) } }
        }
    }

    fun onDismissDialog() {
        if (form.value.isWorking) return
        form.update { it.copy(dialog = GuardiansDialog.None, dialogError = null) }
    }

    private fun open(dialog: GuardiansDialog) {
        if (!uiState.value.canAct || !uiState.value.isPrincipal) return
        form.update { it.copy(dialog = dialog, dialogError = null, message = null) }
    }

    /** Envía la acción del diálogo abierto; si sale bien lo cierra, muestra [message] y vuelve a pedir la lista. */
    private fun act(request: suspend () -> Result<Unit>, message: String, reload: Boolean = true) {
        if (form.value.isWorking) return
        viewModelScope.launch {
            form.update { it.copy(isWorking = true, dialogError = null) }
            request()
                .onSuccess {
                    form.update { it.copy(isWorking = false, dialog = GuardiansDialog.None, message = message) }
                    if (reload) load()
                }
                .onFailure { error -> form.update { it.copy(isWorking = false, dialogError = error.message) } }
        }
    }

    private fun load() {
        viewModelScope.launch {
            form.update { it.copy(isLoading = true, loadError = null) }
            guardianRepository.guardians(childId)
                .onSuccess { guardians -> form.update { it.copy(isLoading = false, guardians = guardians) } }
                .onFailure { error -> form.update { it.copy(isLoading = false, loadError = error.message) } }
        }
    }
}
