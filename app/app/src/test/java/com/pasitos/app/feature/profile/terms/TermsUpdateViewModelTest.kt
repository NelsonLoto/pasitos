package com.pasitos.app.feature.profile.terms

import com.pasitos.app.data.account.FakeAccountRepository
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.auth.model.AuthSession
import com.pasitos.app.data.auth.model.SessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

/** "Actualizamos los términos" (RN-CTA-02). */
@OptIn(ExperimentalCoroutinesApi::class)
class TermsUpdateViewModelTest {

    private val accounts = FakeAccountRepository().apply { pendingTermsVersion.value = "2" }
    private val auth = FakeAuthRepository()
    private lateinit var viewModel: TermsUpdateViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = TermsUpdateViewModel(accounts, auth)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `RN-CTA-02 la casilla empieza sin marcar y sin marcarla no se acepta`() = runTest {
        assertFalse(viewModel.uiState.value.acceptTerms)

        viewModel.onAcceptClick()

        assertEquals(
            "Para seguir usando Pasitos tenés que aceptar los términos y la política de privacidad",
            viewModel.uiState.value.termsError,
        )
        assertTrue(accounts.calls.isEmpty())
    }

    @Test
    fun `RN-CTA-02 al aceptar se registra y deja de haber términos pendientes`() = runTest {
        viewModel.onAcceptTermsChange(true)

        viewModel.onAcceptClick()

        assertEquals(listOf("acceptTerms"), accounts.calls)
        assertNull(accounts.pendingTermsVersion.value)
    }

    @Test
    fun `RN-CTA-02 si los términos vuelven a cambiar con la app abierta, la casilla vuelve sin marcar`() = runTest {
        viewModel.onAcceptTermsChange(true)
        viewModel.onAcceptClick()

        // Mismo ViewModel: vive mientras viva la Activity
        accounts.pendingTermsVersion.value = "3"
        viewModel.onAcceptClick()

        assertFalse(viewModel.uiState.value.acceptTerms)
        assertEquals(listOf("acceptTerms"), accounts.calls)
        assertEquals("3", accounts.pendingTermsVersion.value)
    }

    @Test
    fun `si no acepta puede cerrar sesión, con el aviso de cambios pendientes`() = runTest {
        auth.pending = 2

        viewModel.onLogoutClick()
        assertEquals(2, viewModel.uiState.value.pendingChangesToConfirm)
        assertEquals(0, auth.logoutCalls)

        viewModel.onConfirmLogout()
        assertEquals(1, auth.logoutCalls)
    }

    private class FakeAuthRepository : AuthRepository {
        var pending = 0
        var logoutCalls = 0

        private val session = AuthSession("1", "ana@pasitos.test", "Ana", "Pérez")
        override val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState.SignedIn(session))
        override val currentSession: StateFlow<AuthSession?> = MutableStateFlow(session)

        override suspend fun login(email: String, password: String) = Result.success(session)
        override suspend fun loginWithGoogle(idToken: String, acceptTerms: Boolean, firstName: String?, lastName: String?) = Result.success(session)
        override suspend fun register(firstName: String, lastName: String, email: String, password: String, acceptTerms: Boolean, accountType: String, dni: String?, licenseNumber: String?, specialty: String?) =
            Result.success(session)
        override suspend fun resendEmailVerification(email: String) = Result.success(Unit)
        override suspend fun requestPasswordReset(email: String) = Result.success(Unit)
        override suspend fun isEmailVerified() = Result.success(true)
        override suspend fun pendingChanges(): Int = pending

        override suspend fun logout(): Result<Unit> {
            logoutCalls++
            return Result.success(Unit)
        }
    }
}
