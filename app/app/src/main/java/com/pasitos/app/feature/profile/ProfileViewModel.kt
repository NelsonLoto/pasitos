package com.pasitos.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitos.app.core.sync.SyncStatus
import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    syncStatus: SyncStatus,
    private val clock: ArgentinaClock,
) : ViewModel() {

    private val logout = MutableStateFlow(LogoutState())

    val uiState: StateFlow<ProfileUiState> = combine(
        authRepository.currentSession,
        logout,
        syncStatus.lastSyncedAt,
        syncStatus.pendingChanges,
    ) { session, logout, lastSyncedAt, pendingChanges ->
        ProfileUiState(
            displayName = session?.displayName.orEmpty(),
            email = session?.email.orEmpty(),
            avatarInitial = session?.displayName?.trim()?.firstOrNull()?.uppercase().orEmpty(),
            hasPassword = session?.hasPassword ?: true,
            lastSyncText = lastSyncText(lastSyncedAt, pendingChanges, clock.now()),
            pendingChangesToConfirm = logout.pendingChangesToConfirm,
            isLoggingOut = logout.inProgress,
            logoutError = logout.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun onLogoutClick() {
        if (logout.value.inProgress) return
        viewModelScope.launch {
            val pending = authRepository.pendingChanges()
            if (pending > 0) {
                logout.update { it.copy(pendingChangesToConfirm = pending, error = null) }
            } else {
                logOut()
            }
        }
    }

    fun onConfirmLogout() {
        logout.update { it.copy(pendingChangesToConfirm = null) }
        viewModelScope.launch { logOut() }
    }

    fun onDismissLogout() {
        logout.update { it.copy(pendingChangesToConfirm = null) }
    }

    /** Si sale bien, la app se reinicia y vuelve al login. */
    private suspend fun logOut() {
        logout.update { it.copy(inProgress = true, error = null) }
        authRepository.logout().onFailure { error ->
            logout.update {
                it.copy(inProgress = false, error = error.message ?: "No pudimos cerrar la sesión. Probá de nuevo.")
            }
        }
    }

    private data class LogoutState(
        val pendingChangesToConfirm: Int? = null,
        val inProgress: Boolean = false,
        val error: String? = null,
    )
}
