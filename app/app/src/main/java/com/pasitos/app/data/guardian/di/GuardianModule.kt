package com.pasitos.app.data.guardian.di

import com.pasitos.app.data.guardian.GuardianApiRepository
import com.pasitos.app.data.guardian.GuardianRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class GuardianModule {

    @Binds
    abstract fun bindGuardianRepository(impl: GuardianApiRepository): GuardianRepository
}
