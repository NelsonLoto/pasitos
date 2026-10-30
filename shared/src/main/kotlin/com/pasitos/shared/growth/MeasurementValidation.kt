package com.pasitos.shared.growth

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Validación de una medición antes de guardarla (RF-CRE-01, RN-CRE-03, RN-CRE-11), para que la
 * app la aplique antes de enviar por `/sync/push` (ADR-28) y el backend repita la misma regla al
 * recibirla. El resto del motor de crecimiento (IMC, percentiles, estado) llega en I2.
 */
object MeasurementValidation {

    /**
     * RN-CRE-11: rango absoluto de entrada. Son valores fijos, pensados para rechazar entradas
     * imposibles (ej. "-5 kg"); el rango real de plausibilidad (banderas de puntaje Z) llega con
     * el motor de crecimiento y su configuración por el administrador (RF-ADM-04, RF-ADM-05).
     */
    val WEIGHT_KG_RANGE = 0.1..150.0
    val LENGTH_CM_RANGE = 20.0..200.0
    val HEAD_CIRCUMFERENCE_CM_RANGE = 20.0..70.0

    fun isWeightInRange(kg: Double): Boolean = kg.isFinite() && kg in WEIGHT_KG_RANGE

    fun isLengthInRange(cm: Double): Boolean = cm.isFinite() && cm in LENGTH_CM_RANGE

    fun isHeadCircumferenceInRange(cm: Double): Boolean = cm.isFinite() && cm in HEAD_CIRCUMFERENCE_CM_RANGE

    /** RF-CRE-01: decimales que se guardan de cada valor. */
    const val WEIGHT_DECIMALS = 3
    const val LENGTH_DECIMALS = 1
    const val HEAD_CIRCUMFERENCE_DECIMALS = 1

    /** RN-CRE-03: desde los 60 meses solo se admite medir de pie. */
    fun isLengthPositionValid(birthDate: LocalDate, measuredOn: LocalDate, standing: Boolean): Boolean =
        standing || offersLying(birthDate, measuredOn)

    /** RF-CRE-10, CA3: antes de los 60 meses se ofrecen las dos formas de medición; después, solo de pie. */
    fun offersLying(birthDate: LocalDate, measuredOn: LocalDate): Boolean = ageInMonths(birthDate, measuredOn) < 60

    /** RF-CRE-01 y RN-CRE-01: el perímetro cefálico se registra hasta los 5 años. */
    fun offersHeadCircumference(birthDate: LocalDate, measuredOn: LocalDate): Boolean = ageInMonths(birthDate, measuredOn) < 60

    /**
     * Forma de medición que viene preseleccionada (RF-CRE-10): acostado antes de los 24 meses, de
     * pie desde entonces.
     */
    fun defaultStanding(birthDate: LocalDate, measuredOn: LocalDate): Boolean =
        ageInMonths(birthDate, measuredOn) >= 24

    private fun ageInMonths(birthDate: LocalDate, measuredOn: LocalDate): Long =
        ChronoUnit.MONTHS.between(birthDate, measuredOn)
}
