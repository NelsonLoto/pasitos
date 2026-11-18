package com.pasitos.app.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.secrets by preferencesDataStore(
    name = "secrets",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun secrets(@ApplicationContext context: Context): DataStore<Preferences> = context.secrets

    @Provides @Singleton
    fun database(local: LocalDatabase): PasitosDatabase = local.get()

    @Provides fun pendingMutations(database: PasitosDatabase): PendingMutationDao = database.pendingMutations()
    @Provides fun syncState(database: PasitosDatabase): SyncStateDao = database.syncState()
}
