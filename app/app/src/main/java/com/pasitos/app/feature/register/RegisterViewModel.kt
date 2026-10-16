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

    fun onFirstNameChange(value: String) {
        _uiState.update { it.copy(firstName = value, firstNameError = null, generalError = null) }
    }

    fun onLastNameChange(value: String) {
        _uiState.update { it.copy(lastName = value, lastNameError = null, generalError = null) }
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

    fun onAcceptTermsChange(value: Boolean) {
        _uiState.update { it.copy(acceptTerms = value, termsError = null, generalError = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        val validated = state.copy(
            firstNameError = if (state.firstName.isBlank()) "Ingresá tu nombre" else null,
            lastNameError = if (state.lastName.isBlank()) "Ingresá tu apellido" else null,
            emailError = validateEmail(state.email),
            passwordError = validatePassword(state.password),
            confirmPasswordError = validateConfirmPassword(state.password, state.confirmPassword),
            termsError = if (state.acceptTerms) null else "Para crear la cuenta tenés que aceptar los términos y la política de privacidad",
        )
        val hasErrors = listOfNotNull(
            validated.firstNameError,
            validated.lastNameError,
            validated.emailError,
            validated.passwordError,
            validated.confirmPasswordError,
            validated.termsError,
        ).isNotEmpty()
        if (hasErrors) {
            _uiState.value = validated
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            authRepository.register(state.firstName, state.lastName, state.email, state.password, state.acceptTerms)
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

    private fun validateEmail(value: String): String? = when {
        value.isBlank() -> "Ingresá tu email"
        !Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches() -> "Ingresá un email válido"
        else -> null
    }

    /** RN-CTA-01. La validación en vivo llega con I1-A1. */
    private fun validatePassword(value: String): String? = when {
        value.isBlank() -> "Ingresá una contraseña"
        value.length < 8 || value.none { it.isLetter() } || value.none { it.isDigit() } ->
            "Tiene que tener al menos 8 caracteres, con letras y números"
        else -> null
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): String? = when {
        confirmPassword.isBlank() -> "Confirmá tu contraseña"
        confirmPassword != password -> "Las contraseñas no coinciden"
        else -> null
    }
}
