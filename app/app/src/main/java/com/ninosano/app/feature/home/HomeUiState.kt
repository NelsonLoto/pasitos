package com.ninosano.app.feature.home

import com.ninosano.app.data.child.model.Child

data class HomeUiState(
    val greetingName: String = "",
    val activeChild: Child? = null,
)
