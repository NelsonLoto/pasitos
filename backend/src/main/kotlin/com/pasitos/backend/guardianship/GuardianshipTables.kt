package com.pasitos.backend.guardianship

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Tutores de cada niño (13 · Base de datos). Tabla sincronizable: se borra con [deletedAt] para que
 * la baja llegue a los teléfonos, y la base tiene un solo PRINCIPAL vigente por niño (RN-HIJ-06).
 * El esquema lo crea Flyway; [syncVersion] lo asigna el trigger de I0-08.
 */
object GuardianshipsTable : Table("guardianships") {
    val childId = uuid("child_id")
    val accountId = uuid("account_id")
    val role = text("role")
    val syncVersion = long("sync_version")
    val deletedAt = timestamp("deleted_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").nullable()

    override val primaryKey = PrimaryKey(childId, accountId)
}
