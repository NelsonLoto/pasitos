package com.ninosano.app.data.measurement.model

import java.time.LocalDate

/**
 * Medición antropométrica (RF04). Al menos uno de los tres valores debe estar presente
 * — no se exige cargar los tres juntos, un padre/madre puede registrar solo lo que midió.
 */
data class Measurement(
    val id: String,
    val childId: String,
    val date: LocalDate,
    val weightKg: Double?,
    val heightCm: Double?,
    val headCircumferenceCm: Double?,
)
