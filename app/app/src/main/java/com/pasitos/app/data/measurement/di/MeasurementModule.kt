package com.pasitos.app.data.measurement.di

import com.pasitos.app.data.measurement.MeasurementApiRepository
import com.pasitos.app.data.measurement.MeasurementRepository
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
    abstract fun bindMeasurementRepository(impl: MeasurementApiRepository): MeasurementRepository
}
