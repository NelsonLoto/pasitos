package com.pasitos.app.data.measurement

import com.pasitos.app.core.sync.SyncState
import com.pasitos.app.data.child.ChildTestData
import com.pasitos.app.data.measurement.model.Measurement
import com.pasitos.app.data.measurement.model.MeasurementInput
import com.pasitos.shared.api.AuthorRole
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Repositorio en memoria para las pruebas de las pantallas. Las mediciones nuevas son de [ChildTestData.ACCOUNT_ID]. */
class FakeMeasurementRepository(measurements: List<Measurement> = emptyList()) : MeasurementRepository {

    val state = MutableStateFlow(measurements)

    val added = mutableListOf<Pair<String, MeasurementInput>>()
    val updated = mutableListOf<Pair<String, MeasurementInput>>()
    val deleted = mutableListOf<String>()
    val discarded = mutableListOf<String>()

    override fun observeMeasurements(childId: String): Flow<List<Measurement>> =
        state.map { list -> list.filter { it.childId == childId }.sortedByDescending { it.date } }

    override fun observeMeasurement(id: String): Flow<Measurement?> = state.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun findOwnOn(childId: String, date: LocalDate, excludeId: String?): Measurement? =
        state.value.firstOrNull { it.childId == childId && it.date == date && it.authorAccountId == ChildTestData.ACCOUNT_ID && it.id != excludeId }

    override suspend fun addMeasurement(childId: String, input: MeasurementInput): Result<Measurement> {
        added += childId to input
        val measurement = input.toMeasurement("new-${added.size}", childId)
        state.value = state.value + measurement
        return Result.success(measurement)
    }

    override suspend fun updateMeasurement(id: String, input: MeasurementInput): Result<Measurement> {
        updated += id to input
        val existing = state.value.first { it.id == id }
        val measurement = input.toMeasurement(id, existing.childId)
        state.value = state.value.map { if (it.id == id) measurement else it }
        return Result.success(measurement)
    }

    override suspend fun deleteMeasurement(id: String): Result<Unit> {
        deleted += id
        state.value = state.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun discardChange(id: String): Result<Unit> {
        discarded += id
        state.value = state.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    private fun MeasurementInput.toMeasurement(id: String, childId: String) = Measurement(
        id = id,
        childId = childId,
        date = date,
        weightKg = weightKg,
        lengthCm = lengthCm,
        lengthPosition = lengthPosition,
        headCircumferenceCm = headCircumferenceCm,
        authorAccountId = ChildTestData.ACCOUNT_ID,
        authorRole = AuthorRole.GUARDIAN,
        syncState = SyncState.PENDING,
    )
}
