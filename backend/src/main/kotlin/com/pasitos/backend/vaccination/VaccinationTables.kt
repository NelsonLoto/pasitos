package com.pasitos.backend.vaccination

import com.pasitos.backend.common.AuthorRole
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object Vaccines : Table("vaccines") {
    val id = uuid("id")
    val name = text("name")
    val prevents = text("prevents").nullable()
    // "source" choca con ColumnSet.source de Exposed.
    val sourceText = text("source").nullable()
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

/** Estado de una versión del calendario: solo las publicadas se usan (RN-VAC-03). */
enum class CalendarVersionStatus { DRAFT, PUBLISHED, RETIRED }

object CalendarVersions : Table("calendar_versions") {
    val id = uuid("id")
    val effectiveFrom = date("effective_from")
    val status = enumerationByName<CalendarVersionStatus>("status", 16)
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object CalendarDoses : Table("calendar_doses") {
    val id = uuid("id")
    val versionId = uuid("version_id")
    val vaccineId = uuid("vaccine_id")
    val doseCode = text("dose_code")
    val doseLabel = text("dose_label")
    val ageMonths = integer("age_months")
    val leadDays = integer("lead_days")
    val toleranceDays = integer("tolerance_days")
    val bornFrom = date("born_from").nullable()
    val bornUntil = date("born_until").nullable()
    val sortOrder = integer("sort_order")
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

/**
 * Sincronizable de solo lectura: se escribe por REST (RNF-OFF-06). `sync_version` la asigna un
 * trigger y el borrado es lógico (`deleted_at`). [doseCode] es nulo en las dosis de campañas (I6).
 */
object AppliedDoses : Table("applied_doses") {
    val id = uuid("id")
    val childId = uuid("child_id")
    val doseCode = text("dose_code").nullable()
    val campaignId = uuid("campaign_id").nullable()
    val appliedOn = date("applied_on").nullable()
    val lot = text("lot").nullable()
    val place = text("place").nullable()
    val consultationId = uuid("consultation_id").nullable()
    val authorAccountId = uuid("author_account_id")
    val authorRole = enumerationByName<AuthorRole>("author_role", 16)
    val syncVersion = long("sync_version").databaseGenerated()
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

/** Sincronizable de solo lectura, como [AppliedDoses]. Sin estado ni avisos (RN-VAC-06). */
object OtherVaccines : Table("other_vaccines") {
    val id = uuid("id")
    val childId = uuid("child_id")
    val name = text("name")
    val appliedOn = date("applied_on")
    val lot = text("lot").nullable()
    val note = text("note").nullable()
    val authorAccountId = uuid("author_account_id")
    val authorRole = enumerationByName<AuthorRole>("author_role", 16)
    val syncVersion = long("sync_version").databaseGenerated()
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
