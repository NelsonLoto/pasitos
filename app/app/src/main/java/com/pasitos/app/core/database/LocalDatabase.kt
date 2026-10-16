package com.pasitos.app.core.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.pasitos.app.core.security.DatabaseKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Base local cifrada (ADR-17). Al cerrar sesión, [wipe] borra el archivo y la clave, y la próxima
 * llamada a [get] crea una base nueva. Por eso los repositorios piden el DAO en cada uso
 * (`localDatabase.get().measurementDao()`) y no lo guardan en una propiedad: uno guardado
 * quedaría apuntando a la base cerrada.
 */
class LocalDatabase(
    private val context: Context,
    private val keyStore: DatabaseKeyStore,
    private val name: String = PasitosDatabase.NAME,
) {

    @Volatile
    private var instance: PasitosDatabase? = null

    fun get(): PasitosDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(context, PasitosDatabase::class.java, name)
            .openHelperFactory(EncryptedOpenHelperFactory(::key))
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
            .also { instance = it }
    }

    /** Borra la base y su clave (RF-CTA-07, CA1). Los datos vuelven del servidor al iniciar sesión. */
    suspend fun wipe() = withContext(Dispatchers.IO) {
        synchronized(this@LocalDatabase) {
            instance?.close()
            instance = null
            context.deleteDatabase(name)
        }
        keyStore.delete()
    }

    // La pide SQLCipher al abrir el archivo, siempre fuera del hilo principal
    private fun key(): ByteArray = runBlocking {
        keyStore.read() ?: run {
            // Sin una clave legible, el archivo que haya quedado ya no se puede abrir: se empieza
            // de cero y los datos vuelven del servidor en la próxima sincronización.
            if (context.deleteDatabase(name)) Log.w(TAG, "Base local recreada: no había una clave legible")
            keyStore.create()
        }
    }

    private companion object {
        const val TAG = "LocalDatabase"
    }
}
