package com.pasitos.app.feature.emailverification

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pasitos.app.core.navigation.PsRoute
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.invitation.InvitationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * "Verificá tu email" después de registrarse (RF-CTA-01 CA1, RF-CTA-02): la sesión ya está
 * abierta, así que "Ya verifiqué" consulta `GET /me` en vez de pedir las credenciales de nuevo.
 */
@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val invitationRepository: InvitationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<PsRoute.EmailVerification>()

    private val _uiState = MutableStateFlow(EmailVerificationUiState(email = route.email))
    val uiState: StateFlow<EmailVerificationUiState> = _uiState.asStateFlow()

    private val _navigateNext = Channel<Boolean>(Channel.BUFFERED)

    /**
     * Verificado. Lleva `true` si tiene invitaciones de tutor: lo invitaron con este email antes de
     * registrarse y las ve primero (RF-HIJ-05, CA2). Si no se pudieron consultar, sigue como sin ellas.
     */
    val navigateNext = _navigateNext.receiveAsFlow()

    val afterVerifiedGoHome: Boolean get() = route.afterVerifiedGoHome

    fun onResendClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isResending = true, message = null) }
            authRepository.resendEmailVerification(route.email)
            _uiState.update { it.copy(isResending = false, message = "Te volvimos a mandar el email de verificación.") }
        }
    }

    fun onAlreadyVerifiedClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, message = null) }
            authRepository.isEmailVerified()
                .onSuccess { verified ->
                    if (verified) {
                        val hasInvitations = invitationRepository.refresh().isSuccess && invitationRepository.pending.value.isNotEmpty()
                        _uiState.update { it.copy(isChecking = false) }
                        _navigateNext.send(hasInvitations)
                    } else {
                        _uiState.update {
                            it.copy(isChecking = false, message = "Todavía no verificaste tu email. Revisá tu bandeja de entrada.")
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isChecking = false, message = error.message ?: "No pudimos comprobarlo. Probá de nuevo.") }
                }
        }
    }
}
