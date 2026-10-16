package com.pasitos.app.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
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
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Base local con SQLCipher, Tink y el Android Keystore reales (ADR-17). Corre en emulador o
 * dispositivo: el Keystore no existe en la JVM. Usa un archivo de base y de DataStore propios para
 * no tocar los de la app.
 */
@RunWith(AndroidJUnit4::class)
class LocalDatabaseTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testId = UUID.randomUUID().toString()
    private val databaseName = "test-$testId.db"
    private val dataStoreFile = File(context.filesDir, "datastore/test-$testId.preferences_pb")

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var keyStore: DatabaseKeyStore
    private lateinit var localDatabase: LocalDatabase

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create { dataStoreFile }
        keyStore = DatabaseKeyStore(dataStore, KeystoreCipher(context))
        localDatabase = LocalDatabase(context, keyStore, databaseName)
    }

    @After
    fun tearDown() {
        runBlocking { localDatabase.wipe() }
        dataStoreFile.delete()
    }

    @Test
    fun databaseFileCannotBeOpenedWithoutTheKey() = runTest {
        localDatabase.get().pendingMutationDao().insert(mutation())
        localDatabase.get().close()

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
    fun databaseReopensWithTheStoredKey() = runTest {
        localDatabase.get().pendingMutationDao().insert(mutation())
        localDatabase.get().close()

        val reopened = LocalDatabase(context, keyStore, databaseName)

        assertEquals(1, reopened.get().pendingMutationDao().count())
        reopened.get().close()
    }

    @Test
    fun wipeDeletesTheFileAndTheKey() = runTest {
        localDatabase.get().pendingMutationDao().insert(mutation())
        assertNotNull(keyStore.read())

        localDatabase.wipe()

        assertFalse(context.getDatabasePath(databaseName).exists())
        assertNull(keyStore.read())
        // La próxima sesión empieza con una base vacía y una clave nueva
        assertEquals(0, localDatabase.get().pendingMutationDao().count())
        assertNotNull(keyStore.read())
    }

    @Test
    fun unreadableKeyStartsOverWithAnEmptyDatabase() = runTest {
        localDatabase.get().pendingMutationDao().insert(mutation())
        localDatabase.get().close()

        // Como si el sistema hubiera borrado la clave del Keystore: la guardada ya no se descifra
        val reopened = LocalDatabase(context, DatabaseKeyStore(dataStore, UnreadableCipher), databaseName)

        assertEquals(0, reopened.get().pendingMutationDao().count())
        reopened.get().close()
    }

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
    }

    private companion object {
        val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    }
}
