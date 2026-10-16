package com.pasitos.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pasitos.app.core.sync.local.PendingMutationDao
import com.pasitos.app.core.sync.local.PendingMutationEntity
import com.pasitos.app.core.sync.local.SyncStateDao
import com.pasitos.app.core.sync.local.SyncStateEntity

/**
 * Base local de la app (ADR-02). Cada dominio registra acá sus entidades y su DAO, y sube la
 * versión. Hasta el piloto, un cambio de versión recrea la base (15 · App Android, guía para
 * agregar una pantalla), por eso no se exporta el esquema: se activa cuando haga falta escribir
 * migraciones.
 */
@Database(
    entities = [PendingMutationEntity::class, SyncStateEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class PasitosDatabase : RoomDatabase() {

    abstract fun pendingMutationDao(): PendingMutationDao

    abstract fun syncStateDao(): SyncStateDao

    companion object {
        const val NAME = "pasitos.db"
    }
}
