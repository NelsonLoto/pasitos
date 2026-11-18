package com.pasitos.app.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/** Cambio pendiente. El payload nunca se registra en logs. */
@Entity(tableName = "pending_mutations", indices = [Index("childId"), Index("createdAt")])
data class PendingMutation(
    @PrimaryKey val mutationId: String,
    val entity: String,
    val entityId: String,
    val childId: String,
    val operation: String,
    val payload: String,
    val createdAt: Instant,
)

@Entity(tableName = "sync_state")
data class SyncState(
    @PrimaryKey val id: Int = 1,
    val cursor: Long = 0,
    val lastSyncedAt: Instant? = null,
)
