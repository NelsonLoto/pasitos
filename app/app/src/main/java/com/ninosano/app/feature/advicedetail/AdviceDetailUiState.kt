package com.ninosano.app.feature.advicedetail

data class AdviceDetailUiState(
    val badgeText: String = "",
    val title: String = "",
    val emoji: String = "",
    val body: List<String> = emptyList(),
)
