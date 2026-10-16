package com.pasitos.app.feature.vaccination

import com.pasitos.app.data.vaccination.model.DoseStatus

data class VaccineDoseUiModel(
    val id: String,
    val displayName: String,
    val ageLabel: String,
    val status: DoseStatus,
)

data class VaccinationCalendarUiState(
    val childName: String = "",
    val doses: List<VaccineDoseUiModel> = emptyList(),
)
