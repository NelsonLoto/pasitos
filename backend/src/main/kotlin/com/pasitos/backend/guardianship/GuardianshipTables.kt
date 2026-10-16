package com.pasitos.backend.guardianship

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

enum class GuardianshipRole { PRINCIPAL, SECONDARY }

/** Relación tutor–niño (RN-HIJ-06). Sincronizable: `sync_version` la asigna un trigger. */
object Guardianships : Table("guardianships") {
    val childId = uuid("child_id")
    val accountId = uuid("account_id")
    val role = enumerationByName<GuardianshipRole>("role", 16)
    val syncVersion = long("sync_version").databaseGenerated()
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(childId, accountId)
}
