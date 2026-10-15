package com.pasitos.app.feature.measurementhistory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pasitos.app.core.navigation.NsRoute
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.measurement.MeasurementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MeasurementHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    childRepository: ChildRepository,
    measurementRepository: MeasurementRepository,
) : ViewModel() {

    private val childId: String = savedStateHandle.toRoute<NsRoute.MeasurementHistory>().childId

    val uiState: StateFlow<MeasurementHistoryUiState> = combine(
        childRepository.children,
        measurementRepository.measurements,
    ) { children, allMeasurements ->
        MeasurementHistoryUiState(
            childName = children.firstOrNull { it.id == childId }?.name.orEmpty(),
            measurements = allMeasurements
                .filter { it.childId == childId }
                .sortedByDescending { it.date },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeasurementHistoryUiState())
}
