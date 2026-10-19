package com.pasitos.app.core.sync

import com.pasitos.app.core.network.NoConnectionException
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Datos de referencia después de sincronizar (14 · Sincronización). */
@RunWith(RobolectricTestRunner::class)
class ReferenceDataRefresherTest {

    @Test
    fun `si uno falla, los demás se refrescan igual y se informa cuál falló`() = runTest {
        val refreshed = mutableListOf<String>()
        val failing = reference("VACCINATION_CALENDAR") { throw NoConnectionException(IOException("sin red")) }
        val working = reference("GROWTH_TABLES") { refreshed += "GROWTH_TABLES" }

        val failed = ReferenceDataRefresher(setOf(failing, working)).refreshAll()

        assertEquals(listOf("VACCINATION_CALENDAR"), failed)
        assertEquals(listOf("GROWTH_TABLES"), refreshed)
    }

    private fun reference(id: String, onRefresh: suspend () -> Unit) = object : ReferenceData {
        override val name = id

        override suspend fun refresh() = onRefresh()
    }
}
