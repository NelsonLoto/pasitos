package com.pasitos.app.core.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.pasitos.app.core.security.DatabaseKeyStore
import java.security.GeneralSecurityException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Base local cifrada (ADR-17), una sola por proceso. Hilt la inyecta, y también sus DAOs.
 *
 * Al cerrar sesión, [wipe] borra el archivo y la clave, y después la app se reinicia (I0-06). Así
 * no queda nada de la sesión anterior: ni en el teléfono ni en memoria (15 · App Android,
 * sección 3).
 */
class LocalDatabase(
    private val context: Context,
    private val keyStore: DatabaseKeyStore,
    private val name: String = PasitosDatabase.NAME,
) {

    private val lazyDatabase = lazy {
        Room.databaseBuilder(context, PasitosDatabase::class.java, name)
            .openHelperFactory(EncryptedOpenHelperFactory(::key))
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    val database: PasitosDatabase by lazyDatabase

    /**
     * Cierra la base y borra el archivo y la clave (RF-CTA-07, CA1). Después [database] ya no se
     * puede usar: quien la llama reinicia el proceso.
     */
    suspend fun wipe() = withContext(Dispatchers.IO) {
        if (lazyDatabase.isInitialized()) database.close()
        context.deleteDatabase(name)
        keyStore.delete()
    }

    // La pide SQLCipher al abrir el archivo, siempre fuera del hilo principal
    private fun key(): ByteArray = runBlocking {
        keyStore.read() ?: startOver()
    }

    /** Sin una clave legible, el archivo que haya quedado ya no se puede abrir: se empieza de cero. */
    private suspend fun startOver(): ByteArray {
        if (context.deleteDatabase(name)) Log.w(TAG, "Base local recreada: no había una clave legible")
        return try {
            keyStore.create()
        } catch (e: GeneralSecurityException) {
            // El cifrado mismo dejó de funcionar: se regeneran todas las claves. Los datos vuelven
            // del servidor; lo cargado sin conexión y no enviado ya no se podía recuperar.
            Log.w(TAG, "Claves de cifrado regeneradas: el Keystore no podía usar las anteriores", e)
            keyStore.resetEncryption()
            keyStore.create()
        }
    }

    private companion object {
        const val TAG = "LocalDatabase"
    }
}
