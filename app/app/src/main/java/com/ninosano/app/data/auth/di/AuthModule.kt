package com.ninosano.app.data.auth.di

import com.ninosano.app.data.auth.AuthApiRepository
import com.ninosano.app.data.auth.AuthRepository
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
    abstract fun bindAuthRepository(impl: AuthApiRepository): AuthRepository
}
