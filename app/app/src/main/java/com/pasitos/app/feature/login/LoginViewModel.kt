package com.pasitos.app.feature.login

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitos.app.core.network.ApiException
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.auth.google.GoogleCredentialResult
import com.pasitos.app.data.auth.google.GoogleCredentials
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
    private val googleCredentials: GoogleCredentials,
) : ViewModel() {

    /**
     * ID token de Google entre el 409 `terms_required` y "Crear cuenta" (RF-CTA-03, CA1). Permite
     * iniciar sesión como esa persona durante una hora, así que vive solo acá, en memoria: no va en
     * el estado, ni en el `SavedStateHandle`, ni en los argumentos de navegación.
     */
    private var pendingGoogleToken: String? = null

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

    /** RF-CTA-03. [activityContext] solo se usa para mostrar el diálogo de Google y no se retiene. */
    fun onGoogleSignInClick(activityContext: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            when (val result = googleCredentials.request(activityContext)) {
                // RF-CTA-03, CA3: cerrar el diálogo no es un error
                GoogleCredentialResult.Cancelled -> _uiState.update { it.copy(isLoading = false) }
                is GoogleCredentialResult.Failure -> _uiState.update { it.copy(isLoading = false, generalError = result.message) }
                is GoogleCredentialResult.Success -> signInWithGoogle(result)
            }
        }
    }

    private suspend fun signInWithGoogle(credential: GoogleCredentialResult.Success) {
        authRepository.loginWithGoogle(credential.idToken)
            .onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                _navigateToHome.send(Unit)
            }
            .onFailure { error ->
                if (error is ApiException && error.code == ErrorCode.TERMS_REQUIRED) {
                    // RF-CTA-03, CA1: el backend no guardó nada; falta aceptar los términos
                    pendingGoogleToken = credential.idToken
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            googleSignUp = GoogleSignUpUiState(
                                email = credential.email,
                                firstName = credential.givenName.orEmpty(),
                                lastName = credential.familyName.orEmpty(),
                            ),
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, generalError = error.message ?: GOOGLE_ERROR) }
                }
            }
    }

    fun onGoogleFirstNameChange(value: String) =
        updateGoogleSignUp { it.copy(firstName = value, firstNameError = null) }

    fun onGoogleLastNameChange(value: String) =
        updateGoogleSignUp { it.copy(lastName = value, lastNameError = null) }

    fun onGoogleAcceptTermsChange(value: Boolean) =
        updateGoogleSignUp { it.copy(acceptTerms = value, termsError = null) }

    /** RF-CTA-03, CA1: crea la cuenta con el mismo token, ahora con los términos aceptados. */
    fun onGoogleSignUpSubmit() {
        val signUp = _uiState.value.googleSignUp ?: return
        val idToken = pendingGoogleToken ?: return
        val validated = signUp.copy(
            firstNameError = if (signUp.firstName.isBlank()) "Ingresá tu nombre" else null,
            lastNameError = if (signUp.lastName.isBlank()) "Ingresá tu apellido" else null,
            termsError = if (signUp.acceptTerms) null else TERMS_ERROR,
        )
        if (validated.firstNameError != null || validated.lastNameError != null || validated.termsError != null) {
            _uiState.update { it.copy(googleSignUp = validated) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            authRepository.loginWithGoogle(idToken, acceptTerms = true, firstName = signUp.firstName, lastName = signUp.lastName)
                .onSuccess {
                    pendingGoogleToken = null
                    _uiState.update { it.copy(isLoading = false, googleSignUp = null) }
                    _navigateToHome.send(Unit)
                }
                .onFailure { error ->
                    if (error is ApiException && error.code == ErrorCode.INVALID_GOOGLE_TOKEN) {
                        // El token de Google dura una hora: si venció, hay que pedir otro
                        pendingGoogleToken = null
                        _uiState.update {
                            it.copy(isLoading = false, googleSignUp = null, generalError = "Pasó mucho tiempo. Tocá \"Continuar con Google\" de nuevo.")
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, generalError = error.message ?: GOOGLE_ERROR) }
                    }
                }
        }
    }

    /** Vuelve al login sin crear la cuenta; el token se descarta. */
    fun onGoogleSignUpCancel() {
        pendingGoogleToken = null
        _uiState.update { it.copy(googleSignUp = null, generalError = null) }
    }

    private fun updateGoogleSignUp(transform: (GoogleSignUpUiState) -> GoogleSignUpUiState) {
        _uiState.update { state -> state.copy(googleSignUp = state.googleSignUp?.let(transform), generalError = null) }
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

    private companion object {
        const val GOOGLE_ERROR = "No pudimos conectar con Google. Probá de nuevo."
        const val TERMS_ERROR = "Para crear la cuenta tenés que aceptar los términos y la política de privacidad"
    }
}
