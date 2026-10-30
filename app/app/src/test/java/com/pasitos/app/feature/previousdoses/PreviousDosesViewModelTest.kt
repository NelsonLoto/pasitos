package com.pasitos.app.feature.previousdoses

import androidx.lifecycle.SavedStateHandle
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.data.child.ChildTestData
import com.pasitos.app.data.child.FakeChildRepository
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildSex
import com.pasitos.app.data.vaccination.FakeVaccinationRepository
import com.pasitos.app.data.vaccination.VaccinationTestData
import com.pasitos.app.data.vaccination.VaccinationTestData.applied
import com.pasitos.app.data.vaccination.model.VaccinationCalendar
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

/** Carga retroactiva (RF-VAC-04). Hoy es el 19/10/2026 y Lucía nació el 16/10/2025. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PreviousDosesViewModelTest {

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
    fun `ofrece las dosis con fecha indicada anterior a hoy que no están registradas, agrupadas por edad`() = runTest {
        vaccination.appliedDoses.value = listOf(applied("BCG"))
        val state = collectState(previousDoses())

        assertEquals(listOf("Al nacer", "2 meses", "12 meses"), state().groups.map { it.label })
        assertEquals(listOf("HB-NEO", "NEUMO-1", "PENTA-1", "HA"), state().groups.flatMap { g -> g.doses.map { it.doseCode } })
        assertEquals(4, state().total)
        assertEquals(PreviousDosesStep.Question, state().step)
    }

    @Test
    fun `RF-VAC-04 CA1 con Sí la lista viene toda marcada y se guarda con un solo pedido`() = runTest {
        val viewModel = previousDoses()
        val state = collectState(viewModel)

        viewModel.onAnswer(upToDate = true)
        assertEquals(setOf("BCG", "HB-NEO", "NEUMO-1", "PENTA-1", "HA"), state().checked)
        viewModel.onSave()

        assertEquals(listOf(ChildTestData.SOFIA to listOf("BCG", "HB-NEO", "NEUMO-1", "PENTA-1", "HA")), vaccination.previousDoses)
        assertEquals(PreviousDosesStep.Done, state().step)
        assertEquals(5, state().savedCount)
    }

    @Test
    fun `RF-VAC-04 CA2 con No o No sé la lista viene sin marcar y se eligen una por una`() = runTest {
        val viewModel = previousDoses()
        val state = collectState(viewModel)

        viewModel.onAnswer(upToDate = false)
        assertTrue(state().checked.isEmpty())
        assertFalse(state().canSave)
        viewModel.onToggle("NEUMO-1", true)
        viewModel.onToggle("BCG", true)
        viewModel.onSave()

        // En el orden del calendario
        assertEquals(listOf("BCG", "NEUMO-1"), vaccination.previousDoses.single().second)
    }

    @Test
    fun `sin conexión no guarda`() = runTest {
        online.value = false
        val viewModel = previousDoses()
        val state = collectState(viewModel)

        viewModel.onAnswer(upToDate = true)
        viewModel.onSave()

        assertFalse(state().canSave)
        assertTrue(vaccination.previousDoses.isEmpty())
    }

    @Test
    fun `sin calendario lo pide al abrir, y después del alta se puede saltear`() = runTest {
        vaccination.calendar.value = VaccinationCalendar.EMPTY
        val viewModel = previousDoses(afterChildCreated = true)
        val state = collectState(viewModel)
        var finished = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.finished.collect { finished++ } }

        viewModel.onFinish()

        assertEquals(1, vaccination.refreshCount)
        assertTrue(state().afterChildCreated)
        assertEquals(0, state().total)
        assertEquals(1, finished)
    }

    private fun previousDoses(afterChildCreated: Boolean = false) = PreviousDosesViewModel(
        SavedStateHandle(mapOf("childId" to ChildTestData.SOFIA, "afterChildCreated" to afterChildCreated)),
        children,
        vaccination,
        networkMonitor,
        ChildTestData.clock,
    )

    private fun TestScope.collectState(viewModel: PreviousDosesViewModel): () -> PreviousDosesUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }
}
