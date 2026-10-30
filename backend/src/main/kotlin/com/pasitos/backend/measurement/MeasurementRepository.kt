package com.pasitos.backend.measurement

import com.pasitos.backend.common.AuthorRole
import com.pasitos.backend.common.toUtc
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

data class Measurement(
    val id: UUID,
    val childId: UUID,
    val measuredOn: LocalDate,
    val weightKg: BigDecimal?,
    val lengthCm: BigDecimal?,
    val lengthPosition: LengthPosition?,
    val headCircumferenceCm: BigDecimal?,
    val outlierConfirmed: Boolean,
    val authorAccountId: UUID,
    val authorRole: AuthorRole,
    val syncVersion: Long,
    val deletedAt: Instant?,
)

data class MeasurementEdits(
    val measuredOn: LocalDate,
    val weightKg: BigDecimal?,
    val lengthCm: BigDecimal?,
    val lengthPosition: LengthPosition?,
    val headCircumferenceCm: BigDecimal?,
    val outlierConfirmed: Boolean,
)

/** Todas las funciones asumen una transacción abierta (motor de sincronización, ADR-28). */
class MeasurementRepository {

    fun insert(id: UUID, childId: UUID, authorAccountId: UUID, authorRole: AuthorRole, edits: MeasurementEdits) {
        Measurements.insert {
            it[Measurements.id] = id
            it[Measurements.childId] = childId
            it[measuredOn] = edits.measuredOn
            it[weightKg] = edits.weightKg
            it[lengthCm] = edits.lengthCm
            it[lengthPosition] = edits.lengthPosition
            it[headCircumferenceCm] = edits.headCircumferenceCm
            it[outlierConfirmed] = edits.outlierConfirmed
            it[Measurements.authorAccountId] = authorAccountId
            it[Measurements.authorRole] = authorRole
        }
    }

    fun update(id: UUID, edits: MeasurementEdits) {
        Measurements.update({ Measurements.id eq id }) {
            it[measuredOn] = edits.measuredOn
            it[weightKg] = edits.weightKg
            it[lengthCm] = edits.lengthCm
            it[lengthPosition] = edits.lengthPosition
            it[headCircumferenceCm] = edits.headCircumferenceCm
            it[outlierConfirmed] = edits.outlierConfirmed
        }
    }

    /** RF-CRE-02: borrado lógico. Idempotente (ADR-15): una fila ya borrada no cambia. */
    fun softDelete(id: UUID, at: Instant) {
        Measurements.update({ (Measurements.id eq id) and Measurements.deletedAt.isNull() }) {
            it[deletedAt] = at.toUtc()
        }
    }

    fun findById(id: UUID): Measurement? =
        Measurements.selectAll().where { Measurements.id eq id }.firstOrNull()?.toMeasurement()

    /** RN-CRE-12: si ya hay otra medición (no [excludeId]) de ese autor en esa fecha. */
    fun existsForAuthorOn(childId: UUID, authorAccountId: UUID, measuredOn: LocalDate, excludeId: UUID): Boolean =
        !Measurements.selectAll()
            .where {
                (Measurements.childId eq childId) and
                    (Measurements.authorAccountId eq authorAccountId) and
                    (Measurements.measuredOn eq measuredOn) and
                    (Measurements.id neq excludeId) and
                    Measurements.deletedAt.isNull()
            }
            .empty()

    /** Filas (vigentes o borradas) de niños en [childIds] con `sync_version` mayor a [cursor]. */
    fun changesSince(childIds: Set<UUID>, cursor: Long, limit: Int): List<Measurement> =
        Measurements.selectAll()
            .where { (Measurements.childId inList childIds) and (Measurements.syncVersion greater cursor) }
            .orderBy(Measurements.syncVersion to SortOrder.ASC)
            .limit(limit)
            .map { it.toMeasurement() }

    /** Filas vigentes de un niño, para la foto completa de un `newChildId`. */
    fun listByChild(childId: UUID): List<Measurement> =
        Measurements.selectAll()
            .where { (Measurements.childId eq childId) and Measurements.deletedAt.isNull() }
            .orderBy(Measurements.measuredOn to SortOrder.ASC)
            .map { it.toMeasurement() }

    private fun ResultRow.toMeasurement() = Measurement(
        id = this[Measurements.id],
        childId = this[Measurements.childId],
        measuredOn = this[Measurements.measuredOn],
        weightKg = this[Measurements.weightKg],
        lengthCm = this[Measurements.lengthCm],
        lengthPosition = this[Measurements.lengthPosition],
        headCircumferenceCm = this[Measurements.headCircumferenceCm],
        outlierConfirmed = this[Measurements.outlierConfirmed],
        authorAccountId = this[Measurements.authorAccountId],
        authorRole = this[Measurements.authorRole],
        syncVersion = this[Measurements.syncVersion],
        deletedAt = this[Measurements.deletedAt]?.toInstant(),
    )
}
