package com.pasitos.backend.account

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Cuentas de la app (13 · Base de datos). El esquema lo crea Flyway: estos objetos solo lo describen
 * para Exposed y no se usan para crear tablas.
 *
 * - [email] se guarda en minúsculas y sin espacios: quien escribe o busca tiene que normalizarlo
 *   antes, o la base lo rechaza.
 * - [passwordHash] es un hash bcrypt, o nulo si la cuenta solo ingresa con Google.
 */
object AccountsTable : Table("accounts") {
    val id = uuid("id")
    val email = text("email")
    val emailVerifiedAt = timestamp("email_verified_at").nullable()
    val firstName = text("first_name")
    val lastName = text("last_name")
    val passwordHash = text("password_hash").nullable()
    val googleSubject = text("google_subject").nullable()
    val isGuardian = bool("is_guardian")
    val lastMode = text("last_mode").nullable()
    val failedLoginCount = integer("failed_login_count").default(0)
    val lockedUntil = timestamp("locked_until").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

/** Aceptación de términos, declaración de representación de un hijo y confidencialidad (RN-CTA-02). */
object ConsentsTable : Table("consents") {
    val id = uuid("id")
    val accountId = uuid("account_id")
    val type = text("type")
    val version = text("version")
    val childId = uuid("child_id").nullable()
    val acceptedAt = timestamp("accepted_at")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
