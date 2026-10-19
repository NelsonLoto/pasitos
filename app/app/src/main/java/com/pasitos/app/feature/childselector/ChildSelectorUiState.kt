package com.pasitos.app.feature.childselector

import com.pasitos.app.data.child.model.Child
import java.time.LocalDate

data class ChildSelectorUiState(
    val children: List<Child> = emptyList(),
    val activeChildId: String? = null,
    /** Para la edad de cada hijo, en hora argentina (ADR-26). */
    val today: LocalDate = LocalDate.MIN,
)
