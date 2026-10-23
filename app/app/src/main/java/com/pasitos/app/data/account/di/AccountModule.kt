package com.pasitos.app.data.account.di

import com.pasitos.app.data.account.AccountApiRepository
import com.pasitos.app.data.account.AccountRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {

    @Binds
    abstract fun bindAccountRepository(impl: AccountApiRepository): AccountRepository
}
