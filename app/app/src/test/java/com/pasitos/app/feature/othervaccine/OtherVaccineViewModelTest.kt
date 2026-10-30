package com.pasitos.app.feature.othervaccine

import androidx.lifecycle.SavedStateHandle
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.data.child.ChildTestData
import com.pasitos.app.data.child.FakeChildRepository
import com.pasitos.app.data.child.SignedInAuthRepository
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildSex
import com.pasitos.app.data.vaccination.FakeVaccinationRepository
import com.pasitos.app.data.vaccination.VaccinationTestData
import com.pasitos.app.data.vaccination.model.OtherVaccineInput
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Otras vacunas (RF-VAC-05). Hoy es el 19/10/2026 y Lucía nació el 16/10/2025. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OtherVaccineViewModelTest {

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
    fun `RF-VAC-05 CA1 registra una vacuna con nombre y fecha, desde una sugerencia`() = runTest {
        val viewModel = otherVaccine()
        val state = collectState(viewModel)
        val done = doneEvents(viewModel)

        viewModel.onSuggestion("Antigripal")
        viewModel.onDateSelected(LocalDate.of(2026, 5, 1))
        viewModel.onNoteChange("  Campaña de otoño ")
        viewModel.onSave()

        assertEquals(
            listOf(Triple(ChildTestData.SOFIA, null, OtherVaccineInput("Antigripal", LocalDate.of(2026, 5, 1), "", "Campaña de otoño"))),
            vaccination.savedOtherVaccines,
        )
        assertEquals(1, done())
        assertTrue(state().isNew)
    }

    @Test
    fun `pide nombre y fecha válidos, y respeta los límites de texto`() = runTest {
        val viewModel = otherVaccine()
        val state = collectState(viewModel)

        viewModel.onSave()
        assertEquals("Completá el nombre de la vacuna", state().nameError)
        assertEquals("Elegí la fecha de aplicación", state().dateError)

        viewModel.onNameChange("N".repeat(121))
        viewModel.onDateSelected(LocalDate.of(2025, 10, 15))
        viewModel.onNoteChange("n".repeat(281))
        viewModel.onSave()

        assertEquals("El nombre tiene un máximo de 120 caracteres", state().nameError)
        assertEquals("La fecha no puede ser anterior al nacimiento", state().dateError)
        assertEquals("La nota tiene un máximo de 280 caracteres", state().noteError)
        assertTrue(vaccination.savedOtherVaccines.isEmpty())
    }

    @Test
    fun `una vacuna propia abre con sus datos para editarla o eliminarla`() = runTest {
        vaccination.otherVaccines.value = listOf(VaccinationTestData.otherVaccine())
        val viewModel = otherVaccine(vaccineId = "other-1")
        val state = collectState(viewModel)

        assertEquals("Antigripal", state().name)
        assertEquals("X1", state().lot)
        assertFalse(state().isReadOnly)

        viewModel.onNameChange("Antigripal trivalente")
        viewModel.onSave()
        assertEquals("other-1", vaccination.savedOtherVaccines.single().second)

        viewModel.onDeleteClick()
        viewModel.onConfirmDelete()
        assertEquals(listOf("other-1"), vaccination.deletedOtherVaccines)
    }

    @Test
    fun `la de otro tutor se ve en modo lectura`() = runTest {
        vaccination.otherVaccines.value = listOf(VaccinationTestData.otherVaccine(author = VaccinationTestData.OTHER_ACCOUNT_ID))
        val viewModel = otherVaccine(vaccineId = "other-1")
        val state = collectState(viewModel)

        assertTrue(state().isReadOnly)
        assertEquals("La registró la familia", state().authorText)
        viewModel.onSave()
        viewModel.onDeleteClick()
        assertTrue(vaccination.savedOtherVaccines.isEmpty())
        assertFalse(state().confirmDelete)
    }

    @Test
    fun `sin conexión no guarda`() = runTest {
        online.value = false
        val viewModel = otherVaccine()
        collectState(viewModel)

        viewModel.onNameChange("Antigripal")
        viewModel.onDateSelected(LocalDate.of(2026, 5, 1))
        viewModel.onSave()

        assertTrue(vaccination.savedOtherVaccines.isEmpty())
    }

    private fun otherVaccine(vaccineId: String? = null) = OtherVaccineViewModel(
        SavedStateHandle(mapOf("childId" to ChildTestData.SOFIA, "vaccineId" to vaccineId)),
        children,
        vaccination,
        SignedInAuthRepository(),
        networkMonitor,
        ChildTestData.clock,
    )

    private fun TestScope.collectState(viewModel: OtherVaccineViewModel): () -> OtherVaccineUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    private fun TestScope.doneEvents(viewModel: OtherVaccineViewModel): () -> Int {
        var count = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.done.collect { count++ } }
        return { count }
    }
}
