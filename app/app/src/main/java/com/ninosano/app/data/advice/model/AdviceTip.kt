package com.ninosano.app.data.advice.model

data class AdviceTip(
    val id: String,
    val category: String,
    val ageRangeLabel: String,
    val title: String,
    val summary: String,
    val emoji: String,
    val body: List<String>,
)
