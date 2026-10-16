package com.pasitos.app.core.security.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pasitos.app.core.security.KeystoreCipher
import com.pasitos.app.core.security.LocalCipher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * DataStore para secretos ya cifrados con [LocalCipher]: el archivo no está cifrado, cada valor sí.
 * Está excluido de las copias de seguridad (`res/xml/backup_rules.xml`).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SecretsDataStore

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    abstract fun bindLocalCipher(impl: KeystoreCipher): LocalCipher

    companion object {
        private const val SECRETS_FILE = "secrets"

        @Provides
        @Singleton
        @SecretsDataStore
        fun provideSecretsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(SECRETS_FILE) }
    }
}
