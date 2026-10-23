package com.pasitos.app.data.account

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.pasitos.app.core.network.ApiException
import com.pasitos.app.core.network.FakeServer
import com.pasitos.app.core.network.FakeServer.Companion.bodyText
import com.pasitos.app.core.network.FakeServer.Companion.error
import com.pasitos.app.core.network.FakeServer.Companion.json
import com.pasitos.app.core.network.createHttpClient
import com.pasitos.app.core.security.FakeCipher
import com.pasitos.app.data.auth.AuthTestData.account
import com.pasitos.app.data.auth.AuthTestData.tokens
import com.pasitos.app.data.auth.SessionRepository
import com.pasitos.app.data.auth.local.SessionStore
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.account.ChangePasswordRequest
import com.pasitos.shared.api.account.EmailChangeRequest
import com.pasitos.shared.api.account.PatchMeRequest
import com.pasitos.shared.api.auth.MeResponse
import com.pasitos.shared.api.auth.PasswordResetRequest
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** `/api/v1/me` desde la app (RF-CTA-11, RN-CTA-02). Con Robolectric porque la sesión usa el log de Android. */
@RunWith(RobolectricTestRunner::class)
class AccountApiRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create { File(folder.root, "secrets.preferences_pb") }
    }
    private val session by lazy { SessionRepository(SessionStore(dataStore, FakeCipher())) }
    private val server = FakeServer(validAccessToken = "access-1")
    private val repository by lazy {
        AccountApiRepository(createHttpClient(server.engine, "http://localhost", session), session)
    }

    @Before
    fun signIn() = runTest {
        session.start(account(), tokens(1))
    }

    @Test
    fun `RN-CTA-02 GET me guarda los datos de la cuenta y los términos pendientes`() = runTest {
        server.routes["/api/v1/me"] = {
            json(Json.encodeToString(MeResponse(account(firstName = "Anita").copy(hasPassword = false), pendingTermsVersion = "2")))
        }

        repository.refresh().getOrThrow()

        assertEquals("2", repository.pendingTermsVersion.value)
        assertEquals("Anita", session.session.value?.firstName)
        assertEquals(false, session.session.value?.hasPassword)
    }

    @Test
    fun `RN-CTA-02 aceptar los términos los deja de pedir`() = runTest {
        server.routes["/api/v1/me"] = { json(Json.encodeToString(MeResponse(account(), pendingTermsVersion = "2"))) }
        server.routes["/api/v1/me/consents"] = { respond("", HttpStatusCode.NoContent) }
        repository.refresh().getOrThrow()

        repository.acceptTerms().getOrThrow()

        assertNull(repository.pendingTermsVersion.value)
        assertEquals(HttpMethod.Post, server.requestsTo("/api/v1/me/consents").single().method)
    }

    @Test
    fun `RF-CTA-11 el nombre se guarda con PATCH me y se actualiza en la sesión`() = runTest {
        server.routes["/api/v1/me"] = { json(Json.encodeToString(account(firstName = "Anita"))) }

        repository.updateName(" Anita ", "Pérez").getOrThrow()

        val request = server.requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals(PatchMeRequest("Anita", "Pérez"), Json.decodeFromString<PatchMeRequest>(request.bodyText()))
        assertEquals("Anita", session.session.value?.firstName)
    }

    @Test
    fun `RF-CTA-11 CA1 y CA2 el email y la contraseña se mandan con la contraseña actual`() = runTest {
        server.routes["/api/v1/me/email-change"] = { respond("", HttpStatusCode.NoContent) }
        server.routes["/api/v1/me/password"] = { respond("", HttpStatusCode.NoContent) }

        repository.requestEmailChange(" nueva@pasitos.test ", "secreta123").getOrThrow()
        repository.changePassword("secreta123", "nueva1234").getOrThrow()

        assertEquals(
            EmailChangeRequest("nueva@pasitos.test", "secreta123"),
            Json.decodeFromString<EmailChangeRequest>(server.requestsTo("/api/v1/me/email-change").single().bodyText()),
        )
        assertEquals(
            ChangePasswordRequest("secreta123", "nueva1234"),
            Json.decodeFromString<ChangePasswordRequest>(server.requestsTo("/api/v1/me/password").single().bodyText()),
        )
    }

    @Test
    fun `RF-CTA-11 CA2 con la contraseña actual incorrecta llega el mensaje del servidor`() = runTest {
        server.routes["/api/v1/me/password"] = {
            error(HttpStatusCode.Unauthorized, ErrorCode.INVALID_CREDENTIALS, "La contraseña actual no es correcta")
        }

        val error = repository.changePassword("otra1234", "nueva1234").exceptionOrNull() as ApiException

        assertEquals(ErrorCode.INVALID_CREDENTIALS, error.code)
        assertEquals("La contraseña actual no es correcta", error.message)
    }

    @Test
    fun `RF-CTA-11 CA4 crear una contraseña pide el email de recuperación para la cuenta propia, sin token`() = runTest {
        server.routes["/api/v1/auth/password-reset"] = { respond("", HttpStatusCode.NoContent) }

        repository.requestPasswordCreation().getOrThrow()

        val request = server.requests.single()
        assertNull(request.headers[HttpHeaders.Authorization])
        assertEquals(PasswordResetRequest(account().email), Json.decodeFromString<PasswordResetRequest>(request.bodyText()))
    }
}
