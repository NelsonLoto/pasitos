package com.ninosano.app.data.notification.di

import com.ninosano.app.data.notification.FakeNotificationRepository
import com.ninosano.app.data.notification.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: FakeNotificationRepository): NotificationRepository
}
