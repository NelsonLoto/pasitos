package com.pasitos.backend.measurement

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Contrato heredado que usa la app actual, con claves JSON en español. Lo reemplaza la
// sincronización (POST /sync/push, ADR-28) en I1.

@Serializable
data class CreateMeasurementRequest(
    @SerialName("fechaMedicion") val measuredOn: String,
    @SerialName("pesoKg") val weightKg: Double? = null,
    @SerialName("tallaCm") val lengthCm: Double? = null,
    @SerialName("perimetroCefalicoCm") val headCircumferenceCm: Double? = null,
)

/**
 * Los percentiles van siempre en null y `fueraDeRango` en false: ya no se guardan (ADR-20) y el
 * motor de `shared` los calcula al leer desde I2. Se mantienen porque la app actual los espera.
 */
@Serializable
data class MeasurementResponse(
    val id: String,
    @SerialName("fechaMedicion") val measuredOn: String,
    @SerialName("pesoKg") val weightKg: Double?,
    @SerialName("tallaCm") val lengthCm: Double?,
    @SerialName("perimetroCefalicoCm") val headCircumferenceCm: Double?,
    @SerialName("percentilPeso") val weightPercentile: Double?,
    @SerialName("percentilTalla") val lengthPercentile: Double?,
    @SerialName("percentilPerimetroCefalico") val headCircumferencePercentile: Double?,
    @SerialName("fueraDeRango") val outOfRange: Boolean,
)

internal fun Measurement.toResponse() = MeasurementResponse(
    id = id.toString(),
    measuredOn = measuredOn.toString(),
    weightKg = weightKg?.toDouble(),
    lengthCm = lengthCm?.toDouble(),
    headCircumferenceCm = headCircumferenceCm?.toDouble(),
    weightPercentile = null,
    lengthPercentile = null,
    headCircumferencePercentile = null,
    outOfRange = false,
)
