package com.pasitos.app.core.sync.di

import com.pasitos.app.core.database.PasitosDatabase
import com.pasitos.app.core.sync.SyncScheduler
import com.pasitos.app.core.sync.WorkManagerSyncScheduler
import com.pasitos.app.core.sync.local.PendingMutationDao
import com.pasitos.app.core.sync.local.SyncStateDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    companion object {

        @Provides
        fun providePendingMutationDao(database: PasitosDatabase): PendingMutationDao =
            database.pendingMutationDao()

        @Provides
        fun provideSyncStateDao(database: PasitosDatabase): SyncStateDao = database.syncStateDao()
    }
}
