package com.pasitos.backend.auth

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object Accounts : Table("accounts") {
    val id = uuid("id")
    val email = text("email")
    val emailVerifiedAt = timestampWithTimeZone("email_verified_at").nullable()
    val firstName = text("first_name")
    val lastName = text("last_name")
    val dni = text("dni").nullable()
    val licenseNumber = text("license_number").nullable()
    val specialty = text("specialty").nullable()
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

enum class EmailTokenPurpose { VERIFY_EMAIL, RESET_PASSWORD, CHANGE_EMAIL }

/** Tokens de un solo uso de los links de email (RN-CTA-06). Solo se guarda su hash. */
object EmailTokens : Table("email_tokens") {
    val id = uuid("id")
    val accountId = uuid("account_id")
    val purpose = enumerationByName<EmailTokenPurpose>("purpose", 32)
    val tokenHash = text("token_hash")
    val newEmail = text("new_email").nullable()
    val expiresAt = timestampWithTimeZone("expires_at")
    val usedAt = timestampWithTimeZone("used_at").nullable()
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
