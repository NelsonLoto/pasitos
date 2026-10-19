package com.pasitos.app.feature.vaccinedetail

import androidx.lifecycle.SavedStateHandle
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.data.child.ChildTestData
import com.pasitos.app.data.child.FakeChildRepository
import com.pasitos.app.data.child.SignedInAuthRepository
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildSex
import com.pasitos.app.data.vaccination.FakeVaccinationRepository
import com.pasitos.app.data.vaccination.VaccinationTestData
import com.pasitos.app.data.vaccination.VaccinationTestData.applied
import com.pasitos.app.data.vaccination.model.DoseApplicationInput
import com.pasitos.shared.vaccination.DoseStatus
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Detalle y registro de una dosis (RF-VAC-02, 03, 07). Hoy es el 19/10/2026 y Lucía nació el 16/10/2025. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class VaccineDetailViewModelTest {

    private val online = MutableStateFlow(true)
    private val networkMonitor = object : NetworkMonitor {
        override val isOnline = online
    }
    private val children = FakeChildRepository(
        listOf(Child(ChildTestData.SOFIA, "Lucía", "Pérez", ChildSex.Female, VaccinationTestData.LUCIA_BIRTH, 39)),
    )
    private val vaccination = FakeVaccinationRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `RF-VAC-07 CA1 muestra la vacuna, qué previene, la edad indicada, el estado y la fuente`() = runTest {
        val viewModel = detail("NEUMO-1")
        val state = collectState(viewModel)

        assertEquals("Neumococo conjugada", state().vaccineName)
        assertEquals("1ª dosis", state().doseLabel)
        assertEquals("Meningitis, neumonía y sepsis por neumococo", state().prevents)
        assertEquals("2 meses · 16/12/2025", state().indicatedText)
        assertEquals(VaccinationTestData.SOURCE, state().source)
        assertEquals(DoseStatus.OVERDUE, state().status)
        assertTrue("RN-VAC-05: muestra el aviso de atrasada", state().isOverdue)
        assertTrue(state().showForm)
    }

    @Test
    fun `RF-VAC-02 CA1 y CA3 registra la dosis con fecha, sin lote, y vuelve al calendario`() = runTest {
        val viewModel = detail("NEUMO-1")
        val state = collectState(viewModel)
        val done = doneEvents(viewModel)

        viewModel.onDateSelected(LocalDate.of(2025, 12, 20))
        viewModel.onPlaceChange("  Hospital  ")
        viewModel.onSave()

        assertEquals(
            listOf(Triple(ChildTestData.SOFIA, "NEUMO-1", DoseApplicationInput(LocalDate.of(2025, 12, 20), "", "Hospital"))),
            vaccination.registered,
        )
        assertEquals(1, done())
        assertNull(state().error)
    }

    @Test
    fun `RF-VAC-02 CA2 pide la fecha y la rechaza antes del nacimiento o después de hoy`() = runTest {
        val viewModel = detail("NEUMO-1")
        val state = collectState(viewModel)

        viewModel.onSave()
        assertEquals("Elegí la fecha de aplicación", state().form.dateError)

        viewModel.onDateSelected(LocalDate.of(2025, 10, 15))
        viewModel.onSave()
        assertEquals("La fecha no puede ser anterior al nacimiento", state().form.dateError)

        viewModel.onDateSelected(LocalDate.of(2026, 10, 20))
        viewModel.onSave()
        assertEquals("La fecha no puede ser posterior a hoy", state().form.dateError)

        viewModel.onDateSelected(LocalDate.of(2025, 12, 20))
        viewModel.onLotChange("L".repeat(41))
        viewModel.onSave()
        assertEquals("El lote tiene un máximo de 40 caracteres", state().form.lotError)
        assertTrue(vaccination.registered.isEmpty())
    }

    @Test
    fun `RF-VAC-02 CA4 una dosis ya aplicada muestra su registro, y si es propia se edita`() = runTest {
        vaccination.appliedDoses.value = listOf(applied("BCG").copy(lot = "A1"))
        val viewModel = detail("BCG")
        val state = collectState(viewModel)

        assertFalse(state().showForm)
        assertEquals("17/10/2025", state().applied?.dateText)
        assertEquals("La registraste vos", state().applied?.authorText)
        assertTrue(state().applied?.isOwn == true)

        viewModel.onEditClick()
        assertTrue(state().showForm)
        assertEquals("A1", state().form.lot)
        viewModel.onLotChange("B2")
        viewModel.onSave()

        assertEquals("B2", vaccination.updated.single().second.lot)
        assertFalse("Al guardar vuelve a la vista de la dosis", state().isEditing)
    }

    @Test
    fun `RF-VAC-03 CA1 el autor elimina con confirmación`() = runTest {
        vaccination.appliedDoses.value = listOf(applied("BCG"))
        val viewModel = detail("BCG")
        val state = collectState(viewModel)
        val done = doneEvents(viewModel)

        viewModel.onDeleteClick()
        assertTrue(state().confirmDelete)
        viewModel.onConfirmDelete()

        assertEquals(listOf("dose-BCG"), vaccination.deletedDoses)
        assertEquals(1, done())
    }

    @Test
    fun `RF-VAC-03 CA2 la dosis de otro tutor se ve en modo lectura, con su autor`() = runTest {
        vaccination.appliedDoses.value = listOf(applied("BCG", author = VaccinationTestData.OTHER_ACCOUNT_ID))
        val viewModel = detail("BCG")
        val state = collectState(viewModel)

        assertEquals("La registró la familia", state().applied?.authorText)
        assertFalse(state().showForm)
        viewModel.onEditClick()
        viewModel.onDeleteClick()
        assertFalse(state().isEditing)
        assertFalse(state().confirmDelete)
    }

    @Test
    fun `RF-VAC-04 CA3 una dosis sin fecha se completa después, y la fecha puede quedar vacía`() = runTest {
        vaccination.appliedDoses.value = listOf(applied("HB-NEO", appliedOn = null))
        val viewModel = detail("HB-NEO")
        val state = collectState(viewModel)
        assertEquals("Fecha no registrada", state().applied?.dateText)

        viewModel.onEditClick()
        assertFalse(state().dateRequired)
        viewModel.onLotChange("L7")
        viewModel.onSave()

        assertEquals(DoseApplicationInput(appliedOn = null, lot = "L7", place = ""), vaccination.updated.single().second)
    }

    @Test
    fun `sin conexión no registra`() = runTest {
        online.value = false
        val viewModel = detail("NEUMO-1")
        val state = collectState(viewModel)

        viewModel.onDateSelected(LocalDate.of(2025, 12, 20))
        viewModel.onSave()

        assertFalse(state().isOnline)
        assertTrue(vaccination.registered.isEmpty())
    }

    @Test
    fun `RN-VAC-03 una dosis de un calendario anterior se muestra aplicada, sin edad indicada`() = runTest {
        vaccination.appliedDoses.value = listOf(applied("FA-1"))
        val viewModel = detail("FA-1")
        val state = collectState(viewModel)

        assertEquals("Fiebre amarilla", state().vaccineName)
        assertEquals(DoseStatus.APPLIED, state().status)
        assertNull(state().indicatedText)
        assertFalse(state().notFound)
    }

    private fun detail(doseCode: String) = VaccineDetailViewModel(
        SavedStateHandle(mapOf("childId" to ChildTestData.SOFIA, "doseCode" to doseCode)),
        children,
        vaccination,
        SignedInAuthRepository(),
        networkMonitor,
        ChildTestData.clock,
    )

    private fun TestScope.collectState(viewModel: VaccineDetailViewModel): () -> VaccineDetailUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    private fun TestScope.doneEvents(viewModel: VaccineDetailViewModel): () -> Int {
        var count = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.done.collect { count++ } }
        return { count }
    }
}
