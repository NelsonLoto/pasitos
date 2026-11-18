package com.pasitos.shared.time

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class AgeFormatTest {
    private val birth = LocalDate.of(2026, 1, 10)
    private fun term(date: LocalDate) = formatAge(birth, date, TERM_GESTATIONAL_WEEKS)
    private fun weeks(gestationalWeeks: Int, date: LocalDate) = formatAge(birth, date, gestationalWeeks)
    private fun d(month: Int, day: Int, year: Int = 2026) = LocalDate.of(year, month, day)

    @Test fun `RN-HIJ-04 el primer mes se muestra en dias`() {
        assertEquals("0 días", term(birth))
        assertEquals("1 día", term(d(1, 11)))
        assertEquals("12 días", term(d(1, 22)))
        assertEquals("30 días", term(d(2, 9)))
    }

    @Test fun `RN-HIJ-04 hasta los 2 anos se muestra en meses y dias`() {
        assertEquals("1 mes", term(d(2, 10)))
        assertEquals("4 meses y 1 día", term(d(5, 11)))
        assertEquals("8 meses", term(d(9, 10)))
        assertEquals("12 meses", term(d(1, 10, 2027)))
        assertEquals("13 meses y 5 días", term(d(2, 15, 2027)))
        assertEquals("23 meses y 30 días", term(d(1, 9, 2028)))
    }

    @Test fun `RN-HIJ-04 desde los 2 anos se muestra en anos y meses`() {
        assertEquals("2 años", term(d(1, 10, 2028)))
        assertEquals("2 años y 2 meses", term(d(3, 10, 2028)))
        assertEquals("3 años y 1 mes", term(d(2, 20, 2029)))
    }

    @Test fun `RN-HIJ-04 los meses se cuentan por calendario`() {
        val endOfMonth = LocalDate.of(2026, 1, 31)
        assertEquals("28 días", formatAge(endOfMonth, d(2, 28), TERM_GESTATIONAL_WEEKS))
        assertEquals("1 mes y 1 día", formatAge(endOfMonth, d(3, 1), TERM_GESTATIONAL_WEEKS))
    }

    @Test fun `RN-HIJ-05 antes de la fecha de termino muestra cuanto falta en semanas y dias`() {
        // 28 semanas: la fecha de término es el 04/04/2026.
        assertEquals("14 días · faltan 10 semanas para su fecha de término", weeks(28, d(1, 24)))
        assertEquals("17 días · faltan 9 semanas y 4 días para su fecha de término", weeks(28, d(1, 27)))
        assertEquals("2 meses y 16 días · faltan 1 semana y 2 días para su fecha de término", weeks(28, d(3, 26)))
        assertEquals("2 meses y 18 días · falta 1 semana para su fecha de término", weeks(28, d(3, 28)))
        assertEquals("2 meses y 19 días · faltan 6 días para su fecha de término", weeks(28, d(3, 29)))
        assertEquals("2 meses y 24 días · falta 1 día para su fecha de término", weeks(28, d(4, 3)))
    }

    @Test fun `RN-HIJ-05 el dia de la fecha de termino muestra 0 dias corregidos`() {
        assertEquals("2 meses y 25 días · 0 días corregidos", weeks(28, d(4, 4)))
    }

    @Test fun `RN-HIJ-05 la edad corregida usa el formato de RN-HIJ-04`() {
        // 32 semanas: la fecha de término es el 07/03/2026.
        assertEquals("1 mes y 26 días · 1 día corregido", weeks(32, d(3, 8)))
        assertEquals("2 meses y 28 días · 1 mes corregido", weeks(32, d(4, 7)))
        assertEquals("4 meses y 10 días · 2 meses y 13 días corregidos", weeks(32, d(5, 20)))
        assertEquals("23 meses y 30 días · 21 meses y 5 días corregidos", weeks(28, d(1, 9, 2028)))
    }

    @Test fun `RN-HIJ-05 desde los 24 meses cronologicos no hay edad corregida`() {
        assertEquals("2 años", weeks(28, d(1, 10, 2028)))
        assertEquals("2 años y 2 meses", weeks(22, d(3, 10, 2028)))
    }

    @Test fun `RN-HIJ-03 desde 37 semanas no se corrige y No lo se es a termino`() {
        assertEquals("14 días", weeks(37, d(1, 24)))
        assertEquals("14 días · faltan 2 semanas para su fecha de término", weeks(36, d(1, 24)))
        assertEquals("4 meses y 1 día", weeks(TERM_GESTATIONAL_WEEKS, d(5, 11)))
        assertEquals("4 meses y 1 día", weeks(42, d(5, 11)))
    }

    @Test fun `RN-HIJ-01 y 03 rechaza datos invalidos sin incluir fechas en el mensaje`() {
        for (invalid in listOf(21, 43)) {
            val error = assertFailsWith<IllegalArgumentException> { weeks(invalid, d(2, 1)) }
            assertFalse(error.message.orEmpty().contains("2026"))
        }
        val error = assertFailsWith<IllegalArgumentException> { term(d(1, 9)) }
        assertFalse(error.message.orEmpty().contains("2026"))
    }
}
