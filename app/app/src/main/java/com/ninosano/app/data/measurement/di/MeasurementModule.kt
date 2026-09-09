package com.ninosano.app.data.measurement.di

import com.ninosano.app.data.measurement.FakeMeasurementRepository
import com.ninosano.app.data.measurement.MeasurementRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MeasurementModule {

    @Binds
    @Singleton
    abstract fun bindMeasurementRepository(impl: FakeMeasurementRepository): MeasurementRepository
}
