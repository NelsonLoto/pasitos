package com.pasitos.shared.time

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AgeTest {

    @Test
    fun `RN-HIJ-04 durante el primer mes la edad va en días`() {
        assertEquals("0 días", age("2026-10-01", "2026-10-01"))
        assertEquals("1 día", age("2026-10-01", "2026-10-02"))
        assertEquals("30 días", age("2026-10-01", "2026-10-31"))
        // Del 31/01 al 28/02 todavía no se cumple el primer mes.
        assertEquals("28 días", age("2026-01-31", "2026-02-28"))
    }

    @Test
    fun `RN-HIJ-04 hasta los 2 años la edad va en meses y días`() {
        assertEquals("1 mes", age("2026-09-16", "2026-10-16"))
        assertEquals("4 meses", age("2026-06-15", "2026-10-15"))
        assertEquals("4 meses y 1 día", age("2026-06-15", "2026-10-16"))
        assertEquals("12 meses y 5 días", age("2025-10-11", "2026-10-16"))
        assertEquals("23 meses y 29 días", age("2024-10-16", "2026-10-15"))
    }

    @Test
    fun `RN-HIJ-04 desde los 2 años la edad va en años y meses`() {
        assertEquals("2 años", age("2024-10-16", "2026-10-16"))
        assertEquals("2 años y 1 mes", age("2024-10-16", "2026-11-20"))
        assertEquals("10 años y 5 meses", age("2016-05-03", "2026-10-16"))
    }

    @Test
    fun `RF-CRE-03 CA2 un prematuro muestra las dos edades`() {
        // 32 semanas: la edad corregida es la cronológica menos 8 semanas (RN-HIJ-05).
        val age = formatAge(LocalDate.parse("2026-06-01"), LocalDate.parse("2026-10-27"), gestationalWeeks = 32)

        assertEquals("4 meses y 26 días", age.chronological)
        assertEquals("3 meses corregidos", age.corrected)
        assertEquals("4 meses y 26 días · 3 meses corregidos", age.text)
    }

    @Test
    fun `RN-HIJ-05 la edad corregida concuerda en singular y en plural`() {
        val birth = LocalDate.parse("2026-06-01")

        assertEquals("1 mes corregido", formatAge(birth, LocalDate.parse("2026-08-27"), 32).corrected)
        assertEquals("1 día corregido", formatAge(birth, LocalDate.parse("2026-07-28"), 32).corrected)
        assertEquals("1 mes y 3 días corregidos", formatAge(birth, LocalDate.parse("2026-08-30"), 32).corrected)
    }

    @Test
    fun `RN-HIJ-05 antes de su fecha de término se informa cuánto falta, no la edad corregida`() {
        // 30 semanas: la fecha de término es 10 semanas después del nacimiento, el 10/12/2026.
        val beforeTerm = formatAge(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-15"), gestationalWeeks = 30)

        assertEquals("14 días · faltan 8 semanas para su fecha de término", beforeTerm.text)
        assertNull(beforeTerm.corrected)
    }

    @Test
    fun `RN-HIJ-05 lo que falta para el término va en semanas y días, en singular o plural`() {
        val birth = LocalDate.parse("2026-10-01")

        assertEquals("faltan 7 semanas y 6 días para su fecha de término", untilTerm(birth, "2026-10-16"))
        assertEquals("faltan 1 semana y 1 día para su fecha de término", untilTerm(birth, "2026-12-02"))
        assertEquals("falta 1 semana para su fecha de término", untilTerm(birth, "2026-12-03"))
        assertEquals("faltan 3 días para su fecha de término", untilTerm(birth, "2026-12-07"))
        assertEquals("falta 1 día para su fecha de término", untilTerm(birth, "2026-12-09"))
    }

    @Test
    fun `RN-HIJ-05 desde su fecha de término se informa la edad corregida`() {
        val onTerm = formatAge(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-12-10"), gestationalWeeks = 30)

        assertEquals("0 días corregidos", onTerm.corrected)
        assertNull(onTerm.untilTerm)
    }

    @Test
    fun `RN-HIJ-05 la edad corregida se usa hasta los 24 meses de edad cronológica`() {
        val birth = LocalDate.parse("2024-10-16")

        assertEquals("21 meses y 20 días corregidos", formatAge(birth, LocalDate.parse("2026-10-15"), 30).corrected)
        assertNull(formatAge(birth, LocalDate.parse("2026-10-16"), 30).corrected)
    }

    @Test
    fun `RN-HIJ-03 desde las 37 semanas o sin semanas conocidas no hay edad corregida`() {
        val birth = LocalDate.parse("2026-06-01")
        val date = LocalDate.parse("2026-10-27")

        assertEquals("4 meses y 26 días", formatAge(birth, date, gestationalWeeks = 37).text)
        assertEquals("4 meses y 26 días", formatAge(birth, date, gestationalWeeks = null).text)
    }

    @Test
    fun `una fecha anterior al nacimiento o semanas fuera de rango son un error`() {
        val birth = LocalDate.parse("2026-06-01")

        assertFailsWith<IllegalArgumentException> { formatAge(birth, LocalDate.parse("2026-05-31")) }
        assertFailsWith<IllegalArgumentException> { formatAge(birth, LocalDate.parse("2026-10-01"), gestationalWeeks = 21) }
        assertFailsWith<IllegalArgumentException> { formatAge(birth, LocalDate.parse("2026-10-01"), gestationalWeeks = 43) }
    }

    private fun age(birthDate: String, date: String) = formatAge(LocalDate.parse(birthDate), LocalDate.parse(date)).text

    private fun untilTerm(birthDate: LocalDate, date: String) =
        formatAge(birthDate, LocalDate.parse(date), gestationalWeeks = 30).untilTerm
}
