package com.pasitos.backend.auth

import com.pasitos.backend.common.toUtc
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

data class Session(val id: UUID, val accountId: UUID, val expiresAt: Instant)

/** Todas las funciones asumen una transacción abierta. Los tokens llegan siempre como hash. */
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

    /** La sesión cuyo token de renovación vigente es [refreshTokenHash], bloqueada para rotarla. */
    fun findByRefreshTokenForUpdate(refreshTokenHash: String): Session? =
        Sessions.selectAll()
            .where { Sessions.refreshTokenHash eq refreshTokenHash }
            .forUpdate()
            .firstOrNull()
            ?.toSession()

    /** La sesión en la que [refreshTokenHash] ya se usó y fue reemplazado por otro. */
    fun findByPreviousRefreshToken(refreshTokenHash: String): Session? =
        Sessions.selectAll()
            .where { Sessions.previousRefreshTokenHash eq refreshTokenHash }
            .firstOrNull()
            ?.toSession()

    /** Reemplaza el token de renovación, guarda el anterior para detectar su reutilización y extiende el plazo. */
    fun rotate(id: UUID, newRefreshTokenHash: String, previousRefreshTokenHash: String, expiresAt: Instant, now: Instant) {
        Sessions.update({ Sessions.id eq id }) {
            it[refreshTokenHash] = newRefreshTokenHash
            it[Sessions.previousRefreshTokenHash] = previousRefreshTokenHash
            it[Sessions.expiresAt] = expiresAt.toUtc()
            it[lastUsedAt] = now.toUtc()
            it[updatedAt] = now.toUtc()
        }
    }

    fun delete(id: UUID): Int = Sessions.deleteWhere { Sessions.id eq id }

    private fun ResultRow.toSession() = Session(
        id = this[Sessions.id],
        accountId = this[Sessions.accountId],
        expiresAt = this[Sessions.expiresAt].toInstant(),
    )
}
