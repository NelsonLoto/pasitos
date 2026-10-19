package com.pasitos.app.core.network.di

import com.pasitos.app.core.network.ConnectivityNetworkMonitor
import com.pasitos.app.core.network.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkMonitorModule {

    @Binds
    abstract fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor
}
