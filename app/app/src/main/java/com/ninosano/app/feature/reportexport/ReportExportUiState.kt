package com.ninosano.app.feature.reportexport

data class ReportExportUiState(
    val childName: String = "",
    val includeGrowthChart: Boolean = true,
    val includeMeasurements: Boolean = true,
    val includeVaccinationStatus: Boolean = true,
    val isLoading: Boolean = false,
    val generalError: String? = null,
    val successMessage: String? = null,
)
