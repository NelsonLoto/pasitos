package com.pasitos.app.data.vaccination.remote

import com.pasitos.app.data.vaccination.local.AppliedDoseEntity
import com.pasitos.app.data.vaccination.local.OtherVaccineEntity
import com.pasitos.app.data.vaccination.model.CalendarDoseInfo
import com.pasitos.app.data.vaccination.model.FormerDose
import com.pasitos.app.data.vaccination.model.VaccinationCalendar
import com.pasitos.shared.api.AuthorRole
import com.pasitos.shared.api.vaccination.AppliedDoseResponse
import com.pasitos.shared.api.vaccination.OtherVaccineResponse
import com.pasitos.shared.api.vaccination.VaccinationCalendarResponse
import com.pasitos.shared.vaccination.CalendarDose
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.serialization.Serializable

/** Fila `APPLIED_DOSE` de la sincronización, tal como la arma `AppliedDoseSyncEntity` en el backend. */
@Serializable
internal data class AppliedDoseRow(
    val id: String,
    val childId: String,
    val doseCode: String,
    val appliedOn: String? = null,
    val lot: String? = null,
    val place: String? = null,
    val authorAccountId: String,
    val authorRole: AuthorRole,
    val deleted: Boolean = false,
) {
    fun toEntity() = AppliedDoseEntity(
        id = id,
        childId = childId,
        doseCode = doseCode,
        appliedOn = appliedOn?.toLocalDate(),
        lot = lot,
        place = place,
        authorAccountId = authorAccountId,
        authorRole = authorRole,
    )
}

/** Fila `OTHER_VACCINE` de la sincronización, tal como la arma `OtherVaccineSyncEntity` en el backend. */
@Serializable
internal data class OtherVaccineRow(
    val id: String,
    val childId: String,
    val name: String,
    val appliedOn: String,
    val lot: String? = null,
    val note: String? = null,
    val authorAccountId: String,
    val authorRole: AuthorRole,
    val deleted: Boolean = false,
) {
    fun toEntity() = OtherVaccineEntity(
        id = id,
        childId = childId,
        name = name,
        appliedOn = appliedOn.toLocalDate(),
        lot = lot,
        note = note,
        authorAccountId = authorAccountId,
        authorRole = authorRole,
    )
}

internal fun AppliedDoseResponse.toEntity() = AppliedDoseEntity(
    id = id,
    childId = childId,
    doseCode = doseCode,
    appliedOn = appliedOn?.toLocalDate(),
    lot = lot,
    place = place,
    authorAccountId = authorAccountId,
    authorRole = authorRole,
)

internal fun OtherVaccineResponse.toEntity() = OtherVaccineEntity(
    id = id,
    childId = childId,
    name = name,
    appliedOn = appliedOn.toLocalDate(),
    lot = lot,
    note = note,
    authorAccountId = authorAccountId,
    authorRole = authorRole,
)

/** El calendario de la API como lo usa la app, con los tipos de `shared/vaccination`. */
internal fun VaccinationCalendarResponse.toDomain() = VaccinationCalendar(
    effectiveFrom = effectiveFrom?.toLocalDate(),
    doses = doses.map { dose ->
        CalendarDoseInfo(
            dose = CalendarDose(
                doseCode = dose.doseCode,
                vaccineName = dose.vaccineName,
                doseLabel = dose.doseLabel,
                ageMonths = dose.ageMonths,
                leadDays = dose.leadDays,
                toleranceDays = dose.toleranceDays,
                sortOrder = dose.sortOrder,
                bornFrom = dose.bornFrom?.toLocalDate(),
                bornUntil = dose.bornUntil?.toLocalDate(),
            ),
            prevents = dose.prevents,
            source = dose.source,
        )
    },
    formerDoses = formerDoses.map { FormerDose(it.doseCode, it.vaccineName, it.doseLabel) },
)

/** Una fecha mal formada es una fila ilegible: la sincronización la saltea (`SyncedEntity.apply`). */
private fun String.toLocalDate(): LocalDate =
    try {
        LocalDate.parse(this)
    } catch (e: DateTimeParseException) {
        throw IllegalArgumentException("Fecha inválida", e)
    }
