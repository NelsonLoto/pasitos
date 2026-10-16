package com.pasitos.app.core.sync.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pasitos.app.core.database.PasitosDatabase
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PendingMutationDaoTest {

    private lateinit var database: PasitosDatabase
    private val dao get() = database.pendingMutationDao()

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
    fun `devuelve los cambios en el orden en que se hicieron`() = runTest {
        // La hora del teléfono puede retroceder; manda el orden de llegada a la bandeja
        dao.insert(mutation("m1", createdAt = "2026-10-14T12:00:00Z"))
        dao.insert(mutation("m2", createdAt = "2026-10-14T11:00:00Z"))
        dao.insert(mutation("m3", createdAt = "2026-10-14T12:00:00Z"))

        assertEquals(listOf("m1", "m2", "m3"), dao.oldest(limit = 10).map { it.mutationId })
        assertEquals(listOf("m1", "m2"), dao.oldest(limit = 2).map { it.mutationId })
    }

    @Test
    fun `guarda el cambio completo`() = runTest {
        val mutation = mutation("m1", op = MutationOp.DELETE)

        dao.insert(mutation)

        assertEquals(mutation.copy(sequence = 1), dao.oldest(limit = 1).single())
    }

    @Test
    fun `saca de la bandeja solo los cambios que el servidor respondió`() = runTest {
        dao.insert(mutation("m1"))
        dao.insert(mutation("m2"))
        dao.insert(mutation("m3"))

        dao.delete(listOf("m1", "m3"))

        assertEquals(listOf("m2"), dao.oldest(limit = 10).map { it.mutationId })
    }

    @Test
    fun `RNF-OFF-05 descarta los cambios de un niño al que se perdió el acceso`() = runTest {
        dao.insert(mutation("m1", childId = SOFIA))
        dao.insert(mutation("m2", childId = TOMAS))
        dao.insert(mutation("m3", childId = SOFIA))

        dao.deleteForChild(SOFIA)

        assertEquals(listOf("m2"), dao.oldest(limit = 10).map { it.mutationId })
    }

    @Test
    fun `RF-CTA-07 CA2 cuenta los cambios pendientes para advertir al cerrar sesión`() = runTest {
        assertEquals(0, dao.observeCount().first())

        dao.insert(mutation("m1"))
        dao.insert(mutation("m2"))

        assertEquals(2, dao.count())
        assertEquals(2, dao.observeCount().first())
    }

    @Test
    fun `RNF-OFF-03 el mismo cambio no entra dos veces en la bandeja`() = runTest {
        dao.insert(mutation("m1"))

        val error = runCatching { dao.insert(mutation("m1")) }.exceptionOrNull()

        assertTrue("Se esperaba una violación de unicidad, llegó $error", error is SQLiteConstraintException)
        assertEquals(1, dao.count())
    }

    private fun mutation(
        mutationId: String,
        childId: String = SOFIA,
        op: MutationOp = MutationOp.UPSERT,
        createdAt: String = "2026-10-14T12:00:00Z",
    ) = PendingMutationEntity(
        mutationId = mutationId,
        entity = MutationEntity.MEASUREMENT,
        entityId = "measurement-$mutationId",
        childId = childId,
        op = op,
        payload = """{"id":"measurement-$mutationId","weightKg":7.31}""",
        createdAt = Instant.parse(createdAt),
    )

    private companion object {
        const val SOFIA = "8b0e7a52-1c2d-4e3f-9a8b-7c6d5e4f3a21"
        const val TOMAS = "3f2e1d0c-9b8a-4765-8432-10fedcba9876"
    }
}
