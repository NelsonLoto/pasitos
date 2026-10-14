package com.ninosano.app.feature.home

import com.ninosano.app.data.child.model.Child
import com.ninosano.app.data.measurement.model.Measurement

data class HomeUiState(
    val greetingName: String = "",
    val activeChild: Child? = null,
    val lastMeasurement: Measurement? = null,
)
