package com.pasitos.app.core.sync.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pasitos.app.core.database.PasitosDatabase
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncStateDaoTest {

    private lateinit var database: PasitosDatabase
    private val dao get() = database.syncStateDao()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            PasitosDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `un teléfono que nunca sincronizó no tiene estado`() = runTest {
        assertNull(dao.get())
        assertNull(dao.observe().first())
    }

    @Test
    fun `RNF-OFF-04 guarda el cursor y la hora de la última sincronización`() = runTest {
        val state = SyncStateEntity(cursor = 18_517, lastSyncedAt = Instant.parse("2026-10-14T22:15:00Z"))

        dao.save(state)

        assertEquals(state, dao.get())
    }

    @Test
    fun `cada sincronización reemplaza el estado anterior`() = runTest {
        dao.save(SyncStateEntity(cursor = 18_342, lastSyncedAt = Instant.parse("2026-10-14T22:00:00Z")))
        val latest = SyncStateEntity(cursor = 18_517, lastSyncedAt = Instant.parse("2026-10-14T22:15:00Z"))

        dao.save(latest)

        assertEquals(latest, dao.observe().first())
    }
}
