package com.ninosano.app.feature.childselector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ninosano.app.data.child.ChildRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ChildSelectorViewModel @Inject constructor(
    private val childRepository: ChildRepository,
) : ViewModel() {

    val uiState: StateFlow<ChildSelectorUiState> = combine(
        childRepository.children,
        childRepository.activeChildId,
    ) { children, activeChildId ->
        ChildSelectorUiState(children = children, activeChildId = activeChildId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChildSelectorUiState())

    private val _navigateBack = Channel<Unit>(Channel.BUFFERED)
    val navigateBack = _navigateBack.receiveAsFlow()

    private val _navigateToAddChild = Channel<Unit>(Channel.BUFFERED)
    val navigateToAddChild = _navigateToAddChild.receiveAsFlow()

    fun onSelectChild(childId: String) {
        childRepository.selectChild(childId)
        _navigateBack.trySend(Unit)
    }

    fun onAddChildClick() {
        _navigateToAddChild.trySend(Unit)
    }
}
