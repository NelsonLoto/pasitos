package com.pasitos.app.data.auth.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import com.pasitos.app.core.security.DatabaseKeyStore
import com.pasitos.app.core.security.FakeCipher
import com.pasitos.app.data.auth.AuthTestData.account
import com.pasitos.app.data.auth.AuthTestData.tokens
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SessionStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val cipher = FakeCipher()
    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create { File(folder.root, "secrets.preferences_pb") }
    }
    private val store by lazy { SessionStore(dataStore, cipher) }

    @Test
    fun `RF-CTA-06 sin sesión guardada no hay nada que restaurar`() = runTest {
        assertNull(store.read())
    }

    @Test
    fun `RF-CTA-06 la sesión guardada se puede leer en el próximo inicio`() = runTest {
        val session = StoredSession(account(), tokens(1))

        store.save(session)

        assertEquals(session, SessionStore(dataStore, cipher).read())
    }

    @Test
    fun `los tokens se guardan cifrados`() = runTest {
        store.save(StoredSession(account(), tokens(1)))

        val stored = dataStore.data.first()[byteArrayPreferencesKey("session")]!!.decodeToString()
        assertFalse(stored.contains("refresh-1"))
        assertFalse(stored.contains("access-1"))
    }

    @Test
    fun `una sesión vencida conserva la cuenta sin tokens`() = runTest {
        store.save(StoredSession(account(), tokens = null))

        val restored = store.read()
        assertEquals(account(), restored?.account)
        assertNull(restored?.tokens)
    }

    @Test
    fun `si la sesión ya no se puede descifrar no la devuelve`() = runTest {
        store.save(StoredSession(account(), tokens(1)))
        cipher.keystoreKeyLost = true

        assertNull(store.read())
    }

    @Test
    fun `la sesión no se puede descifrar como si fuera la clave de la base ni al revés`() = runTest {
        store.save(StoredSession(account(), tokens(1)))
        val keyStore = DatabaseKeyStore(dataStore, cipher)
        val encryptedSession = dataStore.data.first()[byteArrayPreferencesKey("session")]!!

        dataStore.edit { it[byteArrayPreferencesKey("database_key")] = encryptedSession }
        assertNull(keyStore.read())

        keyStore.create()
        dataStore.edit { it[byteArrayPreferencesKey("session")] = it[byteArrayPreferencesKey("database_key")]!! }
        assertNull(store.read())
    }

    @Test
    fun `RF-CTA-07 CA3 las revocaciones pendientes se guardan cifradas y sobreviven al cierre de sesión`() = runTest {
        store.save(StoredSession(account(), tokens(1)))

        store.addPendingRevocation("refresh-1")
        store.addPendingRevocation("refresh-2")
        store.delete()

        assertEquals(listOf("refresh-1", "refresh-2"), SessionStore(dataStore, cipher).pendingRevocations())
        val stored = dataStore.data.first()[byteArrayPreferencesKey("pending_revocations")]!!.decodeToString()
        assertFalse(stored.contains("refresh-1"))
    }

    @Test
    fun `una revocación hecha sale de la lista`() = runTest {
        store.addPendingRevocation("refresh-1")
        store.addPendingRevocation("refresh-2")

        store.removePendingRevocations(listOf("refresh-1"))

        assertEquals(listOf("refresh-2"), store.pendingRevocations())
        store.removePendingRevocations(listOf("refresh-2"))
        assertNull(dataStore.data.first()[byteArrayPreferencesKey("pending_revocations")])
    }

    @Test
    fun `RF-CTA-07 al borrarla no queda sesión y la clave de la base sigue`() = runTest {
        val keyStore = DatabaseKeyStore(dataStore, cipher)
        keyStore.create()
        store.save(StoredSession(account(), tokens(1)))

        store.delete()

        assertNull(store.read())
        assertNotNull(keyStore.read())
    }
}
