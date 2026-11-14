package com.pasitos.app.feature.childselector

import com.pasitos.app.data.child.model.Child

data class ChildSelectorUiState(
    val children: List<Child> = emptyList(),
    val activeChildId: String? = null,
)
