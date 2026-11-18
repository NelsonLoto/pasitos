package com.pasitos.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [PendingMutation::class, SyncState::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PasitosDatabase : RoomDatabase() {
    abstract fun pendingMutations(): PendingMutationDao
    abstract fun syncState(): SyncStateDao
}
