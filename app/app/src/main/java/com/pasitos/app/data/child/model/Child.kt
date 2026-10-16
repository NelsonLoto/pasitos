package com.pasitos.app.data.child.model

import com.pasitos.shared.time.formatAge
import java.time.LocalDate

enum class ChildSex { Female, Male }

data class Child(
    val id: String,
    val name: String,
    val birthDate: LocalDate,
    val sex: ChildSex,
)

private val ChildSex.label: String
    get() = when (this) {
        ChildSex.Female -> "Niña"
        ChildSex.Male -> "Niño"
    }

/**
 * Edad según RN-HIJ-04, con el formato compartido con el backend: "12 días", "4 meses y 1 día",
 * "3 años y 2 meses". La app todavía no conoce las semanas de gestación, así que no muestra la
 * edad corregida.
 */
fun Child.ageLabel(today: LocalDate = LocalDate.now()): String =
    // Si el reloj del teléfono atrasa, "hoy" podría quedar antes del nacimiento: se muestra 0 días.
    formatAge(birthDate, maxOf(today, birthDate)).text

/** Ej: "8 meses y 3 días · Niña". */
fun Child.ageAndSexLabel(today: LocalDate = LocalDate.now()): String = "${ageLabel(today)} · ${sex.label}"
