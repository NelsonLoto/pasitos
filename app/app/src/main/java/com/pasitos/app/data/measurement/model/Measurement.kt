package com.pasitos.app.data.measurement.model

import com.pasitos.app.core.sync.SyncState
import com.pasitos.shared.api.AuthorRole
import java.time.LocalDate

/** Forma de medición de la longitud o talla (RN-CRE-03). Mismos nombres que en el backend. */
enum class LengthPosition { LYING, STANDING }

/**
 * Medición antropométrica (RF-CRE-01). Tiene al menos uno de los tres valores; la forma de medición
 * acompaña a la longitud o talla. Percentiles, IMC y estado llegan en I2.
 */
data class Measurement(
    val id: String,
    val childId: String,
    val date: LocalDate,
    val weightKg: Double?,
    val lengthCm: Double?,
    val lengthPosition: LengthPosition?,
    val headCircumferenceCm: Double?,
    val authorAccountId: String,
    val authorRole: AuthorRole,
    val syncState: SyncState,
    /** Motivo del rechazo del servidor, solo con [SyncState.ERROR]. */
    val syncError: String? = null,
)

/** Lo que el tutor carga o edita (RF-CRE-01, RF-CRE-02). */
data class MeasurementInput(
    val date: LocalDate,
    val weightKg: Double?,
    val lengthCm: Double?,
    val lengthPosition: LengthPosition?,
    val headCircumferenceCm: Double?,
)
