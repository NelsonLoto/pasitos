package com.pasitos.app.feature.growthchart

import java.time.LocalDate

enum class MeasurementMetric(val label: String, val unit: String) {
    Weight("Peso", "kg"),
    Height("Talla", "cm"),
    HeadCircumference("P. cefálico", "cm"),
}

data class ChartPoint(val date: LocalDate, val value: Double)

data class GrowthChartUiState(
    val childName: String = "",
    val selectedMetric: MeasurementMetric = MeasurementMetric.Weight,
    val points: List<ChartPoint> = emptyList(),
)
