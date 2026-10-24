package com.pasitos.app.feature.growthchart

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pasitos.app.core.navigation.PsRoute
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.measurement.MeasurementRepository
import com.pasitos.app.data.measurement.model.Measurement
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class GrowthChartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    childRepository: ChildRepository,
    measurementRepository: MeasurementRepository,
) : ViewModel() {

    val childId: String = savedStateHandle.toRoute<PsRoute.GrowthChart>().childId

    private val _selectedMetric = MutableStateFlow(MeasurementMetric.Weight)

    val uiState: StateFlow<GrowthChartUiState> = combine(
        childRepository.children,
        measurementRepository.observeMeasurements(childId),
        _selectedMetric,
    ) { children, measurements, metric ->
        GrowthChartUiState(
            childName = children.firstOrNull { it.id == childId }?.firstName.orEmpty(),
            selectedMetric = metric,
            points = measurements.sortedBy { it.date }.mapNotNull { it.toChartPoint(metric) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrowthChartUiState())

    fun onMetricSelected(metric: MeasurementMetric) {
        _selectedMetric.value = metric
    }
}

private fun Measurement.toChartPoint(metric: MeasurementMetric): ChartPoint? {
    val value = when (metric) {
        MeasurementMetric.Weight -> weightKg
        MeasurementMetric.Height -> lengthCm
        MeasurementMetric.HeadCircumference -> headCircumferenceCm
    } ?: return null
    return ChartPoint(date = date, value = value)
}
