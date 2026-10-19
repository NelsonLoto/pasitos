package com.pasitos.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pasitos.app.core.sync.local.PendingMutationDao
import com.pasitos.app.core.sync.local.PendingMutationEntity
import com.pasitos.app.core.sync.local.SyncStateDao
import com.pasitos.app.core.sync.local.SyncStateEntity
import com.pasitos.app.data.child.local.ActiveChildEntity
import com.pasitos.app.data.child.local.ChildDao
import com.pasitos.app.data.child.local.ChildEntity
import com.pasitos.app.data.child.local.GuardianshipEntity
import com.pasitos.app.data.vaccination.local.AppliedDoseEntity
import com.pasitos.app.data.vaccination.local.OtherVaccineEntity
import com.pasitos.app.data.vaccination.local.VaccinationCalendarEntity
import com.pasitos.app.data.vaccination.local.VaccinationDao

/**
 * Base local de la app (ADR-02). Cada dominio registra acá sus entidades y su DAO, y sube la
 * versión. Hasta el piloto, un cambio de versión recrea la base (15 · App Android, guía para
 * agregar una pantalla), por eso no se exporta el esquema: se activa cuando haga falta escribir
 * migraciones.
 */
@Database(
    entities = [
        PendingMutationEntity::class,
        SyncStateEntity::class,
        ChildEntity::class,
        GuardianshipEntity::class,
        ActiveChildEntity::class,
        VaccinationCalendarEntity::class,
        AppliedDoseEntity::class,
        OtherVaccineEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class PasitosDatabase : RoomDatabase() {

    abstract fun pendingMutationDao(): PendingMutationDao

    abstract fun syncStateDao(): SyncStateDao

    abstract fun childDao(): ChildDao

    abstract fun vaccinationDao(): VaccinationDao

    companion object {
        const val NAME = "pasitos.db"
    }
}
