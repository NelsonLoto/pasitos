package com.pasitos.app.core

import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pasitos.app.core.security.*
import com.pasitos.shared.api.auth.*
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SessionStoreTest {
    private val app = ApplicationProvider.getApplicationContext<Context>()
    private val prefix = "session-test-${UUID.randomUUID()}"
    private val directory = File(app.cacheDir, prefix).apply { mkdirs() }
    private val context = object : ContextWrapper(app) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int) = app.getSharedPreferences("$prefix-$name", mode)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val file = File(directory, "session.preferences_pb")
    private val preferences = PreferenceDataStoreFactory.create(scope = scope) { file }
    private val cipher = LocalCipher(context, "keyset", "${prefix}_master")
    private val store = SessionStore(preferences, cipher)
    private val account = AccountResponse("synthetic-id", "private-test@example.test", "NombrePrivado", "ApellidoPrivado", false, true, true, AccountMode.GUARDIAN, "1", false)

    @After fun cleanup() = runBlocking {
        scope.coroutineContext[Job]!!.cancelAndJoin()
        file.delete(); directory.delete(); cipher.reset()
    }

    @Test fun i0_06_tokensYCuentaSeRestauranJuntosYNoQuedanEnClaro() = runBlocking {
        val session = StoredSession(account, "private-access-test", "private-refresh-test")
        store.save(session)
        assertEquals(session, SessionStore(preferences, cipher).read())
        val disk = file.readBytes().toString(Charsets.ISO_8859_1)
        for (privateValue in listOf(account.email, account.firstName, session.accessToken!!, session.refreshToken!!)) {
            assertFalse(disk.contains(privateValue)); assertFalse(session.toString().contains(privateValue))
        }
        assertThrows(java.security.GeneralSecurityException::class.java) {
            cipher.decrypt(cipher.encrypt("token".toByteArray(), SessionStore.SESSION_AD), DatabaseKeyStore.ASSOCIATED_DATA)
        }
        Unit
    }

    @Test fun RF_CTA_07_revocacionesSeCifranYSeConservanAlBorrarSesion() = runBlocking {
        store.save(StoredSession(account, "access-test", "refresh-test"))
        store.addRevocation("pending-private-one"); store.addRevocation("pending-private-two")
        store.save(null)
        assertNull(store.read())
        assertEquals(setOf("pending-private-one", "pending-private-two"), store.pendingRevocations())
        assertFalse(file.readBytes().toString(Charsets.ISO_8859_1).contains("pending-private"))
        store.removeRevocation("pending-private-one")
        assertEquals(setOf("pending-private-two"), store.pendingRevocations())
    }
}
