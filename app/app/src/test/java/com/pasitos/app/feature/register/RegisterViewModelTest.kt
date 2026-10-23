package com.pasitos.app.feature.register

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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RegisterViewModelTest {

    private val authRepository = FakeAuthRepository()
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = RegisterViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `RN-CTA-01 la validación de la contraseña es en vivo, sin esperar al envío`() = runTest {
        val state = collectState()

        viewModel.onPasswordChange("corta1")
        assertEquals("Tiene que tener al menos 8 caracteres, con letras y números", state().passwordError)

        viewModel.onPasswordChange("secreta123")
        assertNull(state().passwordError)
    }

    @Test
    fun `la contraseña vacía no muestra error hasta que se envía`() = runTest {
        val state = collectState()

        viewModel.onPasswordChange("")

        assertNull(state().passwordError)
    }

    @Test
    fun `RF-CTA-01 CA1 el alta correcta navega a verificar el email`() = runTest {
        var verifiedEmail: String? = null
        backgroundLaunch { viewModel.navigateToEmailVerification.collect { verifiedEmail = it } }
        fillValidForm()

        viewModel.onSubmit()

        assertEquals("ana@pasitos.test", verifiedEmail)
    }

    @Test
    fun `RN-CTA-02 sin aceptar los términos no se manda el pedido`() = runTest {
        val state = collectState()
        fillValidForm()
        viewModel.onAcceptTermsChange(false)

        viewModel.onSubmit()

        assertEquals(0, authRepository.registerCalls)
        assertEquals(
            "Para crear la cuenta tenés que aceptar los términos y la política de privacidad",
            state().termsError,
        )
    }

    private fun fillValidForm() {
        viewModel.onFirstNameChange("Ana")
        viewModel.onLastNameChange("Pérez")
        viewModel.onDniChange("12345678")
        viewModel.onEmailChange("ana@pasitos.test")
        viewModel.onPasswordChange("secreta123")
        viewModel.onConfirmPasswordChange("secreta123")
        viewModel.onAcceptTermsChange(true)
    }

    private fun kotlinx.coroutines.test.TestScope.backgroundLaunch(block: suspend () -> Unit) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { block() }
    }

    private fun kotlinx.coroutines.test.TestScope.collectState(): () -> RegisterUiState {
        backgroundLaunch { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    private class FakeAuthRepository : AuthRepository {
        var registerCalls = 0

        override val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState.SignedOut())
        override val currentSession: StateFlow<AuthSession?> = MutableStateFlow(null)

        override suspend fun login(email: String, password: String) =
            Result.success(AuthSession("1", email, "Ana", "Pérez"))

        override suspend fun loginWithGoogle() = Result.failure<AuthSession>(Exception("No disponible"))

        override suspend fun register(firstName: String, lastName: String, email: String, password: String, acceptTerms: Boolean, accountType: String, dni: String?, licenseNumber: String?, specialty: String?): Result<AuthSession> {
            registerCalls++
            return Result.success(AuthSession("1", email, firstName, lastName))
        }

        override suspend fun resendEmailVerification(email: String) = Result.success(Unit)
        override suspend fun requestPasswordReset(email: String) = Result.success(Unit)
        override suspend fun isEmailVerified() = Result.success(false)

        override suspend fun pendingChanges(): Int = 0
        override suspend fun logout(): Result<Unit> = Result.success(Unit)
    }
}
