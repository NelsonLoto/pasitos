package com.pasitos.backend.medicion

import kotlinx.serialization.Serializable

@Serializable
data class CreateMedicionRequest(
    val fechaMedicion: String,
    val pesoKg: Double? = null,
    val tallaCm: Double? = null,
    val perimetroCefalicoCm: Double? = null,
)

@Serializable
data class MedicionResponse(
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
