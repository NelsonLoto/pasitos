package com.ninosano.app.feature.measurementhistory

import com.ninosano.app.data.measurement.model.Measurement

data class MeasurementHistoryUiState(
    val childName: String = "",
    // Más reciente primero.
    val measurements: List<Measurement> = emptyList(),
)
