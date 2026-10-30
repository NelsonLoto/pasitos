package com.pasitos.app.data.child.model

import com.pasitos.shared.time.formatAge
import java.time.LocalDate

enum class ChildSex { Female, Male }

/** Rol del usuario sobre un niño: un principal y cualquier cantidad de secundarios (RN-HIJ-06). */
enum class GuardianRole { PRINCIPAL, SECONDARY }

data class Child(
    val id: String,
    val firstName: String,
    val lastName: String,
    val sex: ChildSex,
    val birthDate: LocalDate,
    /** Nulo es "No lo sé", que equivale a 40 semanas (RN-HIJ-03). */
    val gestationalWeeks: Int? = null,
    val birthWeightKg: Double? = null,
    val birthLengthCm: Double? = null,
    val birthHeadCircumferenceCm: Double? = null,
    val allergies: String? = null,
    val relevantHistory: String? = null,
    /** Una de `AVATAR_KEYS` de `shared`, o nulo sin avatar (RN-HIJ-11). */
    val avatarKey: String? = null,
    /** Perfil finalizado: no admite mediciones nuevas (RF-CRE-01, CA6). */
    val finalized: Boolean = false,
) {
    val fullName: String get() = "$firstName $lastName"
}

/** Datos que el tutor carga en el alta o la edición (RF-HIJ-01 a 03). */
data class ChildInput(
    val firstName: String,
    val lastName: String,
    val sex: ChildSex,
    val birthDate: LocalDate,
    val gestationalWeeks: Int?,
    val birthWeightKg: Double? = null,
    val birthLengthCm: Double? = null,
    val birthHeadCircumferenceCm: Double? = null,
    val avatarKey: String? = null,
    /** Alergias y antecedentes solo se cargan al editar (RF-HIJ-03). */
    val allergies: String? = null,
    val relevantHistory: String? = null,
)

private val ChildSex.label: String
    get() = when (this) {
        ChildSex.Female -> "Niña"
        ChildSex.Male -> "Niño"
    }

/**
 * Edad según RN-HIJ-04 y RN-HIJ-05, con el formato compartido con el backend: "12 días", "4 meses y
 * 1 día · 2 meses corregidos", "3 años y 2 meses".
 */
fun Child.ageLabel(today: LocalDate = LocalDate.now()): String =
    // Si el reloj del teléfono atrasa, "hoy" podría quedar antes del nacimiento: se muestra 0 días.
    formatAge(birthDate, maxOf(today, birthDate), gestationalWeeks?.takeIf { it in 22..42 }).text

/** 3.25 → "3,25" y 50.0 → "50": con coma decimal, como lo escribe el tutor (RNF-LOC-02). */
fun Double.toDecimalText(): String = toBigDecimal().stripTrailingZeros().toPlainString().replace('.', ',')

/** Ej: "8 meses y 3 días · Niña". */
fun Child.ageAndSexLabel(today: LocalDate = LocalDate.now()): String = "${ageLabel(today)} · ${sex.label}"
