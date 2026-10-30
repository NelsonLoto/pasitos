package com.pasitos.backend.sync

import com.pasitos.backend.guardianship.Guardianships
import com.pasitos.backend.child.PediatricianPatients
import com.pasitos.backend.child.Children
import java.util.UUID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.JoinType

/** Todas las funciones asumen una transacción abierta. */
class SyncRepository {

    /** Niños de los que [accountId] es tutor (alcance del tutor, 14 · Sincronización §4). */
    fun scopeForGuardian(accountId: UUID): Set<UUID> =
        Guardianships.selectAll()
            .where { (Guardianships.accountId eq accountId) and Guardianships.deletedAt.isNull() }
            .map { it[Guardianships.childId] }
            .toSet()

    /** Todo paciente disponible para un profesional, además de cualquier tutoría existente. */
    fun scopeForAccount(accountId: UUID): Set<UUID> =
        scopeForGuardian(accountId) + PediatricianPatients.selectAll()
            .where { PediatricianPatients.pediatricianAccountId eq accountId }
            .map { it[PediatricianPatients.childId] }

    /**
     * Niños que entraron al alcance de [accountId] después de [cursor]: una fila de vínculo
     * nueva (o reactivada) con `sync_version` mayor al cursor (14 · Sincronización §2).
     */
    fun newChildIdsSince(accountId: UUID, cursor: Long): List<UUID> =
        Guardianships.selectAll()
            .where {
                (Guardianships.accountId eq accountId) and
                    Guardianships.deletedAt.isNull() and
                    (Guardianships.syncVersion greater cursor)
            }
            .map { it[Guardianships.childId] }

    fun newPatientIdsSince(accountId: UUID, cursor: Long): List<UUID> =
        PediatricianPatients.join(Children, JoinType.INNER, PediatricianPatients.childId, Children.id)
            .selectAll()
            .where { (PediatricianPatients.pediatricianAccountId eq accountId) and (Children.syncVersion greater cursor) }
            .map { it[PediatricianPatients.childId] }

    /** Niños a los que [accountId] perdió acceso después de [cursor]. */
    fun revokedChildIdsSince(accountId: UUID, cursor: Long): List<UUID> =
        AccessRevocations.selectAll()
            .where { (AccessRevocations.accountId eq accountId) and (AccessRevocations.syncVersion greater cursor) }
            .map { it[AccessRevocations.childId] }
}
