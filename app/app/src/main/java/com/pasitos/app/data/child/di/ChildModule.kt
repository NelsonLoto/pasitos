package com.pasitos.app.data.child.di

import com.pasitos.app.data.child.ChildApiRepository
import com.pasitos.app.data.child.ChildRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChildModule {

    @Binds
    @Singleton
    abstract fun bindChildRepository(impl: ChildApiRepository): ChildRepository
}
