package com.ninosano.app.data.measurement

import com.ninosano.app.data.measurement.model.Measurement
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class FakeMeasurementRepository @Inject constructor() : MeasurementRepository {

    private val _measurements = MutableStateFlow<List<Measurement>>(emptyList())
    override val measurements: StateFlow<List<Measurement>> = _measurements.asStateFlow()

    override suspend fun addMeasurement(
        childId: String,
        date: LocalDate,
        weightKg: Double?,
        heightCm: Double?,
        headCircumferenceCm: Double?,
    ): Result<Measurement> {
        delay(600)
        val measurement = Measurement(
            id = "measurement-${_measurements.value.size + 1}",
            childId = childId,
            date = date,
            weightKg = weightKg,
            heightCm = heightCm,
            headCircumferenceCm = headCircumferenceCm,
        )
        _measurements.update { it + measurement }
        return Result.success(measurement)
    }
}
