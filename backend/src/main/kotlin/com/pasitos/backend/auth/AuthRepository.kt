package com.pasitos.backend.auth

import com.pasitos.backend.account.AccountsTable as A
import com.pasitos.backend.account.ConsentsTable as C
import com.pasitos.backend.auth.SessionsTable as S
import com.pasitos.shared.api.auth.AccountMode
import com.pasitos.shared.api.auth.AccountResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.util.UUID

internal data class AccountRow(val id: UUID, val passwordHash: String?, val failed: Int, val lockedUntil: Instant?) {
    override fun toString() = "AccountRow(id=$id)"
}

internal data class SessionRow(val id: UUID, val accountId: UUID, val hash: ByteArray, val expiresAt: Instant)

/** Solo consultas y mapeos. Las transacciones y reglas pertenecen a AuthService. */
class AuthRepository {
    internal fun accountByEmail(email: String): AccountRow? = A.selectAll().where { A.email eq email }
        .forUpdate().singleOrNull()?.let { AccountRow(it[A.id], it[A.passwordHash], it[A.failedLoginCount], it[A.lockedUntil]) }

    internal fun createAccount(id: UUID, first: String, last: String, email: String, hash: String) {
        A.insert {
            it[A.id] = id; it[firstName] = first; it[lastName] = last; it[A.email] = email
            it[passwordHash] = hash; it[isGuardian] = true; it[lastMode] = "GUARDIAN"
        }
    }

    internal fun consent(accountId: UUID, version: String, now: Instant) {
        C.insert {
            it[id] = UUID.randomUUID(); it[C.accountId] = accountId; it[type] = "TERMS"
            it[C.version] = version; it[acceptedAt] = now
        }
    }

    internal fun account(id: UUID, termsVersion: String): AccountResponse? {
        val row = A.selectAll().where { A.id eq id }.singleOrNull() ?: return null
        val accepted = C.selectAll().where { (C.accountId eq id) and (C.type eq "TERMS") and (C.version eq termsVersion) }.any()
        return AccountResponse(id.toString(), row[A.email], row[A.firstName], row[A.lastName],
            row[A.emailVerifiedAt] != null, row[A.passwordHash] != null, row[A.isGuardian],
            row[A.lastMode]?.let(AccountMode::valueOf), termsVersion, !accepted)
    }

    internal fun loginAttempt(id: UUID, failed: Int, lockedUntil: Instant?, now: Instant) {
        A.update({ A.id eq id }) { it[failedLoginCount] = failed; it[A.lockedUntil] = lockedUntil; it[updatedAt] = now }
    }

    internal fun insertSession(id: UUID, accountId: UUID, hash: ByteArray, deviceLabel: String?, now: Instant) {
        S.insert {
            it[S.id] = id; it[S.accountId] = accountId; it[refreshTokenHash] = hash
            it[expiresAt] = now.plus(AuthTokens.SESSION_LIFETIME); it[lastUsedAt] = now; it[S.deviceLabel] = deviceLabel
        }
    }

    internal fun sessionByToken(hash: ByteArray): SessionRow? = S.selectAll()
        .where { (S.refreshTokenHash eq hash) or (S.previousRefreshTokenHash eq hash) }
        .forUpdate().singleOrNull()?.let(::session)

    internal fun sessionById(id: UUID): SessionRow? = S.selectAll().where { S.id eq id }
        .forUpdate().singleOrNull()?.let(::session)

    private fun session(row: ResultRow) = SessionRow(row[S.id], row[S.accountId], row[S.refreshTokenHash], row[S.expiresAt])

    internal fun rotate(id: UUID, previous: ByteArray, replacement: ByteArray, now: Instant) {
        S.update({ S.id eq id }) {
            it[previousRefreshTokenHash] = previous; it[refreshTokenHash] = replacement
            it[expiresAt] = now.plus(AuthTokens.SESSION_LIFETIME); it[lastUsedAt] = now; it[updatedAt] = now
        }
    }

    internal fun touch(id: UUID, now: Instant) {
        S.update({ S.id eq id }) {
            it[lastUsedAt] = now; it[expiresAt] = now.plus(AuthTokens.SESSION_LIFETIME); it[updatedAt] = now
        }
    }

    internal fun deleteSession(id: UUID) { S.deleteWhere { S.id eq id } }
}
