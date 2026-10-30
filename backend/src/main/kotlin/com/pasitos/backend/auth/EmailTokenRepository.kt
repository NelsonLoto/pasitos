package com.pasitos.backend.auth

import com.pasitos.backend.common.toUtc
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

data class EmailToken(val id: UUID, val accountId: UUID, val newEmail: String? = null)

/** Todas las funciones asumen una transacción abierta. Los tokens llegan siempre como hash. */
class EmailTokenRepository {

    fun insert(accountId: UUID, purpose: EmailTokenPurpose, tokenHash: String, expiresAt: Instant, newEmail: String? = null): UUID {
        val id = UUID.randomUUID()
        EmailTokens.insert {
            it[EmailTokens.id] = id
            it[EmailTokens.accountId] = accountId
            it[EmailTokens.purpose] = purpose
            it[EmailTokens.tokenHash] = tokenHash
            it[EmailTokens.newEmail] = newEmail
            it[EmailTokens.expiresAt] = expiresAt.toUtc()
        }
        return id
    }

    /** El token vigente (sin usar y sin vencer) con ese hash y propósito, o null (RN-CTA-06). */
    fun findActive(tokenHash: String, purpose: EmailTokenPurpose, now: Instant): EmailToken? =
        EmailTokens.selectAll()
            .where {
                (EmailTokens.tokenHash eq tokenHash) and
                    (EmailTokens.purpose eq purpose) and
                    EmailTokens.usedAt.isNull() and
                    (EmailTokens.expiresAt greater now.toUtc())
            }
            .firstOrNull()
            ?.let { EmailToken(it[EmailTokens.id], it[EmailTokens.accountId], it[EmailTokens.newEmail]) }

    fun markUsed(id: UUID, at: Instant) {
        EmailTokens.update({ EmailTokens.id eq id }) {
            it[usedAt] = at.toUtc()
        }
    }
}
