package com.pasitos.app.feature.guardians

import androidx.lifecycle.SavedStateHandle
import com.pasitos.app.core.network.ApiException
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.data.child.ChildTestData
import com.pasitos.app.data.child.ChildTestData.ACCOUNT_ID
import com.pasitos.app.data.child.ChildTestData.SOFIA
import com.pasitos.app.data.child.FakeChildRepository
import com.pasitos.app.data.child.SignedInAuthRepository
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildSex
import com.pasitos.app.data.child.model.GuardianRole
import com.pasitos.app.data.guardian.FakeGuardianRepository
import com.pasitos.app.data.guardian.Guardian
import com.pasitos.shared.api.ErrorCode
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

/** Tutores de un niño (RF-HIJ-05, 07, 08). Con Robolectric por la validación de email de Android. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GuardiansViewModelTest {

    private val me = Guardian(ACCOUNT_ID, "Ana", "Pérez", "ana@pasitos.test", GuardianRole.PRINCIPAL)
    private val beto = Guardian("beto-id", "Beto", "Gómez", "beto@pasitos.test", GuardianRole.SECONDARY)

    private val online = MutableStateFlow(true)
    private val networkMonitor = object : NetworkMonitor {
        override val isOnline = online
    }
    private val guardians = FakeGuardianRepository(listOf(me, beto))
    private val children = FakeChildRepository(
        listOf(Child(id = SOFIA, firstName = "Sofía", lastName = "Pérez", sex = ChildSex.Female, birthDate = LocalDate.of(2026, 3, 15))),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = GuardiansViewModel(
        SavedStateHandle(mapOf("childId" to SOFIA)),
        guardians,
        children,
        SignedInAuthRepository(ChildTestData.session),
        networkMonitor,
    )

    @Test
    fun `al abrir trae la lista y reconoce al usuario como principal`() = runTest {
        val viewModel = viewModel()
        val state = collectState(viewModel)

        assertEquals(listOf(me, beto), state().guardians)
        assertEquals("Sofía", state().childFirstName)
        assertTrue(state().isPrincipal)
        assertTrue(state().isMe(me))
        assertFalse(state().isLoading)
    }

    @Test
    fun `RF-HIJ-05 invita con el email sin espacios y avisa que se mandó`() = runTest {
        val viewModel = viewModel()
        val state = collectState(viewModel)

        viewModel.onInviteClick()
        viewModel.onInviteEmailChange("  cami@pasitos.test ")
        viewModel.onSendInvite()

        assertTrue("invite $SOFIA cami@pasitos.test" in guardians.calls)
        assertEquals(GuardiansDialog.None, state().dialog)
        assertEquals("Le mandamos la invitación a cami@pasitos.test. Vence en 15 días.", state().message)
        // La lista se vuelve a pedir: la invitación nueva aparece entre las enviadas
        assertEquals(listOf("cami@pasitos.test"), state().sentInvitations.map { it.email })
    }

    @Test
    fun `RF-HIJ-05 CA3 el principal ve sus invitaciones enviadas, con la vencida marcada`() = runTest {
        guardians.sent = listOf(FakeGuardianRepository.sentInvitation(expired = true))
        val viewModel = viewModel()
        val state = collectState(viewModel)

        val sent = state().sentInvitations.single()
        assertEquals("Venció el 16/10/2026", sent.expiryText)
        assertEquals("Vence el 16/10/2026", sent.copy(expired = false).expiryText)
    }

    @Test
    fun `RF-HIJ-05 CA3 reenviar una vencida la renueva y lo avisa`() = runTest {
        guardians.sent = listOf(FakeGuardianRepository.sentInvitation(expired = true))
        val viewModel = viewModel()
        val state = collectState(viewModel)

        viewModel.onResendClick(state().sentInvitations.single())

        assertTrue("resend inv-1" in guardians.calls)
        assertEquals("Le volvimos a mandar la invitación a cami@pasitos.test. Vence en 15 días.", state().message)
        assertFalse(state().sentInvitations.single().expired)
    }

    @Test
    fun `una invitación vigente no se reenvía`() = runTest {
        guardians.sent = listOf(FakeGuardianRepository.sentInvitation(expired = false))
        val viewModel = viewModel()
        val state = collectState(viewModel)

        viewModel.onResendClick(state().sentInvitations.single())

        assertTrue(guardians.calls.none { it.startsWith("resend") })
    }

    @Test
    fun `si el reenvío falla el motivo se muestra en la pantalla`() = runTest {
        guardians.sent = listOf(FakeGuardianRepository.sentInvitation(expired = true))
        val viewModel = viewModel()
        val state = collectState(viewModel)
        guardians.actionResult = Result.failure(ApiException(ErrorCode.INVITATION_NOT_FOUND, "No se encontró esa invitación"))

        viewModel.onResendClick(state().sentInvitations.single())

        assertEquals("No se encontró esa invitación", state().message)
    }

    @Test
    fun `un secundario no pide las invitaciones enviadas`() = runTest {
        guardians.list = listOf(beto.copy(role = GuardianRole.PRINCIPAL), me.copy(role = GuardianRole.SECONDARY))
        guardians.sent = listOf(FakeGuardianRepository.sentInvitation())
        val viewModel = viewModel()
        val state = collectState(viewModel)

        assertTrue(state().sentInvitations.isEmpty())
        assertTrue(guardians.calls.none { it.startsWith("sentInvitations") })
    }

    @Test
    fun `un email inválido no se envía`() = runTest {
        val viewModel = viewModel()
        val state = collectState(viewModel)

        viewModel.onInviteClick()
        viewModel.onInviteEmailChange("cami@")
        viewModel.onSendInvite()

        assertEquals("Ingresá un email válido", state().dialogError)
        assertEquals(GuardiansDialog.Invite, state().dialog)
        assertTrue(guardians.calls.none { it.startsWith("invite") })
    }

    @Test
    fun `RF-HIJ-05 CA4 si ya es tutor muestra el mensaje del servidor en el diálogo`() = runTest {
        guardians.actionResult = Result.failure(ApiException(ErrorCode.ALREADY_GUARDIAN, "Ese email ya es tutor de este hijo/a"))
        val viewModel = viewModel()
        val state = collectState(viewModel)

        viewModel.onInviteClick()
        viewModel.onInviteEmailChange("beto@pasitos.test")
        viewModel.onSendInvite()

        assertEquals(GuardiansDialog.Invite, state().dialog)
        assertEquals("Ese email ya es tutor de este hijo/a", state().dialogError)
    }

    @Test
    fun `RF-HIJ-07 CA1 el principal quita a un secundario después de confirmar`() = runTest {
        val viewModel = viewModel()
        val state = collectState(viewModel)

        viewModel.onRemoveClick(beto)
        assertEquals(GuardiansDialog.Remove(beto), state().dialog)
        viewModel.onConfirmRemove()

        assertTrue("remove $SOFIA beto-id" in guardians.calls)
        assertEquals(listOf(me), state().guardians)
        assertEquals(GuardiansDialog.None, state().dialog)
        assertEquals("Beto ya no es tutor de Sofía.", state().message)
    }

    @Test
    fun `RF-HIJ-08 al ceder el rol el usuario pasa a secundario y deja de ver las acciones del principal`() = runTest {
        val viewModel = viewModel()
        val state = collectState(viewModel)

        viewModel.onTransferClick(beto)
        viewModel.onConfirmTransfer()

        assertTrue("transfer $SOFIA beto-id" in guardians.calls)
        assertFalse(state().isPrincipal)
        assertEquals("Beto ahora es el tutor principal.", state().message)
    }

    @Test
    fun `un secundario no puede abrir quitar ni ceder el rol`() = runTest {
        guardians.list = listOf(me.copy(role = GuardianRole.SECONDARY), beto.copy(role = GuardianRole.PRINCIPAL))
        val viewModel = viewModel()
        val state = collectState(viewModel)

        viewModel.onRemoveClick(beto)
        viewModel.onTransferClick(beto)

        assertEquals(GuardiansDialog.None, state().dialog)
    }

    @Test
    fun `RF-HIJ-07 CA2 un secundario deja de ser tutor y la pantalla se cierra`() = runTest {
        guardians.list = listOf(beto.copy(role = GuardianRole.PRINCIPAL), me.copy(role = GuardianRole.SECONDARY))
        children.onlyGuardian = Result.success(false)
        val viewModel = viewModel()
        val state = collectState(viewModel)
        var left = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.left.collect { left++ } }

        viewModel.onLeaveClick()
        assertEquals(GuardiansDialog.Leave, state().dialog)
        viewModel.onConfirmLeave()

        assertEquals(listOf(SOFIA), children.left)
        assertEquals(1, left)
    }

    @Test
    fun `si el principal se fue mientras tanto no ofrece dejar de ser tutor, porque eliminaría el perfil`() = runTest {
        guardians.list = listOf(beto.copy(role = GuardianRole.PRINCIPAL), me.copy(role = GuardianRole.SECONDARY))
        children.onlyGuardian = Result.success(true)
        val viewModel = viewModel()
        val state = collectState(viewModel)

        viewModel.onLeaveClick()

        assertEquals(GuardiansDialog.None, state().dialog)
        assertTrue(children.left.isEmpty())
        assertTrue(state().message!!.startsWith("Ahora sos el único tutor"))
    }

    @Test
    fun `sin conexión muestra el error y vuelve a pedir la lista al recuperarla`() = runTest {
        guardians.listResult = Result.failure(Exception("No pudimos conectarnos. Revisá tu conexión y probá de nuevo."))
        online.value = false
        val viewModel = viewModel()
        val state = collectState(viewModel)
        assertEquals("No pudimos conectarnos. Revisá tu conexión y probá de nuevo.", state().loadError)
        assertFalse(state().canAct)

        guardians.listResult = Result.success(Unit)
        online.value = true

        assertNull(state().loadError)
        assertEquals(listOf(me, beto), state().guardians)
    }

    /** `uiState` se calcula mientras alguien lo observa, como la pantalla. */
    private fun TestScope.collectState(viewModel: GuardiansViewModel): () -> GuardiansUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }
}
