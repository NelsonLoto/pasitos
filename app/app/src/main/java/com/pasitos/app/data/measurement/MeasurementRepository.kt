package com.pasitos.app.data.measurement

import com.pasitos.app.data.measurement.model.Measurement
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de mediciones antropométricas (RF04). Mismo patrón que
 * [com.pasitos.app.data.auth.AuthRepository] / [com.pasitos.app.data.child.ChildRepository]:
 * implementación mock/local hasta que exista el backend.
 */
interface MeasurementRepository {
    val measurements: StateFlow<List<Measurement>>

    suspend fun addMeasurement(
        childId: String,
        date: LocalDate,
        weightKg: Double?,
        heightCm: Double?,
        headCircumferenceCm: Double?,
    ): Result<Measurement>
}
