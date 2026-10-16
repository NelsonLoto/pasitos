package com.pasitos.app.core.database.di

import android.content.Context
import com.pasitos.app.core.database.LocalDatabase
import com.pasitos.app.core.database.PasitosDatabase
import com.pasitos.app.core.security.DatabaseKeyStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Cada dominio provee sus DAOs en su propio módulo (`data/x/di/`) a partir de [PasitosDatabase]. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLocalDatabase(
        @ApplicationContext context: Context,
        keyStore: DatabaseKeyStore,
    ): LocalDatabase = LocalDatabase(context, keyStore)

    @Provides
    fun provideDatabase(localDatabase: LocalDatabase): PasitosDatabase = localDatabase.database
}
