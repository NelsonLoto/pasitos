package com.pasitos.app.feature.vaccination

import com.pasitos.app.data.child.ChildTestData
import com.pasitos.app.data.child.FakeChildRepository
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildSex
import com.pasitos.app.data.vaccination.FakeVaccinationRepository
import com.pasitos.app.data.vaccination.VaccinationTestData
import com.pasitos.app.data.vaccination.VaccinationTestData.applied
import com.pasitos.app.data.vaccination.model.VaccinationCalendar
import com.pasitos.shared.vaccination.DoseStatus
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Calendario del hijo activo (RF-VAC-01). Hoy es el 19/10/2026 y Lucía nació el 16/10/2025. */
@OptIn(ExperimentalCoroutinesApi::class)
class VaccinationCalendarViewModelTest {

    private val lucia = Child(
        id = ChildTestData.SOFIA,
        firstName = "Lucía",
        lastName = "Pérez",
        sex = ChildSex.Female,
        birthDate = VaccinationTestData.LUCIA_BIRTH,
        gestationalWeeks = 39,
    )
    private val children = FakeChildRepository(listOf(lucia))
    private val vaccination = FakeVaccinationRepository()
    private lateinit var viewModel: VaccinationCalendarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = VaccinationCalendarViewModel(children, vaccination, ChildTestData.clock)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `RF-VAC-01 CA1 agrupa las dosis por edad indicada, con su estado y solo las de su cohorte`() = runTest {
        vaccination.appliedDoses.value = listOf(applied("BCG"), applied("HB-NEO", appliedOn = null))
        val state = collectState()

        assertEquals(listOf("Al nacer", "2 meses", "12 meses", "15 meses"), state().groups.map { it.label })
        val rows = state().groups.flatMap { it.doses }.associateBy { it.doseCode }
        assertEquals(DoseStatus.APPLIED, rows.getValue("BCG").status)
        assertEquals("Única dosis · 17/10/2025", rows.getValue("BCG").detail)
        assertEquals("Dosis neonatal · Fecha no registrada", rows.getValue("HB-NEO").detail)
        assertEquals(DoseStatus.OVERDUE, rows.getValue("NEUMO-1").status)
        assertEquals("1ª dosis · Desde el 16/12/2025", rows.getValue("NEUMO-1").detail)
        assertEquals(DoseStatus.DUE, rows.getValue("HA").status)
        assertEquals(DoseStatus.SCHEDULED, rows.getValue("VAR-1").status)
        // Nacida después del 01/07/2024: la 2ª de triple viral es a los 15 meses, no a los 5 años
        assertEquals("2ª dosis · 16/01/2027", rows.getValue("SRP-2").detail)
    }

    @Test
    fun `RF-VAC-01 CA2 el resumen cuenta atrasadas, pendientes y próximas`() = runTest {
        val state = collectState()

        // Sin nada aplicado: 4 atrasadas (nacimiento y 2 meses) y 1 pendiente (12 meses)
        assertEquals("4 atrasadas · 1 pendiente", state().summary?.text)

        vaccination.appliedDoses.value = listOf("BCG", "HB-NEO", "NEUMO-1", "PENTA-1", "HA").map { applied(it) }
        assertEquals("Vacunas al día", state().summary?.text)
    }

    @Test
    fun `RF-VAC-01 CA3 el filtro deja solo las dosis en ese estado`() = runTest {
        vaccination.appliedDoses.value = listOf(applied("BCG"))
        val state = collectState()

        viewModel.onFilterSelected(DoseFilter.Overdue)

        assertEquals(listOf("HB-NEO", "NEUMO-1", "PENTA-1"), state().groups.flatMap { it.doses }.map { it.doseCode })
        assertFalse("Las otras vacunas no tienen estado", state().showOtherVaccines)

        viewModel.onFilterSelected(DoseFilter.Applied)
        assertEquals(listOf("BCG"), state().groups.flatMap { it.doses }.map { it.doseCode })
    }

    @Test
    fun `RF-VAC-01 CA4 y RN-VAC-03 las otras vacunas y las dosis de calendarios anteriores van aparte`() = runTest {
        vaccination.appliedDoses.value = listOf(applied("FA-1", appliedOn = LocalDate.of(2026, 2, 1)))
        vaccination.otherVaccines.value = listOf(VaccinationTestData.otherVaccine())
        val state = collectState()

        assertEquals(listOf(RecordRowUi("FA-1", "Fiebre amarilla", "1ª dosis · 01/02/2026")), state().formerDoses)
        assertEquals(listOf(RecordRowUi("other-1", "Antigripal", "01/05/2026")), state().otherVaccines)
        assertTrue(state().showOtherVaccines)
    }

    @Test
    fun `RF-VAC-04 CA4 ofrece marcar dosis anteriores solo si hay alguna sin registrar`() = runTest {
        val state = collectState()
        assertTrue(state().canMarkPreviousDoses)

        // Hepatitis A (12 meses) cayó el 16/10/2026: también es anterior a hoy
        vaccination.appliedDoses.value = listOf("BCG", "HB-NEO", "NEUMO-1", "PENTA-1").map { applied(it) }
        assertTrue(state().canMarkPreviousDoses)

        vaccination.appliedDoses.value += applied("HA")
        assertFalse(state().canMarkPreviousDoses)
    }

    @Test
    fun `sin calendario cargado lo avisa en lugar de mostrar dosis`() = runTest {
        vaccination.calendar.value = VaccinationCalendar.EMPTY
        val state = collectState()

        assertTrue(state().isLoaded)
        assertFalse(state().hasCalendar)
        assertEquals(null, state().summary)
    }

    private fun TestScope.collectState(): () -> VaccinationCalendarUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }
}
