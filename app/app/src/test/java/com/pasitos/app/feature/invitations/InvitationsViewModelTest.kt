package com.pasitos.app.feature.invitations

import androidx.lifecycle.SavedStateHandle
import com.pasitos.app.core.network.ApiException
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.data.invitation.FakeInvitationRepository
import com.pasitos.app.data.invitation.FakeInvitationRepository.Companion.invitation
import com.pasitos.shared.api.ErrorCode
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

/** Invitaciones de tutor recibidas (RF-HIJ-06). Con Robolectric por `SavedStateHandle.toRoute`. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class InvitationsViewModelTest {

    private val online = MutableStateFlow(true)
    private val networkMonitor = object : NetworkMonitor {
        override val isOnline = online
    }
    private val repository = FakeInvitationRepository(listOf(invitation()))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(afterRegistration: Boolean = false) = InvitationsViewModel(
        SavedStateHandle(mapOf("afterRegistration" to afterRegistration)),
        repository,
        networkMonitor,
    )

    @Test
    fun `al abrir consulta las invitaciones vigentes`() = runTest {
        val viewModel = viewModel()
        val state = collectState(viewModel)

        assertEquals(1, repository.refreshes)
        assertEquals(listOf("inv-1"), state().invitations.map { it.id })
        assertFalse(state().isLoading)
        assertFalse(state().afterRegistration)
    }

    @Test
    fun `RF-HIJ-06 CA1 sin marcar la declaración no se puede aceptar`() = runTest {
        val viewModel = viewModel()
        val state = collectState(viewModel)

        viewModel.onAcceptClick(invitation())
        assertFalse(state().representationDeclared)
        assertFalse(state().canConfirmAccept)
        viewModel.onConfirmAccept()

        assertTrue(repository.accepted.isEmpty())
    }

    @Test
    fun `RF-HIJ-06 CA1 con la declaración acepta y avisa que el niño llega enseguida`() = runTest {
        val viewModel = viewModel()
        val state = collectState(viewModel)

        viewModel.onAcceptClick(invitation())
        viewModel.onRepresentationDeclaredChange(true)
        viewModel.onConfirmAccept()

        assertEquals(listOf("inv-1" to true), repository.accepted)
        assertNull(state().accepting)
        assertTrue(state().invitations.isEmpty())
        assertTrue(state().acceptedAny)
        assertEquals("Ya sos tutor de Tomi. Su perfil aparece en unos segundos.", state().message)
    }

    @Test
    fun `RF-HIJ-06 CA2 rechaza después de confirmar`() = runTest {
        val viewModel = viewModel()
        val state = collectState(viewModel)

        viewModel.onRejectClick(invitation())
        viewModel.onConfirmReject()

        assertEquals(listOf("inv-1"), repository.rejected)
        assertFalse(state().acceptedAny)
        assertEquals("Rechazaste la invitación. Le avisamos a Ana.", state().message)
    }

    @Test
    fun `RF-HIJ-06 CA3 si venció muestra el aviso del servidor y actualiza la lista`() = runTest {
        val viewModel = viewModel()
        val state = collectState(viewModel)
        repository.respondResult = Result.failure(ApiException(ErrorCode.INVITATION_NOT_FOUND, "Esa invitación venció. Pedí que te inviten de nuevo"))
        repository.server = emptyList()

        viewModel.onAcceptClick(invitation())
        viewModel.onRepresentationDeclaredChange(true)
        viewModel.onConfirmAccept()

        assertNull(state().accepting)
        assertEquals("Esa invitación venció. Pedí que te inviten de nuevo", state().message)
        assertEquals(2, repository.refreshes)
        assertTrue(state().invitations.isEmpty())
    }

    @Test
    fun `sin conexión el error queda en el diálogo para reintentar`() = runTest {
        val viewModel = viewModel()
        val state = collectState(viewModel)
        repository.respondResult = Result.failure(Exception("No pudimos conectarnos. Revisá tu conexión y probá de nuevo."))

        viewModel.onRejectClick(invitation())
        viewModel.onConfirmReject()

        assertEquals(invitation(), state().rejecting)
        assertEquals("No pudimos conectarnos. Revisá tu conexión y probá de nuevo.", state().dialogError)
    }

    @Test
    fun `RF-HIJ-05 CA2 después del registro se muestra sin atrás`() = runTest {
        val viewModel = viewModel(afterRegistration = true)
        val state = collectState(viewModel)

        assertTrue(state().afterRegistration)
    }

    @Test
    fun `el vencimiento se muestra con la fecha argentina`() {
        assertEquals("Vence el 02/11/2026", invitation().expiryText)
        assertEquals("Ana Pérez te invitó a ser tutor de Tomi García", invitation().description)
    }

    /** `uiState` se calcula mientras alguien lo observa, como la pantalla. */
    private fun TestScope.collectState(viewModel: InvitationsViewModel): () -> InvitationsUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }
}
