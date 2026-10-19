package com.pasitos.app.data.vaccination.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** Textos del calendario (RF-VAC-01). */
class VaccinationModelTest {

    @Test
    fun `la edad indicada se escribe como en el calendario oficial`() {
        assertEquals(
            listOf("Al nacer", "1 mes", "2 meses", "12 meses", "15 meses", "18 meses", "5 años", "11 años"),
            listOf(0, 1, 2, 12, 15, 18, 60, 132).map(::ageMonthsLabel),
        )
    }

    @Test
    fun `RF-VAC-01 CA2 el resumen nombra solo lo que hay, en singular o plural`() {
        assertEquals("2 atrasadas · 1 próxima", VaccinationSummary(overdue = 2, due = 0, upcoming = 1).text)
        assertEquals("1 atrasada · 3 pendientes", VaccinationSummary(overdue = 1, due = 3, upcoming = 0).text)
        assertEquals("Vacunas al día", VaccinationSummary(overdue = 0, due = 0, upcoming = 0).text)
    }
}
