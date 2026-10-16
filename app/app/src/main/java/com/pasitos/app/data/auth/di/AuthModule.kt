package com.pasitos.app.data.auth.di

import com.pasitos.app.core.network.SessionTokens
import com.pasitos.app.data.auth.AuthApiRepository
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.auth.SessionRepository
import com.pasitos.app.data.auth.SessionRevocation
import com.pasitos.app.data.auth.WorkManagerSessionRevocation
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

    @Binds
    abstract fun bindSessionTokens(impl: SessionRepository): SessionTokens

    @Binds
    abstract fun bindSessionRevocation(impl: WorkManagerSessionRevocation): SessionRevocation
}
