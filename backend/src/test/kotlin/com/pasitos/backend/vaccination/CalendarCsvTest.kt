package com.pasitos.backend.vaccination

import com.pasitos.shared.vaccination.calendarProblems
import java.io.File
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Lector del CSV del calendario (I1-B7): comillas, CRLF y errores con número de línea. */
class CalendarCsvTest {

    @Test
    fun `un campo entre comillas puede tener comas y comillas dobles`() {
        val records = parseCsv("a,\"b, c\",\"dice \"\"hola\"\"\"\n")

        assertEquals(listOf(listOf("a", "b, c", "dice \"hola\"")), records)
    }

    @Test
    fun `CRLF y LF dan las mismas filas, sin BOM ni líneas vacías`() {
        val lf = parseCsv("x,y\n1,\"2,5\"\n\n3,\n")
        val crlf = parseCsv("﻿x,y\r\n1,\"2,5\"\r\n\r\n3,\r\n")

        assertEquals(listOf(listOf("x", "y"), listOf("1", "2,5"), listOf("3", "")), lf)
        assertEquals(lf, crlf)
    }

    @Test
    fun `comillas sin cerrar es un error`() {
        assertFailsWith<IllegalArgumentException> { parseCsv("a,\"b\n") }
    }

    @Test
    fun `el calendario cargado tiene las 27 dosis, la 2ª de triple viral en dos filas por cohorte y su fuente`() {
        val rows = parseCalendarCsv(File("seed/calendar/calendario-nacional-2026-01-01.csv").readText())

        assertEquals(28, rows.size)
        assertEquals(27, rows.map { it.doseCode }.distinct().size)
        val srp2 = rows.filter { it.doseCode == "SRP-2" }
        assertEquals(listOf(15 to LocalDate.of(2024, 7, 1), 60 to null), srp2.map { it.ageMonths to it.bornFrom })
        assertEquals(LocalDate.of(2024, 6, 30), srp2.last().bornUntil)
        assertTrue(rows.all { it.source == "Calendario Nacional de Vacunación 2026, Ministerio de Salud de la Nación" })
        assertEquals("Meningitis, neumonía y sepsis por neumococo", rows.first { it.doseCode == "NEUMO-1" }.prevents)
        assertEquals(emptyList(), calendarProblems(rows.map { it.toCalendarDose() }))
    }

    @Test
    fun `una columna faltante, una fila incompleta o un valor ilegible se informan con su línea`() {
        val header = "vaccine,prevents,source,dose_code,dose_label,age_months,lead_days,tolerance_days,born_from,born_until,sort_order"
        val row = "BCG,Tuberculosis,Fuente,BCG,Única dosis,0,7,30,,,1"

        val missingColumn = assertFailsWith<IllegalArgumentException> { parseCalendarCsv(header.replace(",source", "") + "\n") }
        val shortRow = assertFailsWith<IllegalArgumentException> { parseCalendarCsv("$header\n$row\nBCG,Tuberculosis\n") }
        val badNumber = assertFailsWith<IllegalArgumentException> { parseCalendarCsv("$header\n${row.replace(",0,7,", ",dos,7,")}\n") }
        val badDate = assertFailsWith<IllegalArgumentException> { parseCalendarCsv("$header\n${row.replace(",,,1", ",01/07/2024,,1")}\n") }
        val noCode = assertFailsWith<IllegalArgumentException> { parseCalendarCsv("$header\n${row.replace("Fuente,BCG,", "Fuente,,")}\n") }

        assertEquals("faltan las columnas source", missingColumn.message)
        assertEquals("línea 3: tiene 2 columnas y el encabezado 11", shortRow.message)
        assertEquals("línea 2: age_months no es un número entero", badNumber.message)
        assertEquals("línea 2: born_from no es una fecha AAAA-MM-DD", badDate.message)
        assertEquals("línea 2: falta dose_code", noCode.message)
    }
}
