package com.pasitos.backend.vaccination

import com.pasitos.backend.common.toUtc
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

data class CalendarVersion(val id: UUID, val effectiveFrom: LocalDate)

/** Una fila de `calendar_doses` con los datos de su vacuna. */
data class CalendarDoseRow(
    val doseCode: String,
    val vaccineName: String,
    val prevents: String?,
    val source: String?,
    val doseLabel: String,
    val ageMonths: Int,
    val leadDays: Int,
    val toleranceDays: Int,
    val bornFrom: LocalDate?,
    val bornUntil: LocalDate?,
    val sortOrder: Int,
)

/** Todas las funciones asumen una transacción abierta. */
class CalendarRepository {

    fun versionExists(effectiveFrom: LocalDate): Boolean =
        CalendarVersions.selectAll().where { CalendarVersions.effectiveFrom eq effectiveFrom }.any()

    /**
     * Crea una versión publicada con sus dosis. Cada vacuna está una sola vez en `vaccines`: si ya
     * existe, se actualizan "qué previene" y la fuente con los de esta versión.
     */
    fun insertPublishedVersion(effectiveFrom: LocalDate, rows: List<CalendarSeedRow>, now: Instant): UUID {
        val vaccineIds = rows.distinctBy { it.vaccine }.associate { row -> row.vaccine to upsertVaccine(row, now) }
        val versionId = UUID.randomUUID()
        CalendarVersions.insert {
            it[id] = versionId
            it[CalendarVersions.effectiveFrom] = effectiveFrom
            it[status] = CalendarVersionStatus.PUBLISHED
        }
        for (row in rows) {
            CalendarDoses.insert {
                it[id] = UUID.randomUUID()
                it[CalendarDoses.versionId] = versionId
                it[vaccineId] = vaccineIds.getValue(row.vaccine)
                it[doseCode] = row.doseCode
                it[doseLabel] = row.doseLabel
                it[ageMonths] = row.ageMonths
                it[leadDays] = row.leadDays
                it[toleranceDays] = row.toleranceDays
                it[bornFrom] = row.bornFrom
                it[bornUntil] = row.bornUntil
                it[sortOrder] = row.sortOrder
            }
        }
        return versionId
    }

    /** La versión vigente: la publicada más reciente que ya entró en vigencia (RN-VAC-03). */
    fun currentVersion(today: LocalDate): CalendarVersion? =
        CalendarVersions.selectAll()
            .where { (CalendarVersions.status eq CalendarVersionStatus.PUBLISHED) and (CalendarVersions.effectiveFrom lessEq today) }
            .orderBy(CalendarVersions.effectiveFrom to SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.let { CalendarVersion(it[CalendarVersions.id], it[CalendarVersions.effectiveFrom]) }

    /** Las dosis de una versión, en el orden del calendario. */
    fun doses(versionId: UUID): List<CalendarDoseRow> =
        CalendarDoses.join(Vaccines, JoinType.INNER, CalendarDoses.vaccineId, Vaccines.id)
            .selectAll()
            .where { CalendarDoses.versionId eq versionId }
            .orderBy(CalendarDoses.sortOrder to SortOrder.ASC)
            .map { it.toDoseRow() }

    /**
     * Las dosis de las versiones anteriores a [current] cuyo código ya no está en ella, con el
     * nombre de la versión más reciente que la tenía. Así la app muestra las dosis aplicadas que
     * un calendario nuevo sacó (RN-VAC-03).
     */
    fun formerDoses(current: CalendarVersion): List<CalendarDoseRow> {
        val currentCodes = doses(current.id).mapTo(mutableSetOf()) { it.doseCode }
        return CalendarDoses
            .join(Vaccines, JoinType.INNER, CalendarDoses.vaccineId, Vaccines.id)
            .join(CalendarVersions, JoinType.INNER, CalendarDoses.versionId, CalendarVersions.id)
            .selectAll()
            .where {
                (CalendarVersions.status inList listOf(CalendarVersionStatus.PUBLISHED, CalendarVersionStatus.RETIRED)) and
                    (CalendarVersions.effectiveFrom less current.effectiveFrom)
            }
            .orderBy(CalendarVersions.effectiveFrom to SortOrder.DESC, CalendarDoses.sortOrder to SortOrder.ASC)
            .map { it.toDoseRow() }
            .filterNot { it.doseCode in currentCodes }
            .distinctBy { it.doseCode }
    }

    private fun upsertVaccine(row: CalendarSeedRow, now: Instant): UUID {
        val existing = Vaccines.selectAll().where { Vaccines.name eq row.vaccine }.firstOrNull()?.get(Vaccines.id)
        if (existing != null) {
            Vaccines.update({ Vaccines.id eq existing }) {
                it[prevents] = row.prevents
                it[sourceText] = row.source
                it[updatedAt] = now.toUtc()
            }
            return existing
        }
        val id = UUID.randomUUID()
        Vaccines.insert {
            it[Vaccines.id] = id
            it[name] = row.vaccine
            it[prevents] = row.prevents
            it[sourceText] = row.source
        }
        return id
    }

    private fun ResultRow.toDoseRow() = CalendarDoseRow(
        doseCode = this[CalendarDoses.doseCode],
        vaccineName = this[Vaccines.name],
        prevents = this[Vaccines.prevents],
        source = this[Vaccines.sourceText],
        doseLabel = this[CalendarDoses.doseLabel],
        ageMonths = this[CalendarDoses.ageMonths],
        leadDays = this[CalendarDoses.leadDays],
        toleranceDays = this[CalendarDoses.toleranceDays],
        bornFrom = this[CalendarDoses.bornFrom],
        bornUntil = this[CalendarDoses.bornUntil],
        sortOrder = this[CalendarDoses.sortOrder],
    )
}
