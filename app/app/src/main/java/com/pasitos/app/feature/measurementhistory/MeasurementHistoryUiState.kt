package com.pasitos.app.feature.measurementhistory

import com.pasitos.app.data.measurement.model.Measurement

data class MeasurementHistoryUiState(
    val childName: String = "",
    // Más reciente primero.
    val measurements: List<Measurement> = emptyList(),
)
