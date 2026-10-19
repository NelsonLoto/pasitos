package com.pasitos.app.data.vaccination.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pasitos.app.data.vaccination.model.AppliedDoseRecord
import com.pasitos.app.data.vaccination.model.OtherVaccine
import com.pasitos.shared.api.AuthorRole
import java.time.LocalDate

/**
 * El calendario vigente tal como lo devolvió `GET /vaccination-calendar`, en una sola fila que se
 * reemplaza entera. La app siempre usa el calendario completo, así que no gana nada separándolo en
 * tablas. Con [etag] el próximo pedido recibe `304` si no cambió.
 */
@Entity(tableName = "vaccination_calendar")
data class VaccinationCalendarEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val etag: String?,
    /** `VaccinationCalendarResponse` de `shared/api/vaccination`, en JSON. */
    val json: String,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}

/** Copia local de una dosis aplicada, que llega por la sincronización o por la respuesta del registro. */
@Entity(tableName = "applied_doses", indices = [Index("child_id")])
data class AppliedDoseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "child_id") val childId: String,
    @ColumnInfo(name = "dose_code") val doseCode: String,
    @ColumnInfo(name = "applied_on") val appliedOn: LocalDate?,
    val lot: String?,
    val place: String?,
    @ColumnInfo(name = "author_account_id") val authorAccountId: String,
    @ColumnInfo(name = "author_role") val authorRole: AuthorRole,
) {
    fun toDomain() = AppliedDoseRecord(
        id = id,
        childId = childId,
        doseCode = doseCode,
        appliedOn = appliedOn,
        lot = lot,
        place = place,
        authorAccountId = authorAccountId,
        authorRole = authorRole,
    )
}

/** Copia local de otra vacuna (RF-VAC-05). */
@Entity(tableName = "other_vaccines", indices = [Index("child_id")])
data class OtherVaccineEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "child_id") val childId: String,
    val name: String,
    @ColumnInfo(name = "applied_on") val appliedOn: LocalDate,
    val lot: String?,
    val note: String?,
    @ColumnInfo(name = "author_account_id") val authorAccountId: String,
    @ColumnInfo(name = "author_role") val authorRole: AuthorRole,
) {
    fun toDomain() = OtherVaccine(
        id = id,
        childId = childId,
        name = name,
        appliedOn = appliedOn,
        lot = lot,
        note = note,
        authorAccountId = authorAccountId,
        authorRole = authorRole,
    )
}
