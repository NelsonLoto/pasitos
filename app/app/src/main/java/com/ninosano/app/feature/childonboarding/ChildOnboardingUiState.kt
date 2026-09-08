package com.ninosano.app.feature.childonboarding

import com.ninosano.app.data.child.model.ChildSex
import java.time.LocalDate

data class ChildOnboardingUiState(
    val name: String = "",
    val birthDate: LocalDate? = null,
    val sex: ChildSex = ChildSex.Female,
    val nameError: String? = null,
    val birthDateError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
)
