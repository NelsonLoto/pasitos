package com.pasitos.app.feature.home

import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.measurement.model.Measurement

data class HomeUiState(
    val greetingName: String = "",
    val activeChild: Child? = null,
    val lastMeasurement: Measurement? = null,
)
