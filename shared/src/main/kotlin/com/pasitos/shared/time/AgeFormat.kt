package com.pasitos.shared.time

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/** Semanas de gestación de un niño nacido a término: es el valor de "No lo sé" (RN-HIJ-03). */
const val TERM_GESTATIONAL_WEEKS: Int = 40

private val GESTATIONAL_WEEKS_RANGE = 22..42
private const val PRETERM_BELOW_WEEKS = 37
private const val CORRECTED_AGE_UNTIL_MONTHS = 24L

/**
 * Edad para mostrar, según RN-HIJ-04 y RN-HIJ-05 (10 · Módulo compartido §4).
 *
 * - Edad cronológica: días durante el primer mes, meses y días hasta los 2 años, y años y meses
 *   después. Se cuenta por calendario: del 31/1 al 28/2 son "28 días".
 * - Prematuro (menos de 37 semanas) con menos de 24 meses: antes de su fecha de término agrega cuánto
 *   falta ("14 días · faltan 10 semanas para su fecha de término"); desde esa fecha, la edad corregida
 *   ("4 meses y 10 días · 2 meses y 13 días corregidos").
 *
 * Es pura: [date] la elige quien llama, con ArgentinaClock.today() en la app y el backend (ADR-26).
 *
 * @throws IllegalArgumentException si [gestationalWeeks] está fuera de 22..42 o [date] es anterior a
 *   [birthDate]. El mensaje no incluye las fechas, para que no lleguen a un log.
 */
fun formatAge(birthDate: LocalDate, date: LocalDate, gestationalWeeks: Int): String {
    require(gestationalWeeks in GESTATIONAL_WEEKS_RANGE) { "Las semanas de gestación tienen que estar entre 22 y 42" }
    require(!date.isBefore(birthDate)) { "La fecha es anterior al nacimiento" }

    val chronological = Period.between(birthDate, date)
    val age = formatPeriod(chronological)
    if (gestationalWeeks >= PRETERM_BELOW_WEEKS || chronological.toTotalMonths() >= CORRECTED_AGE_UNTIL_MONTHS) {
        return age
    }
    val termDate = birthDate.plusWeeks((TERM_GESTATIONAL_WEEKS - gestationalWeeks).toLong())
    if (date.isBefore(termDate)) {
        return "$age · ${untilTerm(ChronoUnit.DAYS.between(date, termDate))} para su fecha de término"
    }
    val corrected = formatPeriod(Period.between(termDate, date))
    val suffix = if (corrected == "1 día" || corrected == "1 mes") "corregido" else "corregidos"
    return "$age · $corrected $suffix"
}

private fun formatPeriod(period: Period): String {
    val months = period.toTotalMonths()
    return when {
        months == 0L -> unit(period.days.toLong(), "día", "días")
        months < 24 -> withRest(unit(months, "mes", "meses"), period.days.toLong(), "día", "días")
        else -> withRest(unit(period.years.toLong(), "año", "años"), period.months.toLong(), "mes", "meses")
    }
}

/** "faltan 9 semanas y 4 días"; "falta" solo con "1 semana" o "1 día". */
private fun untilTerm(days: Long): String {
    val weeks = days / 7
    val rest = days % 7
    val text = when {
        weeks == 0L -> unit(rest, "día", "días")
        else -> withRest(unit(weeks, "semana", "semanas"), rest, "día", "días")
    }
    val verb = if (text == "1 semana" || text == "1 día") "falta" else "faltan"
    return "$verb $text"
}

private fun withRest(main: String, rest: Long, singular: String, plural: String): String =
    if (rest == 0L) main else "$main y ${unit(rest, singular, plural)}"

private fun unit(value: Long, singular: String, plural: String): String =
    "$value ${if (value == 1L) singular else plural}"
