package com.pasitos.app.data.child.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildSex
import com.pasitos.app.data.child.model.GuardianRole
import java.time.LocalDate

/** Copia local de un hijo, que llega por la sincronización o por la respuesta del alta y la edición. */
@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "last_name") val lastName: String,
    val sex: ChildSex,
    @ColumnInfo(name = "birth_date") val birthDate: LocalDate,
    /** Nulo es "No lo sé" (RN-HIJ-03). */
    @ColumnInfo(name = "gestational_weeks") val gestationalWeeks: Int?,
    @ColumnInfo(name = "birth_weight_kg") val birthWeightKg: Double?,
    @ColumnInfo(name = "birth_length_cm") val birthLengthCm: Double?,
    @ColumnInfo(name = "birth_head_circumference_cm") val birthHeadCircumferenceCm: Double?,
    val allergies: String?,
    @ColumnInfo(name = "relevant_history") val relevantHistory: String?,
    @ColumnInfo(name = "avatar_key") val avatarKey: String?,
) {
    fun toDomain() = Child(
        id = id,
        firstName = firstName,
        lastName = lastName,
        sex = sex,
        birthDate = birthDate,
        gestationalWeeks = gestationalWeeks,
        birthWeightKg = birthWeightKg,
        birthLengthCm = birthLengthCm,
        birthHeadCircumferenceCm = birthHeadCircumferenceCm,
        allergies = allergies,
        relevantHistory = relevantHistory,
        avatarKey = avatarKey,
    )
}

/**
 * Quién es tutor de cada niño y con qué rol. Sin claves foráneas: en una página de la
 * sincronización la tutoría puede llegar antes que el niño.
 */
@Entity(
    tableName = "guardianships",
    primaryKeys = ["child_id", "account_id"],
    indices = [Index(value = ["account_id"])],
)
data class GuardianshipEntity(
    @ColumnInfo(name = "child_id") val childId: String,
    @ColumnInfo(name = "account_id") val accountId: String,
    val role: GuardianRole,
)

/**
 * El último hijo que eligió el tutor (RF-HIJ-04, CA3). La tabla tiene una sola fila y vive en la
 * base local para que se borre con ella al cerrar sesión.
 */
@Entity(tableName = "active_child")
data class ActiveChildEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    @ColumnInfo(name = "child_id") val childId: String,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
