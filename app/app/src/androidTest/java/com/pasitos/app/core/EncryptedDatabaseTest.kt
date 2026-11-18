package com.pasitos.app.core

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pasitos.app.core.database.LocalDatabase
import com.pasitos.app.core.database.SyncState
import com.pasitos.app.core.security.DatabaseKeyStore
import com.pasitos.app.core.security.LocalCipher
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseTest {
    private val app = ApplicationProvider.getApplicationContext<Context>()
    private val prefix = "cipher-test-${UUID.randomUUID()}"
    private val directory = File(app.cacheDir, prefix).apply { mkdirs() }
    private val context = object : ContextWrapper(app) {
        override fun getApplicationContext(): Context = this
        override fun getDatabasePath(name: String) = File(directory, name)
        override fun deleteDatabase(name: String): Boolean = SQLiteDatabase.deleteDatabase(getDatabasePath(name))
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            app.getSharedPreferences("$prefix-$name", mode)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val secretsFile = File(directory, "secrets.preferences_pb")
    private val secrets = PreferenceDataStoreFactory.create(scope = scope) { secretsFile }
    private val cipher = LocalCipher(context, "test_keyset", "${prefix}_master")
    private val keys = DatabaseKeyStore(secrets, cipher)
    private val opened = mutableListOf<LocalDatabase>()
    private fun local() = LocalDatabase(context, keys).also(opened::add)

    @After fun cleanOwnTestFiles() = runBlocking(Dispatchers.IO) {
        opened.forEach { it.get().close() }
        scope.coroutineContext[Job]!!.cancelAndJoin()
        context.deleteDatabase(LocalDatabase.NAME)
        secretsFile.delete()
        cipher.reset()
    }

    @Test fun i0_07_CA1ArchivoCifradoRechazaSQLiteYSeReabreConLaMismaClave() = runBlocking(Dispatchers.IO) {
        val first = local().get()
        first.syncState().save(SyncState(cursor = 42))
        first.close()
        val file = context.getDatabasePath(LocalDatabase.NAME)
        assertTrue(file.exists())
        val header = file.inputStream().use { stream -> ByteArray(16).also { stream.read(it) }.toString(Charsets.US_ASCII) }
        assertFalse(header.startsWith("SQLite format 3"))
        try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { viewer ->
                viewer.rawQuery("SELECT * FROM sync_state", null).use { it.moveToFirst() }
            }
            fail("SQLite sin clave no debe leer la base")
        } catch (_: SQLiteException) { }
        assertEquals(42L, local().get().syncState().read()!!.cursor)
    }

    @Test fun i0_07BorrarBaseYClaveNoDejaDatosDeLaSesionAnterior() = runBlocking(Dispatchers.IO) {
        val database = local()
        database.get().syncState().save(SyncState(cursor = 90))
        val oldKey = keys.getOrCreate().bytes
        try {
            database.wipe()
            assertFalse(context.getDatabasePath(LocalDatabase.NAME).exists())
            assertNull(secrets.data.first()[DatabaseKeyStore.KEY])
            assertNull(database.get().syncState().read())
            val newKey = keys.getOrCreate().bytes
            try { assertFalse(oldKey.contentEquals(newKey)) } finally { newKey.fill(0) }
        } finally { oldKey.fill(0) }
    }

    @Test fun i0_07ClaveGuardadaCorruptaRecreaLaCopiaLocalVacia() = runBlocking(Dispatchers.IO) {
        val database = local().get()
        database.syncState().save(SyncState(cursor = 55))
        database.close()
        secrets.edit { it[DatabaseKeyStore.KEY] = "clave-corrupta" }
        assertNull(local().get().syncState().read())
    }

    @Test fun ADR17ElDatoAsociadoImpideUsarLaClaveComoToken() {
        val synthetic = ByteArray(32) { it.toByte() }
        val encrypted = cipher.encrypt(synthetic, "database-key")
        assertArrayEquals(synthetic, cipher.decrypt(encrypted, "database-key"))
        assertThrows(GeneralSecurityException::class.java) { cipher.decrypt(encrypted, "session-token") }
        val master = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            .getKey("${prefix}_master", null)
        assertNotNull(master)
        assertNull("La clave maestra no debe poder extraerse", master.encoded)
    }

    @Test fun i0_07KeystoreInvalidadoRecreaBaseYBorraSecretosQueYaNoSePuedenLeer() = runBlocking(Dispatchers.IO) {
        val database = local().get()
        database.syncState().save(SyncState(cursor = 80))
        database.close()
        val sessionMarker = stringPreferencesKey("synthetic_session")
        secrets.edit { it[sessionMarker] = "synthetic-encrypted-session" }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("${prefix}_master") }
        // Una instancia nueva simula un arranque sin el cifrador que estaba en RAM.
        val reloaded = LocalCipher(context, "test_keyset", "${prefix}_master")
        val recovered = LocalDatabase(context, DatabaseKeyStore(secrets, reloaded)).also(opened::add)
        assertNull(recovered.get().syncState().read())
        assertNull(secrets.data.first()[sessionMarker])
    }
}
