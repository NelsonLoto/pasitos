package com.pasitos.app.feature.profile.account

import com.pasitos.app.core.network.ApiException
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.data.account.FakeAccountRepository
import com.pasitos.app.data.auth.model.AuthSession
import com.pasitos.app.data.child.SignedInAuthRepository
import com.pasitos.shared.api.ErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

/** "Nombre y apellido", "Email" y "Contraseña" del perfil (RF-CTA-11). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AccountFormsViewModelTest {

    private val accounts = FakeAccountRepository()
    private val online = MutableStateFlow(true)
    private val networkMonitor = object : NetworkMonitor {
        override val isOnline = online
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `RF-CTA-11 el nombre viene precargado y se guarda`() = runTest {
        val viewModel = EditNameViewModel(accounts, SignedInAuthRepository(ANA), networkMonitor)
        val state = collect(viewModel.uiState)
        var saved = false
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.saved.collect { saved = true } }

        assertEquals("Ana", state().firstName)
        viewModel.onFirstNameChange("Anita")
        viewModel.onSave()

        assertEquals(listOf("updateName Anita Pérez"), accounts.calls)
        assertTrue(saved)
    }

    @Test
    fun `RF-CTA-11 el nombre no puede quedar vacío ni guardarse sin conexión`() = runTest {
        val viewModel = EditNameViewModel(accounts, SignedInAuthRepository(ANA), networkMonitor)
        val state = collect(viewModel.uiState)

        viewModel.onLastNameChange(" ")
        viewModel.onSave()
        online.value = false

        assertEquals("Ingresá tu apellido", state().lastNameError)
        assertFalse(state().canSave)
        assertTrue(accounts.calls.isEmpty())
    }

    @Test
    fun `RF-CTA-11 CA1 el cambio de email pide la contraseña y avisa a dónde se mandó el link`() = runTest {
        val viewModel = ChangeEmailViewModel(accounts, SignedInAuthRepository(ANA), networkMonitor)
        val state = collect(viewModel.uiState)

        viewModel.onNewEmailChange("ana.nueva@pasitos.test")
        viewModel.onSend()
        assertEquals("Ingresá tu contraseña actual", state().passwordError)

        viewModel.onPasswordChange("secreta123")
        viewModel.onSend()

        assertEquals(listOf("requestEmailChange ana.nueva@pasitos.test"), accounts.calls)
        assertEquals("ana.nueva@pasitos.test", state().sentTo)
        assertEquals("", state().password)
    }

    @Test
    fun `RF-CTA-11 CA1 con la contraseña incorrecta el error queda en ese campo`() = runTest {
        accounts.result = Result.failure(ApiException(ErrorCode.INVALID_CREDENTIALS, "La contraseña actual no es correcta"))
        val viewModel = ChangeEmailViewModel(accounts, SignedInAuthRepository(ANA), networkMonitor)
        val state = collect(viewModel.uiState)

        viewModel.onNewEmailChange("ana.nueva@pasitos.test")
        viewModel.onPasswordChange("otra1234")
        viewModel.onSend()

        assertEquals("La contraseña actual no es correcta", state().passwordError)
        assertNull(state().sentTo)
    }

    @Test
    fun `RF-CTA-11 CA4 sin contraseña el cambio de email pide crearla primero`() = runTest {
        val viewModel = ChangeEmailViewModel(accounts, SignedInAuthRepository(ANA.copy(hasPassword = false)), networkMonitor)
        val state = collect(viewModel.uiState)

        assertFalse(state().hasPassword)
    }

    @Test
    fun `RF-CTA-11 CA2 la contraseña nueva cumple RN-CTA-01 y se repite igual`() = runTest {
        val viewModel = ChangePasswordViewModel(accounts, SignedInAuthRepository(ANA), networkMonitor)
        val state = collect(viewModel.uiState)

        viewModel.onCurrentPasswordChange("secreta123")
        viewModel.onNewPasswordChange("corta")
        viewModel.onConfirmPasswordChange("otra")
        viewModel.onChange()

        assertEquals("Tiene que tener al menos 8 caracteres, con letras y números", state().newPasswordError)
        assertEquals("Las contraseñas no coinciden", state().confirmPasswordError)
        assertTrue(accounts.calls.isEmpty())
    }

    @Test
    fun `RF-CTA-11 CA2 cambia la contraseña y limpia los campos`() = runTest {
        val viewModel = ChangePasswordViewModel(accounts, SignedInAuthRepository(ANA), networkMonitor)
        val state = collect(viewModel.uiState)

        viewModel.onCurrentPasswordChange("secreta123")
        viewModel.onNewPasswordChange("nueva1234")
        viewModel.onConfirmPasswordChange("nueva1234")
        viewModel.onChange()

        assertEquals(listOf("changePassword"), accounts.calls)
        assertEquals(PasswordResult.CHANGED, state().result)
        assertEquals("", state().newPassword)
    }

    @Test
    fun `RF-CTA-11 CA4 sin contraseña manda el email para crearla`() = runTest {
        val viewModel = ChangePasswordViewModel(accounts, SignedInAuthRepository(ANA.copy(hasPassword = false)), networkMonitor)
        val state = collect(viewModel.uiState)

        viewModel.onSendCreationEmail()

        assertEquals(listOf("requestPasswordCreation"), accounts.calls)
        assertEquals(PasswordResult.CREATION_EMAIL_SENT, state().result)
    }

    /** `uiState` se calcula mientras alguien lo observa, como la pantalla. */
    private fun <T> TestScope.collect(flow: StateFlow<T>): () -> T {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
        return { flow.value }
    }

    private companion object {
        val ANA = AuthSession("1", "ana@pasitos.test", "Ana", "Pérez")
    }
}
