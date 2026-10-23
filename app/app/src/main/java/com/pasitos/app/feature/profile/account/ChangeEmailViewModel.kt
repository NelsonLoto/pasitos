package com.pasitos.app.feature.profile.account

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitos.app.core.network.ApiException
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.data.account.AccountRepository
import com.pasitos.app.data.auth.AuthRepository
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

data class ChangeEmailUiState(
    val currentEmail: String = "",
    /** Sin contraseña no se puede pedir el cambio: primero hay que crearla (RF-CTA-11, CA4). */
    val hasPassword: Boolean = true,
    val newEmail: String = "",
    val password: String = "",
    val newEmailError: String? = null,
    val passwordError: String? = null,
    val isOnline: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null,
    /** Email al que se mandó la confirmación: desde ahí se muestra "Revisá tu email". */
    val sentTo: String? = null,
) {
    val canSend: Boolean get() = isOnline && !isSending
}

/**
 * RF-CTA-11, CA1: pide la contraseña actual y manda un link al email nuevo. El email no cambia hasta
 * que se abre ese link; el perfil lo muestra actualizado la próxima vez que la app consulta `/me`.
 */
@HiltViewModel
class ChangeEmailViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    authRepository: AuthRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val form = MutableStateFlow(
        authRepository.currentSession.value.let {
            ChangeEmailUiState(currentEmail = it?.email.orEmpty(), hasPassword = it?.hasPassword ?: true)
        },
    )

    val uiState: StateFlow<ChangeEmailUiState> = combine(form, networkMonitor.isOnline) { form, online -> form.copy(isOnline = online) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    fun onNewEmailChange(value: String) = form.update { it.copy(newEmail = value, newEmailError = null, error = null) }

    fun onPasswordChange(value: String) = form.update { it.copy(password = value, passwordError = null, error = null) }

    fun onSend() {
        val state = form.value
        val newEmail = state.newEmail.trim()
        val validated = state.copy(
            newEmailError = when {
                newEmail.isBlank() -> "Ingresá el email nuevo"
                !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches() -> "Ingresá un email válido"
                newEmail.equals(state.currentEmail, ignoreCase = true) -> "Ese ya es tu email actual"
                else -> null
            },
            passwordError = if (state.password.isEmpty()) "Ingresá tu contraseña actual" else null,
        )
        if (validated.newEmailError != null || validated.passwordError != null) {
            form.value = validated
            return
        }
        viewModelScope.launch {
            form.update { it.copy(isSending = true, error = null) }
            accountRepository.requestEmailChange(newEmail, state.password)
                .onSuccess { form.update { it.copy(isSending = false, password = "", sentTo = newEmail) } }
                .onFailure { error -> form.update { it.copy(isSending = false).withError(error) } }
        }
    }

    private fun ChangeEmailUiState.withError(error: Throwable): ChangeEmailUiState {
        val message = error.message ?: "No pudimos pedir el cambio. Probá de nuevo."
        return when ((error as? ApiException)?.code) {
            ErrorCode.INVALID_CREDENTIALS -> copy(passwordError = message)
            ErrorCode.EMAIL_ALREADY_IN_USE, ErrorCode.INVALID_INPUT -> copy(newEmailError = message)
            else -> copy(error = message)
        }
    }
}
