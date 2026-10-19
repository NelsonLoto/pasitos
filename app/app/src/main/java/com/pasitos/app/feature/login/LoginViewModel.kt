package com.pasitos.app.feature.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitos.app.core.network.ApiException
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.auth.model.SessionState
import com.pasitos.shared.api.ErrorCode
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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            generalError = when (authRepository.sessionState.value) {
                SessionState.SignedOut(expired = true) -> "Tu sesión venció. Iniciá sesión de nuevo."
                else -> null
            },
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navigateToHome = Channel<Unit>(Channel.BUFFERED)
    val navigateToHome = _navigateToHome.receiveAsFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, generalError = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onSubmit() {
        val state = _uiState.value
        val emailError = validateEmail(state.email)
        val passwordError = validatePassword(state.password)
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            authRepository.login(state.email, state.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigateToHome.send(Unit)
                }
                .onFailure { error ->
                    if (error is ApiException && error.code == ErrorCode.EMAIL_NOT_VERIFIED) {
                        // No es un error de credenciales: el usuario tiene que verificar su email
                        // antes de poder entrar (RF-CTA-02, CA1). Si ya estaba en esta pantalla
                        // (vino de "Ya verifiqué"), el aviso es que sigue sin estar verificado.
                        val wasAlreadyPending = state.emailVerificationPending
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                emailVerificationPending = true,
                                verificationMessage = if (wasAlreadyPending) "Todavía no verificaste tu email. Revisá tu bandeja de entrada." else null,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, generalError = error.message ?: "No pudimos iniciar sesión. Probá de nuevo.")
                        }
                    }
                }
        }
    }

    /** RF-CTA-02, CA1: reenvía el email de verificación a la cuenta que intentó iniciar sesión. */
    fun onResendVerificationClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isResendingVerification = true, verificationMessage = null) }
            val email = _uiState.value.email
            authRepository.resendEmailVerification(email)
            _uiState.update {
                it.copy(isResendingVerification = false, verificationMessage = "Te volvimos a mandar el email de verificación.")
            }
        }
    }

    /** "Ya verifiqué": reintenta el login con las mismas credenciales, sin pedirlas de nuevo. */
    fun onAlreadyVerifiedClick() {
        _uiState.update { it.copy(verificationMessage = null) }
        onSubmit()
    }

    fun onBackFromVerificationClick() {
        _uiState.update { it.copy(emailVerificationPending = false, verificationMessage = null) }
    }

    fun onGoogleSignInClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            authRepository.loginWithGoogle()
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigateToHome.send(Unit)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, generalError = error.message ?: "No pudimos conectar con Google.")
                    }
                }
        }
    }

    private fun validateEmail(value: String): String? = when {
        value.isBlank() -> "Ingresá tu email"
        !Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches() -> "Ingresá un email válido"
        else -> null
    }

    private fun validatePassword(value: String): String? = when {
        value.isBlank() -> "Ingresá tu contraseña"
        value.length < 6 -> "Mínimo 6 caracteres"
        else -> null
    }
}
