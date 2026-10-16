package com.pasitos.app.core.process.di

import com.pasitos.app.core.process.AndroidProcessRestarter
import com.pasitos.app.core.process.ProcessRestarter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ProcessModule {

    @Binds
    abstract fun bindProcessRestarter(impl: AndroidProcessRestarter): ProcessRestarter
}
