package com.pasitos.app.data.child.model

import java.time.LocalDate
import java.time.Period

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

/** Ej: "8 meses", "1 año", "3 años". */
fun Child.ageLabel(today: LocalDate = LocalDate.now()): String {
    val period = Period.between(birthDate, today)
    return if (period.years < 1) {
        val months = period.years * 12 + period.months
        if (months == 1) "1 mes" else "$months meses"
    } else {
        if (period.years == 1) "1 año" else "${period.years} años"
    }
}

/** Ej: "8 meses · Niña". */
fun Child.ageAndSexLabel(today: LocalDate = LocalDate.now()): String = "${ageLabel(today)} · ${sex.label}"
