package com.pasitos.app.feature.invitations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pasitos.app.core.navigation.PsRoute
import com.pasitos.app.core.network.ApiException
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.data.invitation.InvitationRepository
import com.pasitos.app.data.invitation.ReceivedInvitation
import com.pasitos.shared.api.ErrorCode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Aceptar, con la declaración, o rechazar una invitación de tutor (RF-HIJ-06). */
@HiltViewModel
class InvitationsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val invitationRepository: InvitationRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val form = MutableStateFlow(
        InvitationsUiState(afterRegistration = savedStateHandle.toRoute<PsRoute.Invitations>().afterRegistration),
    )

    val uiState: StateFlow<InvitationsUiState> = combine(
        form,
        invitationRepository.pending,
        networkMonitor.isOnline,
    ) { form, invitations, online ->
        form.copy(invitations = invitations, isOnline = online)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    init {
        load()
    }

    fun onRetryClick() = load()

    fun onAcceptClick(invitation: ReceivedInvitation) {
        if (!uiState.value.canAct) return
        form.update { it.copy(accepting = invitation, representationDeclared = false, dialogError = null, message = null) }
    }

    fun onRepresentationDeclaredChange(declared: Boolean) {
        form.update { it.copy(representationDeclared = declared, dialogError = null) }
    }

    fun onConfirmAccept() {
        val invitation = form.value.accepting ?: return
        if (!uiState.value.canConfirmAccept) return
        respond(
            request = { invitationRepository.accept(invitation.id, representationDeclared = true) },
            message = "Ya sos tutor de ${invitation.childFirstName}. Su perfil aparece en unos segundos.",
            accepted = true,
        )
    }

    fun onRejectClick(invitation: ReceivedInvitation) {
        if (!uiState.value.canAct) return
        form.update { it.copy(rejecting = invitation, dialogError = null, message = null) }
    }

    fun onConfirmReject() {
        val invitation = form.value.rejecting ?: return
        respond(
            request = { invitationRepository.reject(invitation.id) },
            message = "Rechazaste la invitación. Le avisamos a ${invitation.inviterFirstName}.",
            accepted = false,
        )
    }

    fun onDismissDialog() {
        if (form.value.isWorking) return
        form.update { it.copy(accepting = null, rejecting = null, dialogError = null) }
    }

    private fun respond(request: suspend () -> Result<Unit>, message: String, accepted: Boolean) {
        if (form.value.isWorking) return
        viewModelScope.launch {
            form.update { it.copy(isWorking = true, dialogError = null) }
            request()
                .onSuccess {
                    form.update {
                        it.copy(isWorking = false, accepting = null, rejecting = null, message = message, acceptedAny = it.acceptedAny || accepted)
                    }
                }
                .onFailure { error ->
                    if (error is ApiException && error.code == ErrorCode.INVITATION_NOT_FOUND) {
                        // Venció o ya no existe (RF-HIJ-06, CA3): el mensaje del servidor dice que pida una nueva
                        form.update { it.copy(isWorking = false, accepting = null, rejecting = null, message = error.message) }
                        invitationRepository.refresh()
                    } else {
                        form.update { it.copy(isWorking = false, dialogError = error.message) }
                    }
                }
        }
    }

    private fun load() {
        viewModelScope.launch {
            form.update { it.copy(isLoading = true, loadError = null) }
            invitationRepository.refresh()
                .onSuccess { form.update { it.copy(isLoading = false) } }
                .onFailure { error -> form.update { it.copy(isLoading = false, loadError = error.message) } }
        }
    }
}
