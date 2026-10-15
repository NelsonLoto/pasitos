package com.pasitos.app.feature.advicelist

data class AdviceTipUiModel(
    val id: String,
    val category: String,
    val title: String,
    val summary: String,
    val emoji: String,
)

data class AdviceListUiState(
    val childName: String = "",
    val stageLabel: String = "Consejos por edad",
    val tips: List<AdviceTipUiModel> = emptyList(),
)
