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

/** Una vacuna fuera del Calendario Nacional (RF-VAC-05). */
data class OtherVaccine(
    val id: UUID,
    val childId: UUID,
    val name: String,
    val appliedOn: LocalDate,
    val lot: String?,
    val note: String?,
    val authorAccountId: UUID,
    val authorRole: AuthorRole,
    val syncVersion: Long,
    val deletedAt: Instant?,
)

data class OtherVaccineValues(val name: String, val appliedOn: LocalDate, val lot: String?, val note: String?)

/** Todas las funciones asumen una transacción abierta. */
class OtherVaccineRepository {

    fun insert(childId: UUID, values: OtherVaccineValues, author: UUID, role: AuthorRole): OtherVaccine {
        val id = UUID.randomUUID()
        OtherVaccines.insert {
            it[OtherVaccines.id] = id
            it[OtherVaccines.childId] = childId
            it[name] = values.name
            it[appliedOn] = values.appliedOn
            it[lot] = values.lot
            it[note] = values.note
            it[authorAccountId] = author
            it[authorRole] = role
        }
        return requireNotNull(findActive(id))
    }

    fun update(id: UUID, values: OtherVaccineValues) {
        OtherVaccines.update({ OtherVaccines.id eq id }) {
            it[name] = values.name
            it[appliedOn] = values.appliedOn
            it[lot] = values.lot
            it[note] = values.note
        }
    }

    /** Lápida: la app de los demás tutores se entera del borrado por sincronización. */
    fun softDelete(id: UUID, at: Instant) {
        OtherVaccines.update({ OtherVaccines.id eq id }) {
            it[deletedAt] = at.toUtc()
        }
    }

    fun findActive(id: UUID): OtherVaccine? =
        OtherVaccines.selectAll()
            .where { (OtherVaccines.id eq id) and OtherVaccines.deletedAt.isNull() }
            .firstOrNull()
            ?.toOtherVaccine()

    fun listActiveForChild(childId: UUID): List<OtherVaccine> =
        OtherVaccines.selectAll()
            .where { (OtherVaccines.childId eq childId) and OtherVaccines.deletedAt.isNull() }
            .map { it.toOtherVaccine() }

    /** Filas vigentes o borradas de [childIds] con `sync_version` mayor a [cursor] (14 · Sincronización). */
    fun changesSince(childIds: Set<UUID>, cursor: Long, limit: Int): List<OtherVaccine> =
        OtherVaccines.selectAll()
            .where { (OtherVaccines.childId inList childIds) and (OtherVaccines.syncVersion greater cursor) }
            .orderBy(OtherVaccines.syncVersion to SortOrder.ASC)
            .limit(limit)
            .map { it.toOtherVaccine() }

    /** Borrado físico de todas las otras vacunas del niño, al eliminarse su perfil (RN-HIJ-08). */
    fun purgeChild(childId: UUID) {
        OtherVaccines.deleteWhere { OtherVaccines.childId eq childId }
    }

    private fun ResultRow.toOtherVaccine() = OtherVaccine(
        id = this[OtherVaccines.id],
        childId = this[OtherVaccines.childId],
        name = this[OtherVaccines.name],
        appliedOn = this[OtherVaccines.appliedOn],
        lot = this[OtherVaccines.lot],
        note = this[OtherVaccines.note],
        authorAccountId = this[OtherVaccines.authorAccountId],
        authorRole = this[OtherVaccines.authorRole],
        syncVersion = this[OtherVaccines.syncVersion],
        deletedAt = this[OtherVaccines.deletedAt]?.toInstant(),
    )
}
