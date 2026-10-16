package com.pasitos.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitos.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = authRepository.currentSession
        .map { session ->
            ProfileUiState(
                displayName = session?.displayName.orEmpty(),
                email = session?.email.orEmpty(),
                avatarInitial = session?.displayName?.trim()?.firstOrNull()?.uppercase().orEmpty(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun onLogoutClick() {
        authRepository.logout()
    }
}
