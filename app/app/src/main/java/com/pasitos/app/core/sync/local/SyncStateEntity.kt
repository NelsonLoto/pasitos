package com.pasitos.app.core.sync.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** Estado de la sincronización con el servidor. La tabla tiene una sola fila. */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    /** Mayor `sync_version` ya recibida; se envía como `cursor` en `GET /sync/changes`. */
    val cursor: Long = 0,
    /** Última sincronización completa sin errores, que la app muestra en Perfil (RNF-OFF-04). */
    @ColumnInfo(name = "last_synced_at") val lastSyncedAt: Instant? = null,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
