package com.pasitos.app.feature.home

import com.pasitos.app.data.child.model.Child

data class HomeUiState(
    val greetingName: String = "",
    val activeChild: Child? = null,
)
