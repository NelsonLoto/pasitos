package com.pasitos.app.feature.home

import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.measurement.model.Measurement
import com.pasitos.app.data.vaccination.model.VaccinationSummary

data class HomeUiState(
    val greetingName: String = "",
    val activeChild: Child? = null,
    val lastMeasurement: Measurement? = null,
    /** Nulo sin hijo activo o sin calendario cargado. */
    val vaccinationSummary: VaccinationSummary? = null,
)
