package com.pasitos.backend.measurement

import com.pasitos.backend.common.AuthorRole
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

/** Forma de medición de la longitud o talla (RN-CRE-03). */
enum class LengthPosition { LYING, STANDING }

/**
 * Solo los valores cargados: percentiles, puntaje Z y estado se calculan al leer (ADR-20).
 * Sincronizable: `sync_version` la asigna un trigger y el borrado es lógico (`deleted_at`).
 */
object Measurements : Table("measurements") {
    val id = uuid("id")
    val childId = uuid("child_id")
    val measuredOn = date("measured_on")
    val weightKg = decimal("weight_kg", 6, 3).nullable()
    val lengthCm = decimal("length_cm", 5, 1).nullable()
    val lengthPosition = enumerationByName<LengthPosition>("length_position", 16).nullable()
    val headCircumferenceCm = decimal("head_circumference_cm", 5, 1).nullable()
    val outlierConfirmed = bool("outlier_confirmed").default(false)
    val consultationId = uuid("consultation_id").nullable()
    val authorAccountId = uuid("author_account_id")
    val authorRole = enumerationByName<AuthorRole>("author_role", 16)
    val syncVersion = long("sync_version").databaseGenerated()
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
