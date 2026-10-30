package com.pasitos.app.data.child.di

import com.pasitos.app.core.database.PasitosDatabase
import com.pasitos.app.core.sync.SyncedEntity
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.child.ChildSyncedEntity
import com.pasitos.app.data.child.GuardianshipSyncedEntity
import com.pasitos.app.data.child.RoomChildRepository
import com.pasitos.app.data.child.local.ChildDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChildModule {

    @Binds
    @Singleton
    abstract fun bindChildRepository(impl: RoomChildRepository): ChildRepository

    @Binds
    @IntoSet
    abstract fun bindChildSyncedEntity(impl: ChildSyncedEntity): SyncedEntity

    @Binds
    @IntoSet
    abstract fun bindGuardianshipSyncedEntity(impl: GuardianshipSyncedEntity): SyncedEntity

    companion object {

        @Provides
        fun provideChildDao(database: PasitosDatabase): ChildDao = database.childDao()
    }
}
