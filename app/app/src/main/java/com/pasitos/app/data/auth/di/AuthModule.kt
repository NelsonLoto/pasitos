package com.pasitos.app.data.auth.di

import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.auth.RemoteAuthRepository
import com.pasitos.app.data.auth.SessionEffects
import com.pasitos.app.data.auth.AppSessionEffects
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: RemoteAuthRepository): AuthRepository

    @Binds @Singleton
    abstract fun effects(impl: AppSessionEffects): SessionEffects
}
