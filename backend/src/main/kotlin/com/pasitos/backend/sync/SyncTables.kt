package com.pasitos.backend.sync

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

/**
 * Avisos de pérdida de acceso a un niño (14 · Sincronización §2). Una fila por cada vez que
 * [accountId] pierde el alcance de [childId] (vínculo terminado, invitación cancelada, etc.) -
 * no se borra: queda como historial para que la app sepa cuándo revocar, incluso si vuelve a
 * tener acceso después.
 */
object AccessRevocations : Table("access_revocations") {
    val id = uuid("id")
    val accountId = uuid("account_id")
    val childId = uuid("child_id")
    val syncVersion = long("sync_version").databaseGenerated()
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
