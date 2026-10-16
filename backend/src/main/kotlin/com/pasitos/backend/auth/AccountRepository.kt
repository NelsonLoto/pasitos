package com.pasitos.backend.auth

import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

data class Account(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val passwordHash: String?,
) {
    val fullName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}

/** Todas las funciones asumen una transacción abierta. */
class AccountRepository {

    fun findByEmail(email: String): Account? =
        Accounts.selectAll().where { Accounts.email eq email }.firstOrNull()?.toAccount()

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

    private fun ResultRow.toAccount() = Account(
        id = this[Accounts.id],
        email = this[Accounts.email],
        firstName = this[Accounts.firstName],
        lastName = this[Accounts.lastName],
        passwordHash = this[Accounts.passwordHash],
    )
}
