package com.pasitos.app.feature.login

import com.pasitos.app.core.network.ApiException
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.auth.model.AuthSession
import com.pasitos.app.data.auth.model.SessionState
import com.pasitos.shared.api.ErrorCode
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
class LoginViewModelTest {

    private val authRepository = FakeAuthRepository()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `RF-CTA-04 CA1 el login correcto navega a inicio`() = runTest {
        val state = collectState()
        var navigated = false
        backgroundLaunch { viewModel.navigateToHome.collect { navigated = true } }
        viewModel.onEmailChange("ana@pasitos.test")
        viewModel.onPasswordChange("secreta123")

        viewModel.onSubmit()

        assertTrue(navigated)
        assertFalse(state().isLoading)
    }

    @Test
    fun `RF-CTA-04 CA2 credenciales incorrectas muestran el mensaje del servidor`() = runTest {
        authRepository.loginResult = Result.failure(ApiException(ErrorCode.INVALID_CREDENTIALS, "Email o contraseña incorrectos"))
        val state = collectState()
        viewModel.onEmailChange("ana@pasitos.test")
        viewModel.onPasswordChange("otraClave")

        viewModel.onSubmit()

        assertEquals("Email o contraseña incorrectos", state().generalError)
        assertFalse(state().emailVerificationPending)
    }

    @Test
    fun `RF-CTA-02 CA1 el email sin verificar muestra la pantalla de verificación, no un error`() = runTest {
        authRepository.loginResult = Result.failure(ApiException(ErrorCode.EMAIL_NOT_VERIFIED, "Tenés que verificar tu email"))
        val state = collectState()
        viewModel.onEmailChange("ana@pasitos.test")
        viewModel.onPasswordChange("secreta123")

        viewModel.onSubmit()

        assertTrue(state().emailVerificationPending)
        assertNull(state().generalError)
    }

    @Test
    fun `RF-CTA-02 CA1 reenviar la verificación llama al repositorio y avisa`() = runTest {
        val state = collectState()
        viewModel.onEmailChange("ana@pasitos.test")

        viewModel.onResendVerificationClick()

        assertEquals(1, authRepository.resendCalls)
        assertEquals("ana@pasitos.test", authRepository.lastResendEmail)
        assertEquals("Te volvimos a mandar el email de verificación.", state().verificationMessage)
    }

    @Test
    fun `Ya verifiqué reintenta el login y si sigue sin verificar lo avisa`() = runTest {
        authRepository.loginResult = Result.failure(ApiException(ErrorCode.EMAIL_NOT_VERIFIED, "Tenés que verificar tu email"))
        val state = collectState()
        viewModel.onEmailChange("ana@pasitos.test")
        viewModel.onPasswordChange("secreta123")
        viewModel.onSubmit()

        viewModel.onAlreadyVerifiedClick()

        assertEquals(2, authRepository.loginCalls)
        assertTrue(state().emailVerificationPending)
        assertEquals("Todavía no verificaste tu email. Revisá tu bandeja de entrada.", state().verificationMessage)
    }

    @Test
    fun `Ya verifiqué navega a inicio si ahora el login anda`() = runTest {
        authRepository.loginResult = Result.failure(ApiException(ErrorCode.EMAIL_NOT_VERIFIED, "Tenés que verificar tu email"))
        var navigated = false
        backgroundLaunch { viewModel.navigateToHome.collect { navigated = true } }
        viewModel.onEmailChange("ana@pasitos.test")
        viewModel.onPasswordChange("secreta123")
        viewModel.onSubmit()
        authRepository.loginResult = Result.success(AuthSession("1", "ana@pasitos.test", "Ana", "Pérez"))

        viewModel.onAlreadyVerifiedClick()

        assertTrue(navigated)
    }

    @Test
    fun `volver desde la verificación limpia el estado`() = runTest {
        authRepository.loginResult = Result.failure(ApiException(ErrorCode.EMAIL_NOT_VERIFIED, "Tenés que verificar tu email"))
        val state = collectState()
        viewModel.onEmailChange("ana@pasitos.test")
        viewModel.onPasswordChange("secreta123")
        viewModel.onSubmit()

        viewModel.onBackFromVerificationClick()

        assertFalse(state().emailVerificationPending)
        assertNull(state().verificationMessage)
    }

    private fun kotlinx.coroutines.test.TestScope.backgroundLaunch(block: suspend () -> Unit) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { block() }
    }

    private fun kotlinx.coroutines.test.TestScope.collectState(): () -> LoginUiState {
        backgroundLaunch { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    private class FakeAuthRepository : AuthRepository {
        var loginCalls = 0
        var loginResult: Result<AuthSession> = Result.success(AuthSession("1", "ana@pasitos.test", "Ana", "Pérez"))
        var resendCalls = 0
        var lastResendEmail: String? = null

        override val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState.SignedOut())
        override val currentSession: StateFlow<AuthSession?> = MutableStateFlow(null)

        override suspend fun login(email: String, password: String): Result<AuthSession> {
            loginCalls++
            return loginResult
        }

        override suspend fun loginWithGoogle() = Result.failure<AuthSession>(Exception("No disponible"))
        override suspend fun register(firstName: String, lastName: String, email: String, password: String, acceptTerms: Boolean, accountType: String, dni: String?, licenseNumber: String?, specialty: String?) =
            Result.success(AuthSession("1", email, firstName, lastName))

        override suspend fun resendEmailVerification(email: String): Result<Unit> {
            resendCalls++
            lastResendEmail = email
            return Result.success(Unit)
        }

        override suspend fun requestPasswordReset(email: String) = Result.success(Unit)
        override suspend fun isEmailVerified() = Result.success(true)

        override suspend fun pendingChanges(): Int = 0
        override suspend fun logout(): Result<Unit> = Result.success(Unit)
    }
}
