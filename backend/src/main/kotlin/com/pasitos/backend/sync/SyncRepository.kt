package com.pasitos.backend.sync

import com.pasitos.backend.guardianship.GuardianshipsTable as G
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.util.UUID

private object ScopedChildren : Table("children") {
    val id = uuid("id")
    val deletedAt = timestamp("deleted_at").nullable()
}

/** Solo SQL; las transacciones las delimita SyncService o el servicio del dominio. */
class SyncRepository {
    fun lockForRead() { TransactionManager.current().exec("SELECT pg_advisory_xact_lock_shared(162, 1)") }
    fun lockForWrite() { TransactionManager.current().exec("SELECT pg_advisory_xact_lock(162, 1)") }

    fun committedVersion(): Long = TransactionManager.current().exec(
        "SELECT CASE WHEN is_called THEN last_value ELSE 0 END FROM sync_seq",
    ) { rows -> check(rows.next()); rows.getLong(1) }!!

    /** I0: alcance de tutor. patients no otorga permisos; los vínculos pediátricos llegan en I3. */
    fun guardianChildren(accountId: UUID): Map<UUID, Long> = G.innerJoin(ScopedChildren, { childId }, { id })
        .select(G.childId, G.syncVersion).where {
            (G.accountId eq accountId) and G.deletedAt.isNull() and ScopedChildren.deletedAt.isNull()
        }.associate { it[G.childId] to it[G.syncVersion] }

    fun revokedSince(accountId: UUID, cursor: Long, through: Long): List<String> = AccessRevocationsTable.selectAll().where {
        (AccessRevocationsTable.accountId eq accountId) and
            (AccessRevocationsTable.syncVersion greater cursor) and (AccessRevocationsTable.syncVersion lessEq through)
    }.orderBy(AccessRevocationsTable.syncVersion).map { it[AccessRevocationsTable.childId].toString() }.distinct()

    /** El servicio que da de baja una tutoría llama esto en la misma transacción (13). */
    fun recordRevocation(accountId: UUID, childId: UUID) {
        AccessRevocationsTable.insert {
            it[id] = UUID.randomUUID(); it[AccessRevocationsTable.accountId] = accountId
            it[AccessRevocationsTable.childId] = childId; it[syncVersion] = 0 // Lo asigna el trigger.
        }
    }
}
