package com.pasitos.app.data.child.model

/**
 * Rangos de las medidas al nacer fuera de los cuales se pide revisar el valor antes de guardarlo
 * (RF-HIJ-01, CA4). Cubren de 22 a 42 semanas de gestación con margen. ⚕️ PVM-06
 *
 * En I1 solo hay rangos absolutos fijos. El control por puntaje Z de RN-CRE-11 llega con I2-S2 y
 * los rangos pasan a los datos de referencia configurables con I2-B1.
 */
object BirthMeasurementRanges {
    val weightKg: ClosedFloatingPointRange<Double> = 0.3..6.5
    val lengthCm: ClosedFloatingPointRange<Double> = 25.0..62.0
    val headCircumferenceCm: ClosedFloatingPointRange<Double> = 18.0..42.0
}
