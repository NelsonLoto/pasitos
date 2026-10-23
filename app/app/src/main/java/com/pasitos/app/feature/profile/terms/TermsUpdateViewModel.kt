package com.pasitos.app.feature.profile.terms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitos.app.data.account.AccountRepository
import com.pasitos.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TermsUpdateUiState(
    /** RN-CTA-02: nunca marcada de entrada. */
    val acceptTerms: Boolean = false,
    val termsError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Cambios sin sincronizar que se perderían al cerrar sesión: si no es nulo, se pide confirmación. */
    val pendingChangesToConfirm: Int? = null,
)

/**
 * "Actualizamos los términos" (RN-CTA-02): aparece sobre todo lo demás mientras haya una versión
 * pendiente. La única alternativa a aceptarlos es cerrar sesión.
 */
@HiltViewModel
class TermsUpdateViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TermsUpdateUiState())
    val uiState: StateFlow<TermsUpdateUiState> = _uiState.asStateFlow()

    fun onAcceptTermsChange(value: Boolean) {
        _uiState.update { it.copy(acceptTerms = value, termsError = null, error = null) }
    }

    /** Al aceptarlos, `pendingTermsVersion` vuelve a nulo y la app sigue donde estaba. */
    fun onAcceptClick() {
        if (!_uiState.value.acceptTerms) {
            _uiState.update { it.copy(termsError = "Para seguir usando Pasitos tenés que aceptar los términos y la política de privacidad") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            accountRepository.acceptTerms()
                .onSuccess { reset() }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "No pudimos guardar la aceptación. Probá de nuevo.") }
                }
        }
    }

    /**
     * Vuelve al estado inicial, con la casilla sin marcar. Este ViewModel vive mientras viva la
     * Activity, no solo mientras se muestra la pantalla: si los términos vuelven a cambiar con la app
     * abierta, la casilla no puede aparecer marcada por la aceptación anterior (RN-CTA-02).
     */
    fun reset() {
        _uiState.value = TermsUpdateUiState()
    }

    fun onLogoutClick() {
        viewModelScope.launch {
            val pending = authRepository.pendingChanges()
            if (pending > 0) {
                _uiState.update { it.copy(pendingChangesToConfirm = pending) }
            } else {
                logOut()
            }
        }
    }

    fun onConfirmLogout() {
        _uiState.update { it.copy(pendingChangesToConfirm = null) }
        viewModelScope.launch { logOut() }
    }

    fun onDismissLogout() {
        _uiState.update { it.copy(pendingChangesToConfirm = null) }
    }

    /** Si sale bien, la app se reinicia y vuelve al login. */
    private suspend fun logOut() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.logout().onFailure { error ->
            _uiState.update { it.copy(isLoading = false, error = error.message ?: "No pudimos cerrar la sesión. Probá de nuevo.") }
        }
    }
}
