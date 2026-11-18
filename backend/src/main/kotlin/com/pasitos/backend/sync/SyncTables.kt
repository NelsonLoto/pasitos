package com.pasitos.backend.sync

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Avisos de que una cuenta perdió el acceso a un niño: la app borra sus datos locales cuando le llegan
 * como revokedChildIds (14 · Sincronización §2). No tiene claves foráneas: el aviso tiene que seguir
 * existiendo aunque se borre el niño o la cuenta. El esquema lo crea Flyway.
 */
object AccessRevocationsTable : Table("access_revocations") {
    val id = uuid("id")
    val accountId = uuid("account_id")
    val childId = uuid("child_id")
    val syncVersion = long("sync_version")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
