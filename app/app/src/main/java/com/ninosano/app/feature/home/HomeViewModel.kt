package com.ninosano.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ninosano.app.data.auth.AuthRepository
import com.ninosano.app.data.child.ChildRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    authRepository: AuthRepository,
    childRepository: ChildRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        authRepository.currentSession,
        childRepository.children,
        childRepository.activeChildId,
    ) { session, children, activeChildId ->
        HomeUiState(
            greetingName = session?.displayName?.substringBefore(" ").orEmpty(),
            activeChild = children.firstOrNull { it.id == activeChildId },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
