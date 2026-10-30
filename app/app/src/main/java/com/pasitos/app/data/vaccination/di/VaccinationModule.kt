package com.pasitos.app.data.vaccination.di

import com.pasitos.app.core.database.PasitosDatabase
import com.pasitos.app.core.sync.ReferenceData
import com.pasitos.app.core.sync.SyncedEntity
import com.pasitos.app.data.vaccination.AppliedDoseSyncedEntity
import com.pasitos.app.data.vaccination.OtherVaccineSyncedEntity
import com.pasitos.app.data.vaccination.RoomVaccinationRepository
import com.pasitos.app.data.vaccination.VaccinationCalendarReferenceData
import com.pasitos.app.data.vaccination.VaccinationRepository
import com.pasitos.app.data.vaccination.local.VaccinationDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VaccinationModule {

    @Binds
    @Singleton
    abstract fun bindVaccinationRepository(impl: RoomVaccinationRepository): VaccinationRepository

    @Binds
    @IntoSet
    abstract fun bindAppliedDoseSyncedEntity(impl: AppliedDoseSyncedEntity): SyncedEntity

    @Binds
    @IntoSet
    abstract fun bindOtherVaccineSyncedEntity(impl: OtherVaccineSyncedEntity): SyncedEntity

    @Binds
    @IntoSet
    abstract fun bindCalendarReferenceData(impl: VaccinationCalendarReferenceData): ReferenceData

    companion object {

        @Provides
        fun provideVaccinationDao(database: PasitosDatabase): VaccinationDao = database.vaccinationDao()
    }
}
