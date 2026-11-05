package com.ninosano.app.data.child.di

import com.ninosano.app.data.child.ChildRepository
import com.ninosano.app.data.child.FakeChildRepository
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
    abstract fun bindChildRepository(impl: FakeChildRepository): ChildRepository
}
