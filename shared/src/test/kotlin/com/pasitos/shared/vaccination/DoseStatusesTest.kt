package com.pasitos.shared.vaccination

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DoseStatusesTest {

    private val birth = LocalDate.of(2026, 1, 10)

    // 2 meses: fecha indicada 10/3/2026, anticipación 7 días y tolerancia 30 (hasta el 9/4/2026)
    private val penta1 = dose("PENTA-1", ageMonths = 2)

    @Test
    fun `RN-VAC-01 la dosis es Programada mientras falten más días que la anticipación`() {
        assertEquals(DoseStatus.SCHEDULED, statusOf(penta1, today = LocalDate.of(2026, 3, 2)))
    }

    @Test
    fun `RN-VAC-01 la dosis es Próxima desde que faltan tantos días como la anticipación`() {
        assertEquals(DoseStatus.UPCOMING, statusOf(penta1, today = LocalDate.of(2026, 3, 3)))
        assertEquals(DoseStatus.UPCOMING, statusOf(penta1, today = LocalDate.of(2026, 3, 9)))
    }

    @Test
    fun `RN-VAC-01 la dosis es Pendiente desde la fecha indicada hasta el último día de tolerancia`() {
        assertEquals(DoseStatus.DUE, statusOf(penta1, today = LocalDate.of(2026, 3, 10)))
        assertEquals(DoseStatus.DUE, statusOf(penta1, today = LocalDate.of(2026, 4, 9)))
    }

    @Test
    fun `RN-VAC-01 la dosis es Atrasada desde el día siguiente al fin de la tolerancia`() {
        assertEquals(DoseStatus.OVERDUE, statusOf(penta1, today = LocalDate.of(2026, 4, 10)))
    }

    @Test
    fun `RN-VAC-01 sin anticipación la dosis pasa de Programada a Pendiente`() {
        val noLead = dose("BCG", ageMonths = 2, leadDays = 0)

        assertEquals(DoseStatus.SCHEDULED, statusOf(noLead, today = LocalDate.of(2026, 3, 9)))
        assertEquals(DoseStatus.DUE, statusOf(noLead, today = LocalDate.of(2026, 3, 10)))
    }

    @Test
    fun `RN-VAC-01 la tolerancia de cada dosis define cuándo está Atrasada`() {
        // "15 a 18 meses": fecha indicada a los 15 meses y 120 días de tolerancia
        val window = dose("PENTA-R1", ageMonths = 15, toleranceDays = 120)
        val indicated = birth.plusMonths(15)

        assertEquals(DoseStatus.DUE, statusOf(window, today = indicated.plusDays(120)))
        assertEquals(DoseStatus.OVERDUE, statusOf(window, today = indicated.plusDays(121)))
    }

    @Test
    fun `RN-VAC-01 una dosis con registro es Aplicada aunque haya pasado la tolerancia`() {
        val record = AppliedDose("PENTA-1", LocalDate.of(2026, 3, 12))

        val result = doseStatuses(birth, listOf(penta1), listOf(record), today = LocalDate.of(2026, 9, 1)).doses.single()

        assertEquals(DoseStatus.APPLIED, result.status)
        assertEquals(record, result.applied)
    }

    @Test
    fun `RN-VAC-04 una dosis aplicada sin fecha cuenta como Aplicada`() {
        val result = doseStatuses(
            birth,
            listOf(penta1),
            listOf(AppliedDose("PENTA-1", appliedOn = null)),
            today = LocalDate.of(2026, 9, 1),
        ).doses.single()

        assertEquals(DoseStatus.APPLIED, result.status)
        assertNull(result.applied?.appliedOn)
    }

    @Test
    fun `RN-VAC-08 la fecha indicada suma meses de calendario con edad cronológica`() {
        val oneMonth = dose("X", ageMonths = 1)
        val newborn = dose("HB-NEO", ageMonths = 0)

        assertEquals(LocalDate.of(2026, 2, 28), indicatedDate(oneMonth, born = LocalDate.of(2026, 1, 31)))
        assertEquals(LocalDate.of(2024, 2, 29), indicatedDate(oneMonth, born = LocalDate.of(2024, 1, 31)))
        assertEquals(LocalDate.of(2026, 1, 31), indicatedDate(newborn, born = LocalDate.of(2026, 1, 31)))
    }

    @Test
    fun `RN-VAC-10 la 2ª de triple viral depende de la fecha de nacimiento`() {
        // Calendario Nacional 2026: desde el 1/7/2024 a los 15-18 meses; antes, a los 5 años
        val calendar = listOf(
            dose("SRP-2", ageMonths = 15, toleranceDays = 120, bornFrom = LocalDate.of(2024, 7, 1)),
            dose("SRP-2", ageMonths = 60, toleranceDays = 365, bornUntil = LocalDate.of(2024, 6, 30)),
        )

        assertEquals(LocalDate.of(2029, 6, 30), indicatedDate(calendar, born = LocalDate.of(2024, 6, 30)))
        assertEquals(LocalDate.of(2025, 10, 1), indicatedDate(calendar, born = LocalDate.of(2024, 7, 1)))
    }

    @Test
    fun `RN-VAC-10 una dosis de otra cohorte no aparece en el calendario del niño`() {
        val laterCohort = dose("NUEVA-1", ageMonths = 2, bornFrom = LocalDate.of(2027, 1, 1))

        val result = doseStatuses(birth, listOf(penta1, laterCohort), emptyList(), today = birth)

        assertEquals(listOf("PENTA-1"), result.doses.map { it.dose.doseCode })
    }

    @Test
    fun `RN-VAC-03 al cambiar la versión se recalculan las no aplicadas y se conservan las aplicadas`() {
        // La versión nueva saca HA y suma VAR-1; HA ya estaba aplicada
        val newVersion = listOf(penta1, dose("VAR-1", ageMonths = 15, sortOrder = 2))
        val appliedHa = AppliedDose("HA", LocalDate.of(2027, 1, 15))

        val result = doseStatuses(birth, newVersion, listOf(appliedHa), today = LocalDate.of(2026, 3, 10))

        assertEquals(listOf("PENTA-1", "VAR-1"), result.doses.map { it.dose.doseCode })
        assertEquals(listOf(DoseStatus.DUE, DoseStatus.SCHEDULED), result.doses.map { it.status })
        assertEquals(listOf(appliedHa), result.otherApplied)
    }

    @Test
    fun `RN-VAC-03 una dosis aplicada de otra cohorte se conserva aparte`() {
        val laterCohort = dose("NUEVA-1", ageMonths = 2, bornFrom = LocalDate.of(2027, 1, 1))
        val record = AppliedDose("NUEVA-1", appliedOn = null)

        val result = doseStatuses(birth, listOf(penta1, laterCohort), listOf(record), today = birth)

        assertEquals(listOf(record), result.otherApplied)
    }

    @Test
    fun `RN-VAC-09 un registro duplicado cuenta una sola vez`() {
        val first = AppliedDose("PENTA-1", LocalDate.of(2026, 3, 12))
        val second = AppliedDose("PENTA-1", LocalDate.of(2026, 3, 20))
        val removed = AppliedDose("HA", null)

        val result = doseStatuses(birth, listOf(penta1), listOf(first, second, removed, removed), today = birth)

        assertEquals(first, result.doses.single().applied)
        assertEquals(listOf(removed), result.otherApplied)
    }

    @Test
    fun `las dosis salen en el orden del calendario`() {
        val calendar = listOf(dose("C", ageMonths = 4, sortOrder = 3), dose("A", ageMonths = 0, sortOrder = 1), dose("B", ageMonths = 2, sortOrder = 2))

        val result = doseStatuses(birth, calendar, emptyList(), today = birth)

        assertEquals(listOf("A", "B", "C"), result.doses.map { it.dose.doseCode })
    }

    @Test
    fun `un calendario sin problemas es válido`() {
        val calendar = listOf(
            penta1,
            dose("SRP-2", ageMonths = 15, bornFrom = LocalDate.of(2024, 7, 1)),
            dose("SRP-2", ageMonths = 60, bornUntil = LocalDate.of(2024, 6, 30)),
        )

        assertEquals(emptyList(), calendarProblems(calendar))
    }

    @Test
    fun `RN-VAC-10 dos filas del mismo código con rangos superpuestos son un problema`() {
        val overlapping = listOf(
            dose("SRP-2", ageMonths = 15, bornFrom = LocalDate.of(2024, 7, 1)),
            dose("SRP-2", ageMonths = 60, bornUntil = LocalDate.of(2024, 7, 1)),
        )
        val bothOpen = listOf(dose("HA", ageMonths = 12), dose("HA", ageMonths = 15))

        assertEquals(1, calendarProblems(overlapping).size)
        assertEquals(1, calendarProblems(bothOpen).size)
    }

    @Test
    fun `valores negativos o un rango invertido son problemas`() {
        val invalid = listOf(
            dose("A", ageMonths = -1),
            dose("B", ageMonths = 2, leadDays = -1, toleranceDays = -1),
            dose("C", ageMonths = 2, bornFrom = LocalDate.of(2025, 1, 1), bornUntil = LocalDate.of(2024, 1, 1)),
        )

        val problems = calendarProblems(invalid)

        assertEquals(4, problems.size)
        assertTrue(problems.all { it.startsWith("A:") || it.startsWith("B:") || it.startsWith("C:") })
    }

    private fun statusOf(dose: CalendarDose, today: LocalDate): DoseStatus =
        doseStatuses(birth, listOf(dose), emptyList(), today).doses.single().status

    private fun indicatedDate(dose: CalendarDose, born: LocalDate): LocalDate = indicatedDate(listOf(dose), born)

    private fun indicatedDate(calendar: List<CalendarDose>, born: LocalDate): LocalDate =
        doseStatuses(born, calendar, emptyList(), today = born).doses.single().indicatedDate

    private fun dose(
        code: String,
        ageMonths: Int,
        leadDays: Int = 7,
        toleranceDays: Int = 30,
        sortOrder: Int = 1,
        bornFrom: LocalDate? = null,
        bornUntil: LocalDate? = null,
    ) = CalendarDose(
        doseCode = code,
        vaccineName = code,
        doseLabel = "dosis",
        ageMonths = ageMonths,
        leadDays = leadDays,
        toleranceDays = toleranceDays,
        sortOrder = sortOrder,
        bornFrom = bornFrom,
        bornUntil = bornUntil,
    )
}
