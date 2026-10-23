package com.pasitos.app.feature.profile

import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.auth.model.AuthSession
import com.pasitos.app.data.auth.model.SessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val authRepository = FakeAuthRepository()
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = ProfileViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `RF-CTA-07 sin cambios pendientes cierra la sesión sin preguntar`() = runTest {
        viewModel.onLogoutClick()

        assertEquals(1, authRepository.logoutCalls)
    }

    @Test
    fun `RF-CTA-07 CA2 con cambios pendientes avisa que se pierden y espera la confirmación`() = runTest {
        authRepository.pending = 3
        val state = collectState()

        viewModel.onLogoutClick()

        assertEquals(3, state().pendingChangesToConfirm)
        assertEquals(0, authRepository.logoutCalls)

        viewModel.onConfirmLogout()

        assertNull(state().pendingChangesToConfirm)
        assertEquals(1, authRepository.logoutCalls)
    }

    @Test
    fun `RF-CTA-07 CA2 si no confirma la sesión sigue abierta`() = runTest {
        authRepository.pending = 1
        val state = collectState()

        viewModel.onLogoutClick()
        viewModel.onDismissLogout()

        assertNull(state().pendingChangesToConfirm)
        assertEquals(0, authRepository.logoutCalls)
    }

    @Test
    fun `si no se pudo cerrar la sesión muestra el motivo`() = runTest {
        authRepository.logoutResult = Result.failure(Exception("No pudimos conectarnos. Revisá tu conexión y probá de nuevo."))
        val state = collectState()

        viewModel.onLogoutClick()

        assertEquals("No pudimos conectarnos. Revisá tu conexión y probá de nuevo.", state().logoutError)
        assertFalse(state().isLoggingOut)
    }

    /** `uiState` se calcula mientras alguien lo observa, como la pantalla. */
    private fun kotlinx.coroutines.test.TestScope.collectState(): () -> ProfileUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    private class FakeAuthRepository : AuthRepository {
        var pending = 0
        var logoutCalls = 0
        var logoutResult: Result<Unit> = Result.success(Unit)

        private val session = AuthSession("1", "ana@pasitos.test", "Ana", "Pérez")
        override val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState.SignedIn(session))
        override val currentSession: StateFlow<AuthSession?> = MutableStateFlow(session)

        override suspend fun login(email: String, password: String) = Result.success(session)
        override suspend fun loginWithGoogle() = Result.success(session)
        override suspend fun register(firstName: String, lastName: String, email: String, password: String, acceptTerms: Boolean, accountType: String, dni: String?, licenseNumber: String?, specialty: String?) =
            Result.success(session)
        override suspend fun resendEmailVerification(email: String) = Result.success(Unit)
        override suspend fun requestPasswordReset(email: String) = Result.success(Unit)
        override suspend fun isEmailVerified() = Result.success(true)

        override suspend fun pendingChanges(): Int = pending

        override suspend fun logout(): Result<Unit> {
            logoutCalls++
            return logoutResult
        }
    }
}
