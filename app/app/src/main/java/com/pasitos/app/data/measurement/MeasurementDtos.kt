package com.pasitos.app.data.measurement

import kotlinx.serialization.Serializable

@Serializable
internal data class CreateMeasurementRequestDto(
    val fechaMedicion: String,
    val pesoKg: Double? = null,
    val tallaCm: Double? = null,
    val perimetroCefalicoCm: Double? = null,
)

@Serializable
internal data class MeasurementResponseDto(
    val id: Int,
    val fechaMedicion: String,
    val pesoKg: Double?,
    val tallaCm: Double?,
    val perimetroCefalicoCm: Double?,
    val percentilPeso: Double?,
    val percentilTalla: Double?,
    val percentilPerimetroCefalico: Double?,
    val fueraDeRango: Boolean,
)
