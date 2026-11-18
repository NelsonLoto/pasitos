package com.pasitos.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingMutationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(mutation: PendingMutation)

    @Query("SELECT * FROM pending_mutations ORDER BY createdAt, mutationId LIMIT :limit")
    suspend fun oldest(limit: Int = 100): List<PendingMutation>

    @Query("SELECT COUNT(*) FROM pending_mutations")
    fun observeCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM pending_mutations WHERE entity = :entity AND entityId = :entityId)")
    suspend fun hasPending(entity: String, entityId: String): Boolean

    @Query("DELETE FROM pending_mutations WHERE mutationId IN (:mutationIds)")
    suspend fun deleteConfirmed(mutationIds: List<String>)

    @Query("DELETE FROM pending_mutations WHERE childId = :childId")
    suspend fun deleteChild(childId: String)
}

@Dao
interface SyncStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(state: SyncState)

    @Query("SELECT * FROM sync_state WHERE id = 1")
    suspend fun read(): SyncState?

    @Query("SELECT * FROM sync_state WHERE id = 1")
    fun observe(): Flow<SyncState?>
}

