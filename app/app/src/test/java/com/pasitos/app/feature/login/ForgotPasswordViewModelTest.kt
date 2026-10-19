package com.pasitos.app.feature.login

import com.pasitos.app.core.network.NoConnectionException
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.auth.model.AuthSession
import com.pasitos.app.data.auth.model.SessionState
import java.io.IOException
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ForgotPasswordViewModelTest {

    private val authRepository = FakeAuthRepository()
    private lateinit var viewModel: ForgotPasswordViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = ForgotPasswordViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `RF-CTA-05 CA1 pedirlo manda el email y muestra el mensaje neutro`() = runTest {
        val state = collectState()
        viewModel.onEmailChange("ana@pasitos.test")

        viewModel.onSubmit()

        assertEquals("ana@pasitos.test", authRepository.lastEmail)
        assertTrue(state().requestSent)
        assertNull(state().generalError)
    }

    @Test
    fun `un email inválido no llega a pedirse`() = runTest {
        val state = collectState()
        viewModel.onEmailChange("no-es-un-email")

        viewModel.onSubmit()

        assertEquals("Ingresá un email válido", state().emailError)
        assertFalse(state().requestSent)
        assertEquals(0, authRepository.calls)
    }

    @Test
    fun `un problema de conexión real se muestra, no se oculta como si no existiera la cuenta`() = runTest {
        authRepository.result = Result.failure(NoConnectionException(IOException("sin red")))
        val state = collectState()
        viewModel.onEmailChange("ana@pasitos.test")

        viewModel.onSubmit()

        assertFalse(state().requestSent)
        assertEquals("No pudimos conectarnos. Revisá tu conexión y probá de nuevo.", state().generalError)
    }

    private fun kotlinx.coroutines.test.TestScope.collectState(): () -> ForgotPasswordUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    private class FakeAuthRepository : AuthRepository {
        var calls = 0
        var lastEmail: String? = null
        var result: Result<Unit> = Result.success(Unit)

        override val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState.SignedOut())
        override val currentSession: StateFlow<AuthSession?> = MutableStateFlow(null)

        override suspend fun login(email: String, password: String) = Result.success(AuthSession("1", email, "Ana", "Pérez"))
        override suspend fun loginWithGoogle() = Result.failure<AuthSession>(Exception("No disponible"))
        override suspend fun register(firstName: String, lastName: String, email: String, password: String, acceptTerms: Boolean) =
            Result.success(AuthSession("1", email, firstName, lastName))

        override suspend fun resendEmailVerification(email: String) = Result.success(Unit)

        override suspend fun requestPasswordReset(email: String): Result<Unit> {
            calls++
            lastEmail = email
            return result
        }

        override suspend fun isEmailVerified() = Result.success(false)

        override suspend fun pendingChanges(): Int = 0
        override suspend fun logout(): Result<Unit> = Result.success(Unit)
    }
}
