package com.pasitos.backend.auth

import com.pasitos.backend.common.toUtc
import com.pasitos.shared.api.auth.AccountDto
import com.pasitos.shared.api.auth.AccountMode
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

data class Account(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val passwordHash: String?,
    val googleSubject: String? = null,
    val emailVerifiedAt: Instant? = null,
    val isGuardian: Boolean = true,
    val lastMode: AccountMode? = null,
    val failedLoginCount: Int = 0,
    val lockedUntil: Instant? = null,
)

/** La cuenta como la ve la app: en login, registro, `GET /me` y `PATCH /me`. */
fun Account.toDto() = AccountDto(
    id = id.toString(),
    email = email,
    firstName = firstName,
    lastName = lastName,
    emailVerified = emailVerifiedAt != null,
    isGuardian = isGuardian,
    lastMode = lastMode,
    hasPassword = passwordHash != null,
)

/** Todas las funciones asumen una transacción abierta. */
class AccountRepository {

    fun findByEmail(email: String): Account? =
        Accounts.selectAll().where { Accounts.email eq email }.firstOrNull()?.toAccount()

    /**
     * Igual que [findByEmail], pero bloquea la fila hasta el fin de la transacción: dos intentos de
     * login simultáneos no pueden pisarse el contador de intentos fallidos.
     */
    fun findByEmailForUpdate(email: String): Account? =
        Accounts.selectAll().where { Accounts.email eq email }.forUpdate().firstOrNull()?.toAccount()

    fun findById(id: UUID): Account? =
        Accounts.selectAll().where { Accounts.id eq id }.firstOrNull()?.toAccount()

    /** RF-CTA-03: la cuenta que ya ingresó con esa cuenta de Google, aunque su email haya cambiado. */
    fun findByGoogleSubject(googleSubject: String): Account? =
        Accounts.selectAll().where { Accounts.googleSubject eq googleSubject }.firstOrNull()?.toAccount()

    /** RF-CTA-03, CA1: cuenta de tutor creada con Google, sin contraseña y con el email ya verificado. */
    fun insertGoogleGuardian(email: String, firstName: String, lastName: String, googleSubject: String, verifiedAt: Instant): Account {
        val id = UUID.randomUUID()
        Accounts.insert {
            it[Accounts.id] = id
            it[Accounts.email] = email
            it[Accounts.firstName] = firstName
            it[Accounts.lastName] = lastName
            it[Accounts.googleSubject] = googleSubject
            it[emailVerifiedAt] = verifiedAt.toUtc()
            it[isGuardian] = true
        }
        return Account(id, email, firstName, lastName, passwordHash = null, googleSubject = googleSubject, emailVerifiedAt = verifiedAt)
    }

    /** RN-CTA-04: asocia el inicio con Google a una cuenta que ya verificó su email. */
    fun linkGoogle(id: UUID, googleSubject: String) {
        Accounts.update({ Accounts.id eq id }) {
            it[Accounts.googleSubject] = googleSubject
        }
    }

    /**
     * RN-CTA-04, cuenta sin verificar: Google demuestra que la persona controla el email, así que
     * queda verificado y se borra la contraseña, que pudo cargar otra persona antes de la
     * verificación. Las sesiones las cierra quien llama.
     */
    fun linkGoogleReplacingPassword(id: UUID, googleSubject: String, verifiedAt: Instant) {
        Accounts.update({ Accounts.id eq id }) {
            it[Accounts.googleSubject] = googleSubject
            it[emailVerifiedAt] = verifiedAt.toUtc()
            it[passwordHash] = null
            it[failedLoginCount] = 0
            it[lockedUntil] = null
        }
    }

    fun insertGuardian(email: String, firstName: String, lastName: String, passwordHash: String): Account {
        val id = UUID.randomUUID()
        Accounts.insert {
            it[Accounts.id] = id
            it[Accounts.email] = email
            it[Accounts.firstName] = firstName
            it[Accounts.lastName] = lastName
            it[Accounts.passwordHash] = passwordHash
            it[isGuardian] = true
        }
        return Account(id, email, firstName, lastName, passwordHash)
    }

    /** Guarda los intentos fallidos consecutivos y, si corresponde, hasta cuándo queda bloqueada. */
    fun updateLoginFailures(id: UUID, failedLoginCount: Int, lockedUntil: Instant?) {
        Accounts.update({ Accounts.id eq id }) {
            it[Accounts.failedLoginCount] = failedLoginCount
            it[Accounts.lockedUntil] = lockedUntil?.toUtc()
        }
    }

    /** RF-CTA-02, CA2: el link de verificación vigente marca el email como verificado. */
    fun markEmailVerified(id: UUID, at: Instant) {
        Accounts.update({ Accounts.id eq id }) {
            it[emailVerifiedAt] = at.toUtc()
        }
    }

    /** RF-CTA-05, CA2 y RF-CTA-11, CA2: reemplaza el hash guardado. */
    fun updatePassword(id: UUID, passwordHash: String) {
        Accounts.update({ Accounts.id eq id }) {
            it[Accounts.passwordHash] = passwordHash
        }
    }

    /** RF-CTA-11, CA1. */
    fun updateName(id: UUID, firstName: String, lastName: String) {
        Accounts.update({ Accounts.id eq id }) {
            it[Accounts.firstName] = firstName
            it[Accounts.lastName] = lastName
        }
    }

    /** RF-CTA-11, CA1: al confirmar el cambio de email (`/web/confirm-email-change`). */
    fun updateEmail(id: UUID, email: String) {
        Accounts.update({ Accounts.id eq id }) {
            it[Accounts.email] = email
        }
    }

    private fun ResultRow.toAccount() = Account(
        id = this[Accounts.id],
        email = this[Accounts.email],
        firstName = this[Accounts.firstName],
        lastName = this[Accounts.lastName],
        passwordHash = this[Accounts.passwordHash],
        googleSubject = this[Accounts.googleSubject],
        emailVerifiedAt = this[Accounts.emailVerifiedAt]?.toInstant(),
        isGuardian = this[Accounts.isGuardian],
        lastMode = this[Accounts.lastMode]?.let(AccountMode::valueOf),
        failedLoginCount = this[Accounts.failedLoginCount],
        lockedUntil = this[Accounts.lockedUntil]?.toInstant(),
    )
}
