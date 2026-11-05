package com.ninosano.app.feature.measurement

import java.time.LocalDate

data class MeasurementFormUiState(
    val date: LocalDate = LocalDate.now(),
    val weightKg: String = "",
    val heightCm: String = "",
    val headCircumferenceCm: String = "",
    val weightError: String? = null,
    val heightError: String? = null,
    val headCircumferenceError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
)
