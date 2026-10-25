package com.pasitos.app.feature.childprofile

import androidx.lifecycle.SavedStateHandle
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.data.child.ChildTestData
import com.pasitos.app.data.child.FakeChildRepository
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildSex
import com.pasitos.app.data.child.model.GuardianRole
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

/** Perfil y eliminación de un hijo (RF-HIJ-09). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChildProfileViewModelTest {

    private val online = MutableStateFlow(true)
    private val networkMonitor = object : NetworkMonitor {
        override val isOnline = online
    }
    private val repository = FakeChildRepository(
        listOf(
            Child(
                id = ChildTestData.SOFIA,
                firstName = "Lucía",
                lastName = "Pérez",
                sex = ChildSex.Female,
                birthDate = LocalDate.of(2026, 3, 15),
                gestationalWeeks = 34,
            ),
        ),
    )
    private lateinit var viewModel: ChildProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = ChildProfileViewModel(
            SavedStateHandle(mapOf("childId" to ChildTestData.SOFIA)),
            repository,
            networkMonitor,
            ChildTestData.clock,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `muestra el niño con su edad corregida`() = runTest {
        val state = collectState()

        assertEquals("Lucía", state().child?.firstName)
        // 34 semanas: fecha de término el 26/04/2026
        assertEquals("7 meses y 4 días · 5 meses y 23 días corregidos", state().ageLabel)
        assertTrue(state().isPrincipal)
    }

    @Test
    fun `RF-HIJ-09 CA1 el único tutor confirma escribiendo el nombre`() = runTest {
        val state = collectState()
        val deleted = deletedEvents()

        viewModel.onDeleteClick()
        assertEquals(DeletionStep.ConfirmByName, state().deletion)
        assertFalse(state().canConfirmDeletion)

        viewModel.onTypedNameChange("Lucas")
        viewModel.onConfirmDeletion()
        assertTrue(repository.deleted.isEmpty())

        viewModel.onTypedNameChange(" lucia ")
        assertTrue(state().canConfirmDeletion)
        viewModel.onConfirmDeletion()

        assertEquals(listOf(ChildTestData.SOFIA), repository.deleted)
        assertEquals(1, deleted())
    }

    @Test
    fun `RF-HIJ-09 CA2 y RF-HIJ-07 CA2 con otros tutores deja de ser tutor sin escribir el nombre`() = runTest {
        repository.onlyGuardian = Result.success(false)
        val state = collectState()
        val deleted = deletedEvents()

        viewModel.onDeleteClick()

        assertEquals(DeletionStep.ConfirmLeave, state().deletion)
        assertTrue(state().canConfirmDeletion)
        viewModel.onConfirmDeletion()
        // Deja de ser tutor por el camino que le avisa al principal, no por la eliminación
        assertEquals(listOf(ChildTestData.SOFIA), repository.left)
        assertTrue(repository.deleted.isEmpty())
        assertEquals(1, deleted())
    }

    @Test
    fun `un tutor secundario no ve el aviso del rol principal`() = runTest {
        repository.role = GuardianRole.SECONDARY
        val state = collectState()

        assertFalse(state().isPrincipal)
    }

    @Test
    fun `si no se pudo consultar al servidor muestra el error y no pide confirmación`() = runTest {
        repository.onlyGuardian = Result.failure(Exception("No pudimos conectarnos. Revisá tu conexión y probá de nuevo."))
        val state = collectState()

        viewModel.onDeleteClick()

        assertEquals(DeletionStep.None, state().deletion)
        assertEquals("No pudimos conectarnos. Revisá tu conexión y probá de nuevo.", state().error)
    }

    @Test
    fun `si dejar de ser tutor falla se queda en la confirmación con el error`() = runTest {
        repository.leaveResult = Result.failure(Exception("Ocurrió un error, intentá de nuevo"))
        repository.onlyGuardian = Result.success(false)
        val state = collectState()

        viewModel.onDeleteClick()
        viewModel.onConfirmDeletion()

        assertEquals(DeletionStep.ConfirmLeave, state().deletion)
        assertEquals("Ocurrió un error, intentá de nuevo", state().error)
    }

    @Test
    fun `sin conexión no empieza la eliminación`() = runTest {
        online.value = false
        val state = collectState()

        viewModel.onDeleteClick()

        assertEquals(DeletionStep.None, state().deletion)
        assertFalse(state().isOnline)
    }

    @Test
    fun `alcanza con el nombre de pila sin distinguir mayúsculas, tildes ni espacios`() {
        assertTrue(matchesFirstName("lucia", "Lucía"))
        assertTrue(matchesFirstName("  LUCÍA ", "Lucía"))
        assertTrue(matchesFirstName("ana   maria", "Ana María"))
        assertFalse(matchesFirstName("Lucas", "Lucía"))
        assertFalse(matchesFirstName("", "Lucía"))
        assertFalse(matchesFirstName("Lucía Pérez", "Lucía"))
    }

    /** `uiState` se calcula mientras alguien lo observa, como la pantalla. */
    private fun TestScope.collectState(): () -> ChildProfileUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    private fun TestScope.deletedEvents(): () -> Int {
        var count = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.deleted.collect { count++ } }
        return { count }
    }
}
