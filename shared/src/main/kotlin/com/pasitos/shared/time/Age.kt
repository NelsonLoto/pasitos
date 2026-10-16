package com.pasitos.shared.time

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/** Semanas de gestación de un nacimiento a término (RN-HIJ-03: "No lo sé" equivale a 40). */
const val TERM_WEEKS = 40

/** Por debajo de estas semanas de gestación el niño es prematuro (RN-HIJ-03). */
const val PRETERM_BELOW_WEEKS = 37

/** La edad corregida se usa hasta esta edad cronológica, en meses (RN-HIJ-05). */
const val CORRECTED_AGE_UNTIL_MONTHS = 24

/**
 * Edad lista para mostrar. En un prematuro, [untilTerm] se informa antes de su fecha de término y
 * [corrected] desde esa fecha hasta los 24 meses; nunca los dos a la vez.
 */
data class FormattedAge(
    val chronological: String,
    val corrected: String? = null,
    val untilTerm: String? = null,
) {
    /**
     * "4 meses · 2 meses corregidos" (RF-CRE-03, CA2), "14 días · faltan 8 semanas para su fecha
     * de término" o solo la edad cronológica.
     */
    val text: String get() = listOfNotNull(chronological, corrected, untilTerm).joinToString(" · ")
}

/**
 * Edad a la fecha [date] de un niño nacido el [birthDate] (RN-HIJ-04): en días durante el primer
 * mes, en meses y días hasta los 2 años, y en años y meses después.
 *
 * Si [gestationalWeeks] indica un prematuro (RN-HIJ-03), agrega lo que corresponde según RN-HIJ-05:
 * antes de su fecha de término (nacimiento + 40 − semanas de gestación), cuánto falta para esa
 * fecha; desde entonces y hasta los 24 meses de edad cronológica, la edad corregida.
 */
fun formatAge(birthDate: LocalDate, date: LocalDate, gestationalWeeks: Int? = null): FormattedAge {
    require(!date.isBefore(birthDate)) { "La fecha es anterior al nacimiento" }
    require(gestationalWeeks == null || gestationalWeeks in 22..42) { "Semanas de gestación fuera de 22 a 42" }

    val chronological = Period.between(birthDate, date)
    val age = FormattedAge(chronological = amountsOf(chronological).joinToString(" y "))
    if (gestationalWeeks == null || gestationalWeeks >= PRETERM_BELOW_WEEKS) return age
    if (chronological.toTotalMonths() >= CORRECTED_AGE_UNTIL_MONTHS) return age

    val termDate = birthDate.plusWeeks((TERM_WEEKS - gestationalWeeks).toLong())
    return if (date.isBefore(termDate)) {
        age.copy(untilTerm = untilTerm(ChronoUnit.DAYS.between(date, termDate).toInt()))
    } else {
        age.copy(corrected = corrected(amountsOf(Period.between(termDate, date))))
    }
}

/** "2 meses corregidos", "1 mes corregido". */
private fun corrected(amounts: List<Amount>): String {
    val adjective = if (amounts.isSingleUnit()) "corregido" else "corregidos"
    return "${amounts.joinToString(" y ")} $adjective"
}

/** "faltan 7 semanas y 6 días para su fecha de término", "falta 1 día para su fecha de término". */
private fun untilTerm(days: Int): String {
    val amounts = listOfNotNull(
        Amount(days / 7, "semana", "semanas").takeIf { it.value > 0 },
        (days % 7).takeIf { it > 0 }?.let(::days),
    )
    val verb = if (amounts.isSingleUnit()) "falta" else "faltan"
    return "$verb ${amounts.joinToString(" y ")} para su fecha de término"
}

private data class Amount(val value: Int, val singular: String, val plural: String) {
    override fun toString() = "$value ${if (value == 1) singular else plural}"
}

/** Una sola unidad que vale 1: el adjetivo o el verbo van en singular. */
private fun List<Amount>.isSingleUnit() = size == 1 && single().value == 1

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
