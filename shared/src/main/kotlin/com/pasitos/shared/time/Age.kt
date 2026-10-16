package com.pasitos.shared.time

import java.time.LocalDate
import java.time.Period

/** Semanas de gestación de un nacimiento a término (RN-HIJ-03: "No lo sé" equivale a 40). */
const val TERM_WEEKS = 40

/** Por debajo de estas semanas de gestación el niño es prematuro (RN-HIJ-03). */
const val PRETERM_BELOW_WEEKS = 37

/** La edad corregida se usa hasta esta edad cronológica, en meses (RN-HIJ-05). */
const val CORRECTED_AGE_UNTIL_MONTHS = 24

/** Edad lista para mostrar. [corrected] existe solo en prematuros, mientras corresponde usarla. */
data class FormattedAge(
    val chronological: String,
    val corrected: String? = null,
) {
    /** "4 meses · 2 meses corregidos" (RF-CRE-03, CA2) o solo la edad cronológica. */
    val text: String get() = listOfNotNull(chronological, corrected).joinToString(" · ")
}

/**
 * Edad a la fecha [date] de un niño nacido el [birthDate] (RN-HIJ-04): en días durante el primer
 * mes, en meses y días hasta los 2 años, y en años y meses después.
 *
 * Si [gestationalWeeks] indica un prematuro, hasta los 24 meses de edad cronológica devuelve
 * también la edad corregida (RN-HIJ-05). Antes de llegar al término la edad corregida sería
 * negativa y no se informa.
 */
fun formatAge(birthDate: LocalDate, date: LocalDate, gestationalWeeks: Int? = null): FormattedAge {
    require(!date.isBefore(birthDate)) { "La fecha es anterior al nacimiento" }
    require(gestationalWeeks == null || gestationalWeeks in 22..42) { "Semanas de gestación fuera de 22 a 42" }

    val chronological = Period.between(birthDate, date)
    val corrected = correctedAge(birthDate, date, gestationalWeeks, chronological)?.let { amountsOf(it) }
    return FormattedAge(
        chronological = amountsOf(chronological).joinToString(" y "),
        corrected = corrected?.let { amounts ->
            val adjective = if (amounts.size == 1 && amounts.single().value == 1) "corregido" else "corregidos"
            "${amounts.joinToString(" y ")} $adjective"
        },
    )
}

private fun correctedAge(birthDate: LocalDate, date: LocalDate, gestationalWeeks: Int?, chronological: Period): Period? {
    if (gestationalWeeks == null || gestationalWeeks >= PRETERM_BELOW_WEEKS) return null
    if (chronological.toTotalMonths() >= CORRECTED_AGE_UNTIL_MONTHS) return null
    val termDate = birthDate.plusWeeks((TERM_WEEKS - gestationalWeeks).toLong())
    return if (date.isBefore(termDate)) null else Period.between(termDate, date)
}

private data class Amount(val value: Int, val singular: String, val plural: String) {
    override fun toString() = "$value ${if (value == 1) singular else plural}"
}

/** Las unidades de RN-HIJ-04 para esa edad; la segunda unidad se omite cuando vale cero. */
private fun amountsOf(age: Period): List<Amount> {
    val totalMonths = age.toTotalMonths().toInt()
    return when {
        totalMonths < 1 -> listOf(days(age.days))
        totalMonths < 24 -> listOfNotNull(months(totalMonths), age.days.takeIf { it > 0 }?.let(::days))
        else -> listOfNotNull(Amount(age.years, "año", "años"), age.months.takeIf { it > 0 }?.let(::months))
    }
}

private fun days(value: Int) = Amount(value, "día", "días")

private fun months(value: Int) = Amount(value, "mes", "meses")
