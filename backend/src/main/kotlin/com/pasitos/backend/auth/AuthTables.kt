package com.pasitos.backend.auth

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Sesiones de la app (11 · Backend §3, 13 · Base de datos). El esquema lo crea Flyway.
 *
 * Los tokens de renovación nunca se guardan: [refreshTokenHash] y [previousRefreshTokenHash] son su
 * hash SHA-256, de 32 bytes exactos. La base rechaza cualquier otro largo.
 */
object SessionsTable : Table("sessions") {
    val id = uuid("id")
    val accountId = uuid("account_id")
    val refreshTokenHash = binary("refresh_token_hash", SHA256_BYTES)
    val previousRefreshTokenHash = binary("previous_refresh_token_hash", SHA256_BYTES).nullable()
    val expiresAt = timestamp("expires_at")
    val lastUsedAt = timestamp("last_used_at")
    val deviceLabel = text("device_label").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

/** Largo de un hash SHA-256, el único que aceptan las columnas de hashes de tokens. */
const val SHA256_BYTES: Int = 32
