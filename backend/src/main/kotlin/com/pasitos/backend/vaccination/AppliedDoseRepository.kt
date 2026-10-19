package com.pasitos.backend.vaccination

import com.pasitos.backend.common.AuthorRole
import com.pasitos.backend.common.toUtc
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

/** Una dosis del calendario registrada para un niño. [appliedOn] nulo: fecha no registrada (RN-VAC-04). */
data class AppliedDose(
    val id: UUID,
    val childId: UUID,
    val doseCode: String,
    val appliedOn: LocalDate?,
    val lot: String?,
    val place: String?,
    val authorAccountId: UUID,
    val authorRole: AuthorRole,
    val syncVersion: Long,
    val deletedAt: Instant?,
)

data class AppliedDoseValues(val appliedOn: LocalDate?, val lot: String?, val place: String?)

/**
 * Todas las funciones asumen una transacción abierta. Solo maneja dosis del calendario: las de
 * campañas (`dose_code` nulo) llegan en I6.
 */
class AppliedDoseRepository {

    fun insert(childId: UUID, doseCode: String, values: AppliedDoseValues, author: UUID, role: AuthorRole): AppliedDose {
        val id = UUID.randomUUID()
        AppliedDoses.insert {
            it[AppliedDoses.id] = id
            it[AppliedDoses.childId] = childId
            it[AppliedDoses.doseCode] = doseCode
            it[appliedOn] = values.appliedOn
            it[lot] = values.lot
            it[place] = values.place
            it[authorAccountId] = author
            it[authorRole] = role
        }
        return requireNotNull(findActive(id))
    }

    fun update(id: UUID, values: AppliedDoseValues) {
        AppliedDoses.update({ AppliedDoses.id eq id }) {
            it[appliedOn] = values.appliedOn
            it[lot] = values.lot
            it[place] = values.place
        }
    }

    /** Lápida: la app de los demás tutores se entera del borrado por sincronización. */
    fun softDelete(id: UUID, at: Instant) {
        AppliedDoses.update({ AppliedDoses.id eq id }) {
            it[deletedAt] = at.toUtc()
        }
    }

    fun findActive(id: UUID): AppliedDose? =
        AppliedDoses.selectAll()
            .where { (AppliedDoses.id eq id) and AppliedDoses.doseCode.isNotNull() and AppliedDoses.deletedAt.isNull() }
            .firstOrNull()
            ?.toAppliedDose()

    /** Códigos ya registrados para el niño (RN-VAC-09). */
    fun activeCodes(childId: UUID): Set<String> =
        AppliedDoses.selectAll()
            .where { (AppliedDoses.childId eq childId) and AppliedDoses.doseCode.isNotNull() and AppliedDoses.deletedAt.isNull() }
            .mapTo(mutableSetOf()) { requireNotNull(it[AppliedDoses.doseCode]) }

    fun listActiveForChild(childId: UUID): List<AppliedDose> =
        AppliedDoses.selectAll()
            .where { (AppliedDoses.childId eq childId) and AppliedDoses.doseCode.isNotNull() and AppliedDoses.deletedAt.isNull() }
            .map { it.toAppliedDose() }

    /** Filas vigentes o borradas de [childIds] con `sync_version` mayor a [cursor] (14 · Sincronización). */
    fun changesSince(childIds: Set<UUID>, cursor: Long, limit: Int): List<AppliedDose> =
        AppliedDoses.selectAll()
            .where {
                (AppliedDoses.childId inList childIds) and
                    AppliedDoses.doseCode.isNotNull() and
                    (AppliedDoses.syncVersion greater cursor)
            }
            .orderBy(AppliedDoses.syncVersion to SortOrder.ASC)
            .limit(limit)
            .map { it.toAppliedDose() }

    /** Borrado físico de todas las dosis del niño, al eliminarse su perfil (RN-HIJ-08). */
    fun purgeChild(childId: UUID) {
        AppliedDoses.deleteWhere { AppliedDoses.childId eq childId }
    }

    private fun ResultRow.toAppliedDose() = AppliedDose(
        id = this[AppliedDoses.id],
        childId = this[AppliedDoses.childId],
        doseCode = requireNotNull(this[AppliedDoses.doseCode]),
        appliedOn = this[AppliedDoses.appliedOn],
        lot = this[AppliedDoses.lot],
        place = this[AppliedDoses.place],
        authorAccountId = this[AppliedDoses.authorAccountId],
        authorRole = this[AppliedDoses.authorRole],
        syncVersion = this[AppliedDoses.syncVersion],
        deletedAt = this[AppliedDoses.deletedAt]?.toInstant(),
    )
}
