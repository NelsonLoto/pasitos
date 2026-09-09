package com.ninosano.app.feature.vaccinedetail

import com.ninosano.app.core.designsystem.component.NsBadgeTone
import java.time.LocalDate

data class VaccineDetailUiState(
    val vaccineDisplayName: String = "",
    val statusText: String = "",
    val statusTone: NsBadgeTone = NsBadgeTone.Warning,
    val recommendedAgeLabel: String = "",
    val description: String = "",
    val date: LocalDate? = null,
    val lot: String = "",
    val dateError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
)
