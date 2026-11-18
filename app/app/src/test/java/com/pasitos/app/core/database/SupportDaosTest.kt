package com.pasitos.app.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SupportDaosTest {
    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), PasitosDatabase::class.java,
    ).build()

    @After fun close() = database.close()

    @Test fun `I0-07 CA3 bandeja mantiene orden y solo borra cambios confirmados o del nino revocado`() = runBlocking {
        val dao = database.pendingMutations()
        val first = mutation("a", "child-a", 1)
        dao.insert(mutation("c", "child-b", 3))
        dao.insert(first)
        dao.insert(mutation("b", "child-a", 2))
        assertEquals(listOf("a", "b"), dao.oldest(2).map { it.mutationId })
        assertEquals(3, dao.observeCount().first())
        dao.deleteConfirmed(listOf("a"))
        assertTrue(dao.hasPending("MEASUREMENT", "row-b"))
        assertFalse(dao.hasPending("MEASUREMENT", "row-a"))
        dao.deleteChild("child-a")
        assertEquals(listOf("c"), dao.oldest().map { it.mutationId })
    }

    @Test fun `I0-07 CA3 una transaccion fallida no guarda mutacion ni cursor parcial`() = runBlocking {
        val dao = database.pendingMutations()
        val original = mutation("a", "child", 1)
        dao.insert(original)
        database.syncState().save(SyncState(cursor = 10))
        try {
            database.withTransaction {
                database.syncState().save(SyncState(cursor = 20))
                dao.insert(original.copy(payload = "no sobrescribir"))
            }
            fail("La mutación duplicada debe rechazar la transacción")
        } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        assertEquals(10L, database.syncState().read()!!.cursor)
        assertEquals(original, dao.oldest().single())
    }

    @Test fun `I0-07 CA3 cursor y ultima sincronizacion conservan momentos UTC`() = runBlocking {
        assertNull(database.syncState().read())
        val value = SyncState(cursor = 90, lastSyncedAt = Instant.parse("2026-11-18T01:00:00Z"))
        database.syncState().save(value)
        assertEquals(value, database.syncState().observe().first())
        database.syncState().save(value.copy(cursor = 100))
        assertEquals(100L, database.syncState().read()!!.cursor)
    }

    private fun mutation(id: String, child: String, seconds: Long) = PendingMutation(
        id, "MEASUREMENT", "row-$id", child, "UPSERT", "{}", Instant.ofEpochSecond(seconds),
    )
}
