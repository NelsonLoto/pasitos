package com.ninosano.app.feature.childselector

import com.ninosano.app.data.child.model.Child

data class ChildSelectorUiState(
    val children: List<Child> = emptyList(),
    val activeChildId: String? = null,
)
