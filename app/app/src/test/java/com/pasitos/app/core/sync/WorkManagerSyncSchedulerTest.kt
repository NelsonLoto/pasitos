package com.pasitos.app.core.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkManagerSyncSchedulerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scheduler = WorkManagerSyncScheduler(context)
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        val configuration = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        workManager = WorkManager.getInstance(context)
    }

    @After
    fun tearDown() {
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    @Test
    fun `I0-07 la sincronización periódica corre cada 15 minutos y solo con red`() {
        scheduler.schedulePeriodic()

        val work = pending(WorkManagerSyncScheduler.PERIODIC_WORK).single()
        assertEquals(Duration.ofMinutes(15).toMillis(), work.periodicityInfo?.repeatIntervalMillis)
        assertEquals(NetworkType.CONNECTED, work.constraints.requiredNetworkType)
    }

    @Test
    fun `registrar la periódica en cada apertura de la app no la duplica`() {
        scheduler.schedulePeriodic()
        scheduler.schedulePeriodic()

        assertEquals(1, pending(WorkManagerSyncScheduler.PERIODIC_WORK).size)
    }

    @Test
    fun `una sincronización inmediata espera a que haya red`() {
        scheduler.requestSync(SyncTrigger.LOCAL_CHANGE)

        val work = pending(WorkManagerSyncScheduler.IMMEDIATE_WORK).single()
        assertEquals(WorkInfo.State.ENQUEUED, work.state)
        assertEquals(NetworkType.CONNECTED, work.constraints.requiredNetworkType)
    }

    @Test
    fun `un pedido nuevo reemplaza al que seguía esperando`() {
        scheduler.requestSync(SyncTrigger.LOCAL_CHANGE)
        scheduler.requestSync(SyncTrigger.MANUAL)

        assertEquals(1, pending(WorkManagerSyncScheduler.IMMEDIATE_WORK).size)
    }

    private fun pending(uniqueName: String): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWork(uniqueName).get().filterNot { it.state.isFinished }
}
