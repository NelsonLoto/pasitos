package com.pasitos.app.data.child.remote

import com.pasitos.app.data.child.local.ChildEntity
import com.pasitos.app.data.child.local.GuardianshipEntity
import com.pasitos.app.data.child.model.ChildSex
import com.pasitos.app.data.child.model.GuardianRole
import com.pasitos.shared.api.child.ChildResponse
import com.pasitos.shared.api.child.Sex
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.serialization.Serializable

/** Fila `CHILD` de la sincronización, tal como la arma `ChildSyncEntity` en el backend. */
@Serializable
internal data class ChildRow(
    val id: String,
    val firstName: String,
    val lastName: String,
    val sex: Sex,
    val birthDate: String,
    val gestationalWeeks: Int? = null,
    val birthWeightKg: Double? = null,
    val birthLengthCm: Double? = null,
    val birthHeadCircumferenceCm: Double? = null,
    val allergies: String? = null,
    val relevantHistory: String? = null,
    val avatarKey: String? = null,
    val deleted: Boolean = false,
) {
    fun toEntity() = ChildEntity(
        id = id,
        firstName = firstName,
        lastName = lastName,
        sex = sex.toChildSex(),
        birthDate = birthDate.toLocalDate(),
        gestationalWeeks = gestationalWeeks,
        birthWeightKg = birthWeightKg,
        birthLengthCm = birthLengthCm,
        birthHeadCircumferenceCm = birthHeadCircumferenceCm,
        allergies = allergies,
        relevantHistory = relevantHistory,
        avatarKey = avatarKey,
    )
}

/** Fila `GUARDIANSHIP` de la sincronización, tal como la arma `GuardianshipSyncEntity` en el backend. */
@Serializable
internal data class GuardianshipRow(
    val childId: String,
    val accountId: String,
    val role: GuardianRole,
    val deleted: Boolean = false,
) {
    fun toEntity() = GuardianshipEntity(childId = childId, accountId = accountId, role = role)
}

internal fun ChildResponse.toEntity() = ChildEntity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    sex = sex.toChildSex(),
    birthDate = birthDate.toLocalDate(),
    gestationalWeeks = gestationalWeeks,
    birthWeightKg = birthWeightKg,
    birthLengthCm = birthLengthCm,
    birthHeadCircumferenceCm = birthHeadCircumferenceCm,
    allergies = allergies,
    relevantHistory = relevantHistory,
    avatarKey = avatarKey,
)

internal fun Sex.toChildSex() = when (this) {
    Sex.FEMALE -> ChildSex.Female
    Sex.MALE -> ChildSex.Male
}

internal fun ChildSex.toSex() = when (this) {
    ChildSex.Female -> Sex.FEMALE
    ChildSex.Male -> Sex.MALE
}

/** Una fecha mal formada es una fila ilegible: la sincronización la saltea (`SyncedEntity.apply`). */
private fun String.toLocalDate(): LocalDate =
    try {
        LocalDate.parse(this)
    } catch (e: DateTimeParseException) {
        throw IllegalArgumentException("Fecha inválida", e)
    }
