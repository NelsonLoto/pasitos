package com.pasitos.app.core.database

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.pasitos.app.core.security.DatabaseKeyStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Inject
import javax.inject.Singleton

/** Una base por proceso; tras logout el llamador cancela los trabajos y reinicia la app. */
@Singleton
class LocalDatabase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keys: DatabaseKeyStore,
) {
    private var cached: PasitosDatabase? = null

    @Synchronized
    fun get(): PasitosDatabase = cached ?: Room.databaseBuilder(context, PasitosDatabase::class.java, NAME)
        .openHelperFactory(SupportSQLiteOpenHelper.Factory { configuration ->
            check(Looper.myLooper() != Looper.getMainLooper()) { "La base debe abrirse fuera del hilo principal" }
            System.loadLibrary("sqlcipher")
            val material = runBlocking { keys.getOrCreate() }
            try {
                if (material.isNew) context.deleteDatabase(NAME)
                val raw = ("x'" + material.bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) } + "'")
                    .toByteArray(Charsets.UTF_8)
                SupportOpenHelperFactory(raw).create(configuration)
            } finally { material.bytes.fill(0) }
        })
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build().also { cached = it }

    /** Llamar después de cancelar SyncWorker. El reinicio elimina referencias viejas y la clave en RAM. */
    suspend fun wipe() = withContext(Dispatchers.IO) {
        synchronized(this@LocalDatabase) {
            cached?.close()
            cached = null
            context.deleteDatabase(NAME)
            runBlocking { keys.delete() }
        }
    }

    companion object { const val NAME = "pasitos.db" }
}
