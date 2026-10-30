package com.pasitos.app.feature.childselector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.app.data.child.ChildRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** "Mis hijos": elegir el hijo activo (RF-HIJ-04), ver un perfil o agregar otro hijo. */
@HiltViewModel
class ChildSelectorViewModel @Inject constructor(
    private val childRepository: ChildRepository,
    clock: ArgentinaClock,
) : ViewModel() {

    private val today = clock.today()

    val uiState: StateFlow<ChildSelectorUiState> = combine(
        childRepository.children,
        childRepository.activeChildId,
    ) { children, activeChildId ->
        ChildSelectorUiState(children = children, activeChildId = activeChildId, today = today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChildSelectorUiState(today = today))

    private val _navigateBack = Channel<Unit>(Channel.BUFFERED)
    val navigateBack = _navigateBack.receiveAsFlow()

    private val _navigateToAddChild = Channel<Unit>(Channel.BUFFERED)
    val navigateToAddChild = _navigateToAddChild.receiveAsFlow()

    fun onSelectChild(childId: String) {
        viewModelScope.launch {
            childRepository.selectChild(childId)
            _navigateBack.send(Unit)
        }
    }

    fun onAddChildClick() {
        _navigateToAddChild.trySend(Unit)
    }
}
