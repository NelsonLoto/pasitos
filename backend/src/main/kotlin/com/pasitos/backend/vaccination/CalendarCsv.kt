package com.pasitos.backend.vaccination

import com.pasitos.shared.vaccination.CalendarDose
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Una fila del CSV del calendario (`backend/seed/calendar/README.md`). */
data class CalendarSeedRow(
    val vaccine: String,
    val prevents: String?,
    val source: String?,
    val doseCode: String,
    val doseLabel: String,
    val ageMonths: Int,
    val leadDays: Int,
    val toleranceDays: Int,
    val bornFrom: LocalDate?,
    val bornUntil: LocalDate?,
    val sortOrder: Int,
) {
    fun toCalendarDose() = CalendarDose(
        doseCode = doseCode,
        vaccineName = vaccine,
        doseLabel = doseLabel,
        ageMonths = ageMonths,
        leadDays = leadDays,
        toleranceDays = toleranceDays,
        sortOrder = sortOrder,
        bornFrom = bornFrom,
        bornUntil = bornUntil,
    )
}

private val CALENDAR_COLUMNS = listOf(
    "vaccine", "prevents", "source", "dose_code", "dose_label", "age_months", "lead_days",
    "tolerance_days", "born_from", "born_until", "sort_order",
)

/**
 * Las filas de un CSV del calendario. Lanza [IllegalArgumentException] con el número de línea si
 * falta una columna o un valor no se puede leer: la carga no publica un calendario a medias.
 */
fun parseCalendarCsv(text: String): List<CalendarSeedRow> {
    val records = parseCsv(text)
    require(records.isNotEmpty()) { "el archivo está vacío" }
    val header = records.first()
    val missing = CALENDAR_COLUMNS - header.toSet()
    require(missing.isEmpty()) { "faltan las columnas ${missing.joinToString()}" }
    val index = CALENDAR_COLUMNS.associateWith { header.indexOf(it) }

    return records.drop(1).mapIndexed { i, record ->
        val line = i + 2
        require(record.size == header.size) { "línea $line: tiene ${record.size} columnas y el encabezado ${header.size}" }
        fun text(column: String) = record[index.getValue(column)].trim()
        fun required(column: String) = text(column).ifEmpty { throw IllegalArgumentException("línea $line: falta $column") }
        fun int(column: String) = required(column).toIntOrNull()
            ?: throw IllegalArgumentException("línea $line: $column no es un número entero")
        fun date(column: String) = text(column).ifEmpty { null }?.let {
            try {
                LocalDate.parse(it)
            } catch (e: DateTimeParseException) {
                throw IllegalArgumentException("línea $line: $column no es una fecha AAAA-MM-DD")
            }
        }

        CalendarSeedRow(
            vaccine = required("vaccine"),
            prevents = text("prevents").ifEmpty { null },
            source = text("source").ifEmpty { null },
            doseCode = required("dose_code"),
            doseLabel = required("dose_label"),
            ageMonths = int("age_months"),
            leadDays = int("lead_days"),
            toleranceDays = int("tolerance_days"),
            bornFrom = date("born_from"),
            bornUntil = date("born_until"),
            sortOrder = int("sort_order"),
        )
    }
}

/**
 * Lector mínimo de CSV (RFC 4180): campos separados por coma, entre comillas si llevan comas o
 * saltos de línea, `""` para una comilla dentro de un campo, y fin de línea LF o CRLF (en Windows,
 * Git deja los archivos con CRLF). Ignora la marca BOM inicial y las líneas vacías.
 */
fun parseCsv(text: String): List<List<String>> {
    val records = mutableListOf<List<String>>()
    var record = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var i = if (text.startsWith('﻿')) 1 else 0

    fun endRecord() {
        record += field.toString()
        field.clear()
        if (record.size > 1 || record.single().isNotEmpty()) records += record
        record = mutableListOf()
    }

    while (i < text.length) {
        val c = text[i]
        when {
            inQuotes && c == '"' && text.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
            inQuotes && c == '"' -> inQuotes = false
            inQuotes -> field.append(c)
            c == '"' && field.isEmpty() -> inQuotes = true
            c == ',' -> { record += field.toString(); field.clear() }
            c == '\r' && text.getOrNull(i + 1) == '\n' -> { endRecord(); i++ }
            c == '\n' || c == '\r' -> endRecord()
            else -> field.append(c)
        }
        i++
    }
    require(!inQuotes) { "hay comillas sin cerrar" }
    if (field.isNotEmpty() || record.isNotEmpty()) endRecord()
    return records
}
