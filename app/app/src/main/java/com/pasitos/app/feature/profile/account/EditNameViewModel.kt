package com.pasitos.app.feature.profile.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.data.account.AccountRepository
import com.pasitos.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditNameUiState(
    val firstName: String = "",
    val lastName: String = "",
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val isOnline: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val canSave: Boolean get() = isOnline && !isSaving
}

/** RF-CTA-11: nombre y apellido, que se guardan al instante (`PATCH /me`). */
@HiltViewModel
class EditNameViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    authRepository: AuthRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val form = MutableStateFlow(
        authRepository.currentSession.value.let { EditNameUiState(firstName = it?.firstName.orEmpty(), lastName = it?.lastName.orEmpty()) },
    )

    val uiState: StateFlow<EditNameUiState> = combine(form, networkMonitor.isOnline) { form, online -> form.copy(isOnline = online) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    fun onFirstNameChange(value: String) = form.update { it.copy(firstName = value, firstNameError = null, error = null) }

    fun onLastNameChange(value: String) = form.update { it.copy(lastName = value, lastNameError = null, error = null) }

    fun onSave() {
        val state = form.value
        val validated = state.copy(
            firstNameError = if (state.firstName.isBlank()) "Ingresá tu nombre" else null,
            lastNameError = if (state.lastName.isBlank()) "Ingresá tu apellido" else null,
        )
        if (validated.firstNameError != null || validated.lastNameError != null) {
            form.value = validated
            return
        }
        viewModelScope.launch {
            form.update { it.copy(isSaving = true, error = null) }
            accountRepository.updateName(state.firstName, state.lastName)
                .onSuccess {
                    form.update { it.copy(isSaving = false) }
                    _saved.send(Unit)
                }
                .onFailure { error ->
                    form.update { it.copy(isSaving = false, error = error.message ?: "No pudimos guardar el cambio. Probá de nuevo.") }
                }
        }
    }
}
