package com.ninosano.app.data.vaccination.di

import com.ninosano.app.data.vaccination.FakeVaccinationRepository
import com.ninosano.app.data.vaccination.VaccinationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VaccinationModule {

    @Binds
    @Singleton
    abstract fun bindVaccinationRepository(impl: FakeVaccinationRepository): VaccinationRepository
}
