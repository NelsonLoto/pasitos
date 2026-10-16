package com.pasitos.backend.auth

import com.pasitos.backend.common.toUtc
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

/** Todas las funciones asumen una transacción abierta. */
class SessionRepository {

    fun insert(id: UUID, accountId: UUID, refreshTokenHash: String, expiresAt: Instant, now: Instant) {
        Sessions.insert {
            it[Sessions.id] = id
            it[Sessions.accountId] = accountId
            it[Sessions.refreshTokenHash] = refreshTokenHash
            it[Sessions.expiresAt] = expiresAt.toUtc()
            it[lastUsedAt] = now.toUtc()
        }
    }

    fun isActive(id: UUID, now: Instant): Boolean =
        !Sessions.selectAll()
            .where { (Sessions.id eq id) and (Sessions.expiresAt greater now.toUtc()) }
            .empty()

    fun delete(id: UUID): Int = Sessions.deleteWhere { Sessions.id eq id }
}
