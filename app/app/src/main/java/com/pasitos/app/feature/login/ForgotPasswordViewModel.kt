package com.pasitos.app.feature.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitos.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** "Olvidé mi contraseña" (RF-CTA-05, CA1). */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onSubmit() {
        val email = _uiState.value.email
        val emailError = validateEmail(email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            authRepository.requestPasswordReset(email)
                // RF-CTA-05, CA1: la respuesta es la misma exista o no la cuenta. Un fallo acá es
                // un problema real (sin conexión, servidor caído), no que el email no exista.
                .onSuccess { _uiState.update { it.copy(isLoading = false, requestSent = true) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, generalError = error.message ?: "No pudimos pedir la recuperación. Probá de nuevo.")
                    }
                }
        }
    }

    private fun validateEmail(value: String): String? = when {
        value.isBlank() -> "Ingresá tu email"
        !Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches() -> "Ingresá un email válido"
        else -> null
    }
}
