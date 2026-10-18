package com.pasitos.shared.vaccination

/**
 * Problemas de una versión del calendario que impedirían calcular bien los estados. La usan la
 * carga de datos y el panel antes de publicar una versión; una lista vacía es un calendario válido.
 *
 * - Edad, anticipación o tolerancia negativas.
 * - Un rango de nacimiento que termina antes de empezar.
 * - Dos filas del mismo código cuyos rangos de nacimiento se superponen (RN-VAC-10): para algún
 *   niño habría dos edades para la misma dosis.
 */
fun calendarProblems(calendar: List<CalendarDose>): List<String> {
    val problems = mutableListOf<String>()
    for (dose in calendar) {
        if (dose.ageMonths < 0) problems += "${dose.doseCode}: la edad en meses es negativa"
        if (dose.leadDays < 0) problems += "${dose.doseCode}: la anticipación es negativa"
        if (dose.toleranceDays < 0) problems += "${dose.doseCode}: la tolerancia es negativa"
        if (dose.bornFrom != null && dose.bornUntil != null && dose.bornUntil.isBefore(dose.bornFrom)) {
            problems += "${dose.doseCode}: el rango de nacimiento termina antes de empezar"
        }
    }
    for ((code, rows) in calendar.groupBy { it.doseCode }) {
        for (i in rows.indices) {
            for (j in i + 1 until rows.size) {
                if (overlap(rows[i], rows[j])) problems += "$code: dos filas con rangos de nacimiento superpuestos"
            }
        }
    }
    return problems
}

private fun overlap(a: CalendarDose, b: CalendarDose): Boolean {
    val aStartsAfterBEnds = a.bornFrom != null && b.bornUntil != null && a.bornFrom.isAfter(b.bornUntil)
    val bStartsAfterAEnds = b.bornFrom != null && a.bornUntil != null && b.bornFrom.isAfter(a.bornUntil)
    return !aStartsAfterBEnds && !bStartsAfterAEnds
}
