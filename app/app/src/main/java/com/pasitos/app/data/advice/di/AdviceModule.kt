package com.pasitos.app.data.advice.di

import com.pasitos.app.data.advice.AdviceRepository
import com.pasitos.app.data.advice.FakeAdviceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AdviceModule {

    @Binds
    @Singleton
    abstract fun bindAdviceRepository(impl: FakeAdviceRepository): AdviceRepository
}
