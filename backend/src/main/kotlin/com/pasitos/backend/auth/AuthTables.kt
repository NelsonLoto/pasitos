package com.pasitos.backend.auth

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object Accounts : Table("accounts") {
    val id = uuid("id")
    val email = text("email")
    val emailVerifiedAt = timestampWithTimeZone("email_verified_at").nullable()
    val firstName = text("first_name")
    val lastName = text("last_name")
    val passwordHash = text("password_hash").nullable()
    val googleSubject = text("google_subject").nullable()
    val isGuardian = bool("is_guardian").default(false)
    val lastMode = text("last_mode").nullable()
    val failedLoginCount = integer("failed_login_count").default(0)
    val lockedUntil = timestampWithTimeZone("locked_until").nullable()
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

/** Un login activo por fila; revocar una sesión es borrar su fila (ADR-11). */
object Sessions : Table("sessions") {
    val id = uuid("id")
    val accountId = uuid("account_id")
    val refreshTokenHash = text("refresh_token_hash")
    val previousRefreshTokenHash = text("previous_refresh_token_hash").nullable()
    val expiresAt = timestampWithTimeZone("expires_at")
    val lastUsedAt = timestampWithTimeZone("last_used_at")
    val deviceLabel = text("device_label").nullable()
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

enum class ConsentType { TERMS, GUARDIAN_DECLARATION, CONFIDENTIALITY }

/** Consentimientos con su versión y momento de aceptación (RN-CTA-02). */
object Consents : Table("consents") {
    val id = uuid("id")
    val accountId = uuid("account_id")
    val type = enumerationByName<ConsentType>("type", 32)
    val version = text("version")
    val childId = uuid("child_id").nullable()
    val acceptedAt = timestampWithTimeZone("accepted_at")
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
