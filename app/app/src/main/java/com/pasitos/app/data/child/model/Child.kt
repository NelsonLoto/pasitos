package com.pasitos.app.data.child.model

import com.pasitos.shared.time.TERM_GESTATIONAL_WEEKS
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
 * Edad según RN-HIJ-04 y RN-HIJ-05, con [formatAge] de shared. Ej: "8 meses y 12 días".
 *
 * La app todavía no registra las semanas de gestación: hasta I1-A4 se usa el valor de "No lo sé"
 * (RN-HIJ-03), así que no se muestra edad corregida.
 *
 * Una fecha de nacimiento posterior a [today] solo puede aparecer si el reloj o la zona del teléfono
 * retrocedieron después del alta: se muestra "0 días" en lugar de cerrar la app.
 */
fun Child.ageLabel(today: LocalDate): String =
    formatAge(birthDate, maxOf(today, birthDate), TERM_GESTATIONAL_WEEKS)

/** Ej: "8 meses y 12 días · Niña". */
fun Child.ageAndSexLabel(today: LocalDate): String = "${ageLabel(today)} · ${sex.label}"
