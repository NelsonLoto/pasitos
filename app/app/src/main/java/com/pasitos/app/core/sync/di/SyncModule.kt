package com.pasitos.app.core.sync.di

import com.pasitos.app.core.database.PasitosDatabase
import com.pasitos.app.core.sync.OutboxEntity
import com.pasitos.app.core.sync.ReferenceData
import com.pasitos.app.core.sync.RoomSyncStatus
import com.pasitos.app.core.sync.SyncScheduler
import com.pasitos.app.core.sync.SyncStatus
import com.pasitos.app.core.sync.SyncedEntity
import com.pasitos.app.core.sync.WorkManagerSyncScheduler
import com.pasitos.app.core.sync.local.PendingMutationDao
import com.pasitos.app.core.sync.local.SyncStateDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    @Binds
    abstract fun bindSyncStatus(impl: RoomSyncStatus): SyncStatus

    /** Cada dominio agrega su [SyncedEntity] con `@IntoSet`; declararlo acá permite que el conjunto empiece vacío. */
    @Multibinds
    abstract fun syncedEntities(): Set<SyncedEntity>

    /** Las entidades que se escriben en el teléfono agregan su [OutboxEntity] con `@IntoSet` (ADR-28). */
    @Multibinds
    abstract fun outboxEntities(): Set<OutboxEntity>

    /** Igual que [syncedEntities]: cada dominio agrega sus datos de referencia con `@IntoSet`. */
    @Multibinds
    abstract fun referenceData(): Set<ReferenceData>

    companion object {

        @Provides
        fun providePendingMutationDao(database: PasitosDatabase): PendingMutationDao =
            database.pendingMutationDao()

        @Provides
        fun provideSyncStateDao(database: PasitosDatabase): SyncStateDao = database.syncStateDao()
    }
}
