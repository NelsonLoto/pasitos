package com.pasitos.app.core.sync.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStateDao {

    /** Nulo si el teléfono todavía no sincronizó nunca. */
    @Query("SELECT * FROM sync_state WHERE id = ${SyncStateEntity.SINGLE_ROW_ID}")
    suspend fun get(): SyncStateEntity?

    @Query("SELECT * FROM sync_state WHERE id = ${SyncStateEntity.SINGLE_ROW_ID}")
    fun observe(): Flow<SyncStateEntity?>

    @Upsert
    suspend fun save(state: SyncStateEntity)
}
