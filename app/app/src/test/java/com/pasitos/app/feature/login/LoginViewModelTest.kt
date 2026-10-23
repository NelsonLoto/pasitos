package com.pasitos.app.feature.login

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pasitos.app.core.network.ApiException
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.auth.google.GoogleCredentialResult
import com.pasitos.app.data.auth.google.GoogleCredentials
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
    private val googleCredentials = FakeGoogleCredentials()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = LoginViewModel(authRepository, googleCredentials)
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

    @Test
    fun `RF-CTA-03 con una cuenta que ya existe, el inicio con Google navega a inicio`() = runTest {
        var navigated = false
        backgroundLaunch { viewModel.navigateToHome.collect { navigated = true } }

        viewModel.onGoogleSignInClick(context)

        assertTrue(navigated)
        assertEquals(listOf(GoogleCall(TOKEN, acceptTerms = false)), authRepository.googleCalls)
    }

    @Test
    fun `RF-CTA-03 CA3 cerrar el diálogo de Google deja el login sin error`() = runTest {
        googleCredentials.result = GoogleCredentialResult.Cancelled
        val state = collectState()

        viewModel.onGoogleSignInClick(context)

        assertNull(state().generalError)
        assertFalse(state().isLoading)
        assertTrue(authRepository.googleCalls.isEmpty())
    }

    @Test
    fun `si Credential Manager falla se muestra el error`() = runTest {
        googleCredentials.result = GoogleCredentialResult.Failure("No pudimos conectar con Google. Probá de nuevo.")
        val state = collectState()

        viewModel.onGoogleSignInClick(context)

        assertEquals("No pudimos conectar con Google. Probá de nuevo.", state().generalError)
        assertTrue(authRepository.googleCalls.isEmpty())
    }

    @Test
    fun `RF-CTA-03 CA1 el primer ingreso muestra los términos sin marcar, con nombre y apellido de Google`() = runTest {
        authRepository.googleResult = { if (it) SIGNED_IN else termsRequired() }
        val state = collectState()
        var navigated = false
        backgroundLaunch { viewModel.navigateToHome.collect { navigated = true } }

        viewModel.onGoogleSignInClick(context)

        assertEquals(GoogleSignUpUiState(email = "ana@gmail.com", firstName = "Ana", lastName = "Pérez"), state().googleSignUp)
        assertFalse(state().googleSignUp!!.acceptTerms)
        assertFalse(navigated)
        // El token queda solo en el ViewModel, nunca en el estado
        assertFalse(state().toString().contains(TOKEN))
    }

    @Test
    fun `RF-CTA-03 CA1 sin aceptar los términos o sin nombre no se crea la cuenta`() = runTest {
        authRepository.googleResult = { if (it) SIGNED_IN else termsRequired() }
        val state = collectState()
        viewModel.onGoogleSignInClick(context)
        viewModel.onGoogleFirstNameChange(" ")

        viewModel.onGoogleSignUpSubmit()

        val signUp = state().googleSignUp!!
        assertEquals("Ingresá tu nombre", signUp.firstNameError)
        assertEquals("Para crear la cuenta tenés que aceptar los términos y la política de privacidad", signUp.termsError)
        assertEquals(1, authRepository.googleCalls.size)
    }

    @Test
    fun `RF-CTA-03 CA1 al aceptar reenvía el mismo token con los términos y el nombre confirmado`() = runTest {
        authRepository.googleResult = { if (it) SIGNED_IN else termsRequired() }
        val state = collectState()
        var navigated = false
        backgroundLaunch { viewModel.navigateToHome.collect { navigated = true } }
        viewModel.onGoogleSignInClick(context)
        viewModel.onGoogleFirstNameChange("Anita")
        viewModel.onGoogleAcceptTermsChange(true)

        viewModel.onGoogleSignUpSubmit()

        assertEquals(GoogleCall(TOKEN, acceptTerms = true, firstName = "Anita", lastName = "Pérez"), authRepository.googleCalls.last())
        assertTrue(navigated)
        assertNull(state().googleSignUp)
    }

    @Test
    fun `cancelar el paso de términos vuelve al login y descarta el token`() = runTest {
        authRepository.googleResult = { if (it) SIGNED_IN else termsRequired() }
        val state = collectState()
        viewModel.onGoogleSignInClick(context)
        viewModel.onGoogleAcceptTermsChange(true)

        viewModel.onGoogleSignUpCancel()
        viewModel.onGoogleSignUpSubmit()

        assertNull(state().googleSignUp)
        assertEquals(1, authRepository.googleCalls.size)
    }

    @Test
    fun `si el token de Google venció antes de confirmar, vuelve al login con el aviso`() = runTest {
        authRepository.googleResult = {
            if (it) Result.failure(ApiException(ErrorCode.INVALID_GOOGLE_TOKEN, "No pudimos validar tu cuenta de Google. Probá de nuevo")) else termsRequired()
        }
        val state = collectState()
        viewModel.onGoogleSignInClick(context)
        viewModel.onGoogleAcceptTermsChange(true)

        viewModel.onGoogleSignUpSubmit()

        assertNull(state().googleSignUp)
        assertEquals("Pasó mucho tiempo. Tocá \"Continuar con Google\" de nuevo.", state().generalError)
    }

    private fun termsRequired() = Result.failure<AuthSession>(
        ApiException(ErrorCode.TERMS_REQUIRED, "Para crear la cuenta tenés que aceptar los términos y la política de privacidad"),
    )

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

        val googleCalls = mutableListOf<GoogleCall>()

        /** Según `acceptTerms`; por defecto, una cuenta que ya existe. */
        var googleResult: (acceptTerms: Boolean) -> Result<AuthSession> = { SIGNED_IN }

        override suspend fun loginWithGoogle(idToken: String, acceptTerms: Boolean, firstName: String?, lastName: String?): Result<AuthSession> {
            googleCalls += GoogleCall(idToken, acceptTerms, firstName, lastName)
            return googleResult(acceptTerms)
        }

        override suspend fun register(firstName: String, lastName: String, email: String, password: String, acceptTerms: Boolean) =
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

    private data class GoogleCall(
        val idToken: String,
        val acceptTerms: Boolean,
        val firstName: String? = null,
        val lastName: String? = null,
    )

    private class FakeGoogleCredentials : GoogleCredentials {
        var result: GoogleCredentialResult =
            GoogleCredentialResult.Success(TOKEN, email = "ana@gmail.com", givenName = "Ana", familyName = "Pérez")

        override suspend fun request(activityContext: Context) = result
        override suspend fun clear() = Unit
    }

    private companion object {
        const val TOKEN = "id-token-de-google"
        val SIGNED_IN = Result.success(AuthSession("1", "ana@gmail.com", "Ana", "Pérez"))
    }
}
