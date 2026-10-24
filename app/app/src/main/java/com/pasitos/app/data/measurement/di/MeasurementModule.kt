package com.pasitos.app.data.measurement.di

import com.pasitos.app.core.database.PasitosDatabase
import com.pasitos.app.core.sync.OutboxEntity
import com.pasitos.app.core.sync.SyncedEntity
import com.pasitos.app.data.measurement.MeasurementRepository
import com.pasitos.app.data.measurement.MeasurementSyncedEntity
import com.pasitos.app.data.measurement.RoomMeasurementRepository
import com.pasitos.app.data.measurement.local.MeasurementDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MeasurementModule {

    @Binds
    @Singleton
    abstract fun bindMeasurementRepository(impl: RoomMeasurementRepository): MeasurementRepository

    @Binds
    @IntoSet
    abstract fun bindMeasurementSyncedEntity(impl: MeasurementSyncedEntity): SyncedEntity

    @Binds
    @IntoSet
    abstract fun bindMeasurementOutboxEntity(impl: MeasurementSyncedEntity): OutboxEntity

    companion object {

        @Provides
        fun provideMeasurementDao(database: PasitosDatabase): MeasurementDao = database.measurementDao()
    }
}
