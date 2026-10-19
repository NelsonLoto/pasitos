package com.pasitos.backend.guardianship

import com.pasitos.backend.common.toUtc
import com.pasitos.backend.sync.AccessRevocations
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

data class Guardianship(val childId: UUID, val accountId: UUID, val role: GuardianshipRole, val syncVersion: Long, val deletedAt: Instant?)

/** Todas las funciones asumen una transacción abierta. */
class GuardianshipRepository {

    fun insert(childId: UUID, accountId: UUID, role: GuardianshipRole) {
        Guardianships.insert {
            it[Guardianships.childId] = childId
            it[Guardianships.accountId] = accountId
            it[Guardianships.role] = role
        }
    }

    /** El rol de [accountId] sobre [childId], o null si no es su tutor. */
    fun findRole(childId: UUID, accountId: UUID): GuardianshipRole? =
        Guardianships.selectAll()
            .where { (Guardianships.childId eq childId) and (Guardianships.accountId eq accountId) and Guardianships.deletedAt.isNull() }
            .firstOrNull()
            ?.get(Guardianships.role)

    /** Tutores vigentes de un niño, el principal primero y los demás por antigüedad (RF-HIJ-09, CA2). */
    fun listForChild(childId: UUID): List<Guardianship> =
        Guardianships.selectAll()
            .where { (Guardianships.childId eq childId) and Guardianships.deletedAt.isNull() }
            .orderBy(Guardianships.role to SortOrder.ASC, Guardianships.createdAt to SortOrder.ASC)
            .map { it.toGuardianship() }

    /**
     * Lápida (RF-HIJ-07, RF-HIJ-09): el tutor deja de verlo, los registros que cargó quedan igual.
     *
     * Registra también la revocación (`revokedChildIds`, 14 · Sincronización §2): el niño sale del
     * alcance de [accountId], así que a sus otros teléfonos ya no les llega ninguna lápida y sin el
     * aviso conservarían sus datos (RNF-OFF-05).
     */
    fun softDelete(childId: UUID, accountId: UUID, at: Instant) {
        val updated = Guardianships.update({
            (Guardianships.childId eq childId) and (Guardianships.accountId eq accountId) and Guardianships.deletedAt.isNull()
        }) {
            it[deletedAt] = at.toUtc()
        }
        if (updated > 0) {
            AccessRevocations.insert {
                it[AccessRevocations.id] = UUID.randomUUID()
                it[AccessRevocations.accountId] = accountId
                it[AccessRevocations.childId] = childId
            }
        }
    }

    fun promoteToPrincipal(childId: UUID, accountId: UUID) {
        Guardianships.update({ (Guardianships.childId eq childId) and (Guardianships.accountId eq accountId) }) {
            it[role] = GuardianshipRole.PRINCIPAL
        }
    }

    /** Filas (vigentes o borradas) de niños en [childIds] con `sync_version` mayor a [cursor]. */
    fun changesSince(childIds: Set<UUID>, cursor: Long, limit: Int): List<Guardianship> =
        Guardianships.selectAll()
            .where { (Guardianships.childId inList childIds) and (Guardianships.syncVersion greater cursor) }
            .orderBy(Guardianships.syncVersion to SortOrder.ASC)
            .limit(limit)
            .map { it.toGuardianship() }

    private fun ResultRow.toGuardianship() = Guardianship(
        childId = this[Guardianships.childId],
        accountId = this[Guardianships.accountId],
        role = this[Guardianships.role],
        syncVersion = this[Guardianships.syncVersion],
        deletedAt = this[Guardianships.deletedAt]?.toInstant(),
    )
}
