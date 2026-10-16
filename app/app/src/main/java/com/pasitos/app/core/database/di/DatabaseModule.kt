package com.pasitos.app.core.database.di

import android.content.Context
import com.pasitos.app.core.database.LocalDatabase
import com.pasitos.app.core.security.DatabaseKeyStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Se expone [LocalDatabase] y no los DAOs: un DAO inyectado en un singleton sobreviviría al
 * cierre de sesión apuntando a la base borrada.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLocalDatabase(
        @ApplicationContext context: Context,
        keyStore: DatabaseKeyStore,
    ): LocalDatabase = LocalDatabase(context, keyStore)
}
