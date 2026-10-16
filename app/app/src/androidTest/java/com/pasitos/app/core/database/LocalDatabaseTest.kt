package com.pasitos.app.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pasitos.app.core.security.DatabaseKeyStore
import com.pasitos.app.core.security.KeystoreCipher
import com.pasitos.app.core.security.LocalCipher
import com.pasitos.app.core.sync.local.MutationEntity
import com.pasitos.app.core.sync.local.MutationOp
import com.pasitos.app.core.sync.local.PendingMutationEntity
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import net.zetetic.database.sqlcipher.SQLiteConnection
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase

/**
 * Base local con SQLCipher, Tink y el Android Keystore reales (ADR-17). Corre en emulador o
 * dispositivo: el Keystore no existe en la JVM. Usa base, DataStore, keyset y clave maestra propios
 * para no tocar los de la app.
 *
 * Cada `LocalDatabase` nueva sobre los mismos archivos equivale a un proceso nuevo de la app, por
 * ejemplo el que arranca después de cerrar sesión.
 */
@RunWith(AndroidJUnit4::class)
class LocalDatabaseTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testId = UUID.randomUUID().toString()
    private val databaseName = "test-$testId.db"
    private val dataStoreFile = File(context.filesDir, "datastore/test-$testId.preferences_pb")
    private val keysetFile = "test_keyset_$testId"
    private val masterKeyAlias = "test_master_key_$testId"

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var keyStore: DatabaseKeyStore
    private lateinit var localDatabase: LocalDatabase

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create { dataStoreFile }
        keyStore = newKeyStore()
        localDatabase = LocalDatabase(context, keyStore, databaseName)
    }

    @After
    fun tearDown() {
        runBlocking { localDatabase.wipe() }
        newCipher().reset()
        dataStoreFile.delete()
    }

    @Test
    fun databaseFileCannotBeOpenedWithoutTheKey() = runTest {
        localDatabase.database.pendingMutationDao().insert(mutation())
        localDatabase.database.close()

        val file = context.getDatabasePath(databaseName)
        val header = ByteArray(SQLITE_HEADER.size).also { bytes -> file.inputStream().use { it.read(bytes) } }
        assertFalse("El archivo empieza como una base SQLite sin cifrar", header.contentEquals(SQLITE_HEADER))
        assertThrows(SQLiteException::class.java) {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { plain ->
                plain.rawQuery("SELECT count(*) FROM sqlite_master", null).use { it.moveToFirst() }
            }
        }
    }

    @Test
    fun sqlCipherUsesTheStoredKeyAsRawKey() = runTest {
        localDatabase.database.pendingMutationDao().insert(mutation())
        localDatabase.database.close()

        // Con una clave cruda, SQLCipher no deriva nada: abre igual con 1 iteración de PBKDF2 en
        // lugar de 256.000. Si la tratara como contraseña, derivaría otra clave y no podría leer.
        val oneKdfIteration = object : SQLiteDatabaseHook {
            override fun preKey(connection: SQLiteConnection) = Unit
            override fun postKey(connection: SQLiteConnection) =
                connection.executeRaw("PRAGMA kdf_iter = 1", null, null)
        }
        val rawKey = keyStore.read()!!.toRawKey()
        val path = context.getDatabasePath(databaseName).path
        CipherDatabase.openDatabase(path, rawKey, null, CipherDatabase.OPEN_READWRITE, oneKdfIteration).use { db ->
            db.rawQuery("SELECT count(*) FROM pending_mutations", arrayOf<String>()).use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    @Test
    fun databaseReopensWithTheStoredKey() = runTest {
        localDatabase.database.pendingMutationDao().insert(mutation())
        localDatabase.database.close()

        val nextProcess = LocalDatabase(context, newKeyStore(), databaseName)

        assertEquals(1, nextProcess.database.pendingMutationDao().count())
        nextProcess.database.close()
    }

    @Test
    fun wipeDeletesTheFileAndTheKey() = runTest {
        localDatabase.database.pendingMutationDao().insert(mutation())
        val oldKey = keyStore.read()!!

        localDatabase.wipe()

        assertFalse(context.getDatabasePath(databaseName).exists())
        assertNull(keyStore.read())
        // Después de cerrar sesión la app se reinicia: la base nueva está vacía y tiene otra clave
        val nextProcess = LocalDatabase(context, newKeyStore(), databaseName)
        assertEquals(0, nextProcess.database.pendingMutationDao().count())
        assertFalse(oldKey.contentEquals(keyStore.read()!!))
        nextProcess.database.close()
    }

    @Test
    fun unreadableKeyStartsOverWithAnEmptyDatabase() = runTest {
        localDatabase.database.pendingMutationDao().insert(mutation())
        localDatabase.database.close()

        // Como si la clave guardada se hubiera cifrado con un keyset que ya no existe
        val nextProcess = LocalDatabase(context, DatabaseKeyStore(dataStore, UnreadableCipher), databaseName)

        assertEquals(0, nextProcess.database.pendingMutationDao().count())
        nextProcess.database.close()
    }

    @Test
    fun lostKeystoreKeyRegeneratesAllKeysAndStartsOver() = runTest {
        localDatabase.database.pendingMutationDao().insert(mutation())
        localDatabase.database.close()
        dataStore.edit { it[stringPreferencesKey("refresh_token")] = "cifrado-con-la-clave-perdida" }

        // El sistema pierde la clave maestra: el keyset guardado ya no se puede descifrar
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(masterKeyAlias)
        val nextProcess = LocalDatabase(context, newKeyStore(), databaseName)

        assertEquals(0, nextProcess.database.pendingMutationDao().count())
        assertNull(dataStore.data.first()[stringPreferencesKey("refresh_token")])
        assertNotNull(newKeyStore().read())
        nextProcess.database.close()
    }

    private fun newCipher() = KeystoreCipher(context, keysetFile, masterKeyAlias)

    private fun newKeyStore() = DatabaseKeyStore(dataStore, newCipher())

    private fun mutation() = PendingMutationEntity(
        mutationId = UUID.randomUUID().toString(),
        entity = MutationEntity.MEASUREMENT,
        entityId = UUID.randomUUID().toString(),
        childId = UUID.randomUUID().toString(),
        op = MutationOp.UPSERT,
        payload = """{"weightKg":7.31}""",
        createdAt = Instant.parse("2026-10-14T12:00:00Z"),
    )

    /** No puede descifrar nada de lo guardado; cifrar funciona para poder crear una clave nueva. */
    private object UnreadableCipher : LocalCipher {
        override fun encrypt(plaintext: ByteArray, associatedData: ByteArray) = plaintext.copyOf()
        override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray =
            throw GeneralSecurityException("decryption failed")
        override fun reset() = Unit
    }

    private companion object {
        val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    }
}
