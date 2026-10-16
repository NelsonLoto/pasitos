package com.pasitos.backend.measurement

import com.pasitos.backend.common.AuthorRole
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

data class Measurement(
    val id: UUID,
    val childId: UUID,
    val measuredOn: LocalDate,
    val weightKg: BigDecimal?,
    val lengthCm: BigDecimal?,
    val lengthPosition: LengthPosition?,
    val headCircumferenceCm: BigDecimal?,
    val authorAccountId: UUID,
    val authorRole: AuthorRole,
)

/** Todas las funciones asumen una transacción abierta. Las mediciones borradas no se devuelven. */
class MeasurementRepository {

    fun insert(measurement: Measurement) {
        Measurements.insert {
            it[id] = measurement.id
            it[childId] = measurement.childId
            it[measuredOn] = measurement.measuredOn
            it[weightKg] = measurement.weightKg
            it[lengthCm] = measurement.lengthCm
            it[lengthPosition] = measurement.lengthPosition
            it[headCircumferenceCm] = measurement.headCircumferenceCm
            it[authorAccountId] = measurement.authorAccountId
            it[authorRole] = measurement.authorRole
        }
    }

    fun listByChild(childId: UUID): List<Measurement> =
        Measurements.selectAll()
            .where { (Measurements.childId eq childId) and Measurements.deletedAt.isNull() }
            .orderBy(Measurements.measuredOn to SortOrder.ASC, Measurements.createdAt to SortOrder.ASC)
            .map { it.toMeasurement() }

    fun existsForAuthorOn(childId: UUID, authorAccountId: UUID, measuredOn: LocalDate): Boolean =
        !Measurements.selectAll()
            .where {
                (Measurements.childId eq childId) and
                    (Measurements.authorAccountId eq authorAccountId) and
                    (Measurements.measuredOn eq measuredOn) and
                    Measurements.deletedAt.isNull()
            }
            .empty()

    private fun ResultRow.toMeasurement() = Measurement(
        id = this[Measurements.id],
        childId = this[Measurements.childId],
        measuredOn = this[Measurements.measuredOn],
        weightKg = this[Measurements.weightKg],
        lengthCm = this[Measurements.lengthCm],
        lengthPosition = this[Measurements.lengthPosition],
        headCircumferenceCm = this[Measurements.headCircumferenceCm],
        authorAccountId = this[Measurements.authorAccountId],
        authorRole = this[Measurements.authorRole],
    )
}
