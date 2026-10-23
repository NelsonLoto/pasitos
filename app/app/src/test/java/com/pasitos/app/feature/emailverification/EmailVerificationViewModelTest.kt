package com.pasitos.app.feature.emailverification

import androidx.lifecycle.SavedStateHandle
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EmailVerificationViewModelTest {

    private val authRepository = FakeAuthRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(afterVerifiedGoHome: Boolean = false) = EmailVerificationViewModel(
        authRepository,
        SavedStateHandle(mapOf("email" to "ana@pasitos.test", "afterVerifiedGoHome" to afterVerifiedGoHome)),
    )

    @Test
    fun `RF-CTA-02 CA2 ya verifiqué navega si el email ya está verificado`() = runTest {
        authRepository.isEmailVerifiedResult = Result.success(true)
        val viewModel = viewModel()
        var navigated = false
        backgroundLaunch { viewModel.navigateNext.collect { navigated = true } }

        viewModel.onAlreadyVerifiedClick()

        assertTrue(navigated)
    }

    @Test
    fun `RF-CTA-02 CA1 ya verifiqué avisa si todavía no se verificó`() = runTest {
        authRepository.isEmailVerifiedResult = Result.success(false)
        val viewModel = viewModel()
        val state = collectState(viewModel)

        viewModel.onAlreadyVerifiedClick()

        assertEquals("Todavía no verificaste tu email. Revisá tu bandeja de entrada.", state().message)
    }

    @Test
    fun `un error de conexión al comprobar muestra el motivo`() = runTest {
        authRepository.isEmailVerifiedResult = Result.failure(ApiException(ErrorCode.UNAUTHORIZED, "Tu sesión venció"))
        val viewModel = viewModel()
        val state = collectState(viewModel)

        viewModel.onAlreadyVerifiedClick()

        assertEquals("Tu sesión venció", state().message)
    }

    @Test
    fun `reenviar llama al repositorio con el email de la ruta`() = runTest {
        val viewModel = viewModel()
        val state = collectState(viewModel)

        viewModel.onResendClick()

        assertEquals("ana@pasitos.test", authRepository.lastResendEmail)
        assertEquals("Te volvimos a mandar el email de verificación.", state().message)
    }

    private fun kotlinx.coroutines.test.TestScope.backgroundLaunch(block: suspend () -> Unit) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { block() }
    }

    private fun kotlinx.coroutines.test.TestScope.collectState(viewModel: EmailVerificationViewModel): () -> EmailVerificationUiState {
        backgroundLaunch { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    private class FakeAuthRepository : AuthRepository {
        var isEmailVerifiedResult: Result<Boolean> = Result.success(false)
        var lastResendEmail: String? = null

        override val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState.SignedOut())
        override val currentSession: StateFlow<AuthSession?> = MutableStateFlow(null)

        override suspend fun login(email: String, password: String) = Result.success(AuthSession("1", email, "Ana", "Pérez"))
        override suspend fun loginWithGoogle() = Result.failure<AuthSession>(Exception("No disponible"))
        override suspend fun register(firstName: String, lastName: String, email: String, password: String, acceptTerms: Boolean, accountType: String, dni: String?, licenseNumber: String?, specialty: String?) =
            Result.success(AuthSession("1", email, firstName, lastName))

        override suspend fun resendEmailVerification(email: String): Result<Unit> {
            lastResendEmail = email
            return Result.success(Unit)
        }

        override suspend fun requestPasswordReset(email: String) = Result.success(Unit)
        override suspend fun isEmailVerified() = isEmailVerifiedResult

        override suspend fun pendingChanges(): Int = 0
        override suspend fun logout(): Result<Unit> = Result.success(Unit)
    }
}
