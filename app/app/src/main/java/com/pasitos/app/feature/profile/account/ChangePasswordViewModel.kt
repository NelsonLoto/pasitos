package com.pasitos.app.feature.profile.account

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

enum class PasswordResult {
    /** RF-CTA-11, CA2: las demás sesiones se cerraron; esta sigue. */
    CHANGED,

    /** RF-CTA-11, CA4: se mandó el email para crearla. */
    CREATION_EMAIL_SENT,
}

data class ChangePasswordUiState(
    val email: String = "",
    /** Falso: la pantalla ofrece "Crear una contraseña" por email (RF-CTA-11, CA4). */
    val hasPassword: Boolean = true,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val isOnline: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val result: PasswordResult? = null,
) {
    val canSave: Boolean get() = isOnline && !isSaving
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    authRepository: AuthRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val form = MutableStateFlow(
        authRepository.currentSession.value.let {
            ChangePasswordUiState(email = it?.email.orEmpty(), hasPassword = it?.hasPassword ?: true)
        },
    )

    val uiState: StateFlow<ChangePasswordUiState> = combine(form, networkMonitor.isOnline) { form, online -> form.copy(isOnline = online) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    fun onCurrentPasswordChange(value: String) =
        form.update { it.copy(currentPassword = value, currentPasswordError = null, error = null) }

    fun onNewPasswordChange(value: String) =
        form.update { it.copy(newPassword = value, newPasswordError = null, error = null) }

    fun onConfirmPasswordChange(value: String) =
        form.update { it.copy(confirmPassword = value, confirmPasswordError = null, error = null) }

    /** RF-CTA-11, CA2: pide la actual, y la nueva cumple RN-CTA-01. */
    fun onChange() {
        val state = form.value
        val validated = state.copy(
            currentPasswordError = if (state.currentPassword.isEmpty()) "Ingresá tu contraseña actual" else null,
            newPasswordError = validateNewPassword(state.newPassword),
            confirmPasswordError = if (state.confirmPassword != state.newPassword) "Las contraseñas no coinciden" else null,
        )
        if (validated.currentPasswordError != null || validated.newPasswordError != null || validated.confirmPasswordError != null) {
            form.value = validated
            return
        }
        viewModelScope.launch {
            form.update { it.copy(isSaving = true, error = null) }
            accountRepository.changePassword(state.currentPassword, state.newPassword)
                .onSuccess {
                    form.update {
                        it.copy(isSaving = false, currentPassword = "", newPassword = "", confirmPassword = "", result = PasswordResult.CHANGED)
                    }
                }
                .onFailure { error ->
                    val message = error.message ?: "No pudimos cambiar la contraseña. Probá de nuevo."
                    form.update {
                        when ((error as? ApiException)?.code) {
                            ErrorCode.INVALID_CREDENTIALS -> it.copy(isSaving = false, currentPasswordError = message)
                            ErrorCode.INVALID_INPUT -> it.copy(isSaving = false, newPasswordError = message)
                            else -> it.copy(isSaving = false, error = message)
                        }
                    }
                }
        }
    }

    /** RF-CTA-11, CA4: el link del email demuestra que la persona controla la cuenta. */
    fun onSendCreationEmail() {
        viewModelScope.launch {
            form.update { it.copy(isSaving = true, error = null) }
            accountRepository.requestPasswordCreation()
                .onSuccess { form.update { it.copy(isSaving = false, result = PasswordResult.CREATION_EMAIL_SENT) } }
                .onFailure { error ->
                    form.update { it.copy(isSaving = false, error = error.message ?: "No pudimos mandarte el email. Probá de nuevo.") }
                }
        }
    }

    /** Las mismas reglas que el registro (RN-CTA-01). */
    private fun validateNewPassword(value: String): String? = when {
        value.isBlank() -> "Ingresá la contraseña nueva"
        value.length < 8 || value.none { it.isLetter() } || value.none { it.isDigit() } ->
            "Tiene que tener al menos 8 caracteres, con letras y números"
        else -> null
    }
}
