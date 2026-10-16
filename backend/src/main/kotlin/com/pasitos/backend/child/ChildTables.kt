package com.pasitos.backend.child

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

/** RN-HIJ-02: femenino o masculino, porque las tablas de crecimiento se separan por sexo. */
enum class Sex { FEMALE, MALE }

enum class ChildStatus { ACTIVE, FINALIZED }

/** Sincronizable: `sync_version` la asigna un trigger y el borrado es lógico (`deleted_at`). */
object Children : Table("children") {
    val id = uuid("id")
    val firstName = text("first_name")
    val lastName = text("last_name")
    val sex = enumerationByName<Sex>("sex", 8)
    val birthDate = date("birth_date")
    val gestationalWeeks = integer("gestational_weeks").nullable()
    val gestationalWeeksUnknown = bool("gestational_weeks_unknown").default(false)
    val birthWeightKg = decimal("birth_weight_kg", 6, 3).nullable()
    val birthLengthCm = decimal("birth_length_cm", 5, 1).nullable()
    val birthHeadCircumferenceCm = decimal("birth_head_circumference_cm", 5, 1).nullable()
    val allergies = text("allergies").nullable()
    val relevantHistory = text("relevant_history").nullable()
    val avatarKey = text("avatar_key").nullable()
    val status = enumerationByName<ChildStatus>("status", 16).default(ChildStatus.ACTIVE)
    val createdByAccountId = uuid("created_by_account_id").nullable()
    val syncVersion = long("sync_version").databaseGenerated()
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
