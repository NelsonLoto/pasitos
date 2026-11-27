package com.pasitos.app.feature.register

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitos.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _navigateToHome = Channel<Unit>(Channel.BUFFERED)
    val navigateToHome = _navigateToHome.receiveAsFlow()

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, fullNameError = null, generalError = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, confirmPasswordError = null, generalError = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null, generalError = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.acceptTerms) {
            _uiState.update { it.copy(generalError = "Aceptá los términos y la política de privacidad para continuar") }
            return
        }
        val fullNameError = validateFullName(state.fullName)
        val emailError = validateEmail(state.email)
        val passwordError = validatePassword(state.password)
        val confirmPasswordError = validateConfirmPassword(state.password, state.confirmPassword)
        if (fullNameError != null || emailError != null || passwordError != null || confirmPasswordError != null) {
            _uiState.update {
                it.copy(
                    fullNameError = fullNameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            authRepository.register(state.fullName, state.email, state.password, state.acceptTerms)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigateToHome.send(Unit)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, generalError = error.message ?: "No pudimos crear la cuenta. Probá de nuevo.")
                    }
                }
        }
    }

    private fun validateFullName(value: String): String? = when {
        value.isBlank() -> "Ingresá tu nombre completo"
        value.trim().split(Regex("\\s+"), limit = 2).size < 2 -> "Ingresá tu nombre y apellido"
        else -> null
    }

    private fun validateEmail(value: String): String? = when {
        value.isBlank() -> "Ingresá tu email"
        !Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches() -> "Ingresá un email válido"
        else -> null
    }

    private fun validatePassword(value: String): String? = when {
        value.isBlank() -> "Ingresá una contraseña"
        value.length < 8 || !value.any(Char::isLetter) || !value.any(Char::isDigit) -> "Usá al menos 8 caracteres, una letra y un número"
        else -> null
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): String? = when {
        confirmPassword.isBlank() -> "Confirmá tu contraseña"
        confirmPassword != password -> "Las contraseñas no coinciden"
        else -> null
    }

    fun onTermsChange(accepted: Boolean) { _uiState.update { it.copy(acceptTerms = accepted, generalError = null) } }
}
