package com.pasitos.app.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.pasitos.app.core.network.RefreshResult
import com.pasitos.app.core.security.FakeCipher
import com.pasitos.app.data.auth.AuthTestData.account
import com.pasitos.app.data.auth.AuthTestData.tokens
import com.pasitos.app.data.auth.local.SessionStore
import com.pasitos.app.data.auth.local.StoredSession
import com.pasitos.app.data.auth.model.SessionState
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Con Robolectric porque [SessionRepository] registra en el log de Android. */
@RunWith(RobolectricTestRunner::class)
class SessionRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create { File(folder.root, "secrets.preferences_pb") }
    }
    private val store by lazy { SessionStore(dataStore, FakeCipher()) }

    /** Una instancia nueva equivale a volver a abrir la app: lee lo guardado. */
    private suspend fun openApp(): SessionRepository =
        SessionRepository(store).also { repository -> repository.state.first { it != SessionState.Restoring } }

    @Test
    fun `RF-CTA-06 sin sesión guardada la app abre en el login`() = runTest {
        val repository = openApp()

        assertEquals(SessionState.SignedOut(expired = false), repository.state.value)
        assertNull(repository.current())
    }

    @Test
    fun `RF-CTA-11 los datos nuevos de la cuenta reemplazan a los guardados sin tocar los tokens`() = runTest {
        val repository = openApp()
        repository.start(account(), tokens(1))

        repository.updateAccount(account(firstName = "Anita").copy(hasPassword = false))
        repository.updateAccount(account(id = "otra-cuenta", firstName = "Otra"))

        val reopened = openApp()
        val session = (reopened.state.value as SessionState.SignedIn).session
        assertEquals("Anita", session.firstName)
        assertEquals(false, session.hasPassword)
        assertEquals(tokens(1), reopened.current())
    }

    @Test
    fun `RF-CTA-06 CA1 al volver a abrir la app la sesión sigue iniciada`() = runTest {
        openApp().start(account(), tokens(1))

        val reopened = openApp()

        assertEquals("Ana", (reopened.state.value as SessionState.SignedIn).session.firstName)
        assertEquals(tokens(1), reopened.current())
    }

    @Test
    fun `los tokens renovados reemplazan a los anteriores y sobreviven al cierre de la app`() = runTest {
        val repository = openApp()
        repository.start(account(), tokens(1))

        val renewed = repository.refresh("access-1") { refreshToken ->
            assertEquals("refresh-1", refreshToken)
            RefreshResult.Renewed(tokens(2))
        }

        assertEquals(tokens(2), renewed)
        assertEquals(tokens(2), openApp().current())
    }

    @Test
    fun `si otro pedido ya renovó los tokens no se vuelve a usar el de renovación`() = runTest {
        val repository = openApp()
        repository.start(account(), tokens(1))
        repository.refresh("access-1") { RefreshResult.Renewed(tokens(2)) }

        val result = repository.refresh("access-1") { error("No tiene que renovar de nuevo") }

        assertEquals(tokens(2), result)
    }

    @Test
    fun `RF-CTA-06 CA2 si el servidor rechaza la renovación la sesión vence y conserva la cuenta`() = runTest {
        val repository = openApp()
        repository.start(account(), tokens(1))

        val result = repository.refresh("access-1") { RefreshResult.Rejected }

        assertNull(result)
        assertEquals(SessionState.SignedOut(expired = true), repository.state.value)
        assertNull(repository.session.value)
        assertEquals(StoredSession(account(), tokens = null), store.read())
        assertEquals(SessionState.SignedOut(expired = true), openApp().state.value)
    }

    @Test
    fun `si el servidor no puede renovar en ese momento la sesión sigue`() = runTest {
        val repository = openApp()
        repository.start(account(), tokens(1))

        val result = repository.refresh("access-1") { RefreshResult.Unavailable }

        assertNull(result)
        assertEquals(tokens(1), repository.current())
        assertEquals(account().id, (repository.state.value as SessionState.SignedIn).session.userId)
    }

    @Test
    fun `iniciar sesión con otra cuenta después de vencer avisa que los datos locales son de otra`() = runTest {
        val repository = openApp()
        repository.start(account(id = "cuenta-anterior"), tokens(1))
        repository.refresh("access-1") { RefreshResult.Rejected }

        assertEquals("cuenta-anterior", repository.start(account(id = "cuenta-nueva"), tokens(2)))
        assertNull(openApp().start(account(id = "cuenta-nueva"), tokens(3)))
    }

    @Test
    fun `RF-CTA-07 al cerrar sesión no queda nada guardado`() = runTest {
        val repository = openApp()
        repository.start(account(), tokens(1))

        repository.clear()

        assertEquals(SessionState.SignedOut(expired = false), repository.state.value)
        assertNull(store.read())
        assertNull(openApp().current())
    }
}
