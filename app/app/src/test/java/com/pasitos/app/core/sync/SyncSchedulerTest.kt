package com.pasitos.app.core.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.NetworkType
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.testing.SynchronousExecutor
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SyncSchedulerTest {
    @Test fun `I0-07 CA2 periodico espera red y vuelve a ejecutarse al cumplirse el intervalo`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val executions = AtomicInteger()
        val configuration = Configuration.Builder().setExecutor(SynchronousExecutor())
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
                    if (workerClassName != SyncWorker::class.java.name) return null
                    executions.incrementAndGet()
                    return SyncWorker(appContext, workerParameters)
                }
            }).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        val manager = WorkManager.getInstance(context)
        val scheduler = SyncScheduler(context)
        try {
            scheduler.schedulePeriodic().result.get()
            val work = manager.getWorkInfosForUniqueWork(SyncScheduler.PERIODIC).get().single()
            assertEquals(0, executions.get())
            val driver = WorkManagerTestInitHelper.getTestDriver(context)!!
            driver.setAllConstraintsMet(work.id)
            awaitPeriodic(manager, 1, executions)
            driver.setPeriodDelayMet(work.id)
            awaitPeriodic(manager, 2, executions)
        } finally {
            scheduler.cancelSync()
            WorkManagerTestInitHelper.closeWorkDatabase()
        }
    }

    private fun awaitPeriodic(manager: WorkManager, count: Int, executions: AtomicInteger) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            val state = manager.getWorkInfosForUniqueWork(SyncScheduler.PERIODIC).get().single().state
            if (executions.get() >= count && state == WorkInfo.State.ENQUEUED) return
            Thread.sleep(20)
        }
        fail("No se ejecutó el período $count")
    }

    @Test @Config(sdk = [30])
    fun `I0-07 inmediato en Android 11 no necesita servicio en primer plano`() {
        assertFalse(SyncScheduler.immediate(SyncReason.MANUAL).workSpec.expedited)
    }

    @Test fun `I0-07 CA2 inmediato y periodico exigen red y se distinguen por nombre`() {
        val immediate = SyncScheduler.immediate(SyncReason.LOCAL_CHANGE)
        assertEquals(NetworkType.CONNECTED, immediate.workSpec.constraints.requiredNetworkType)
        assertTrue(immediate.workSpec.expedited)
        val periodic = SyncScheduler.periodic()
        assertEquals(NetworkType.CONNECTED, periodic.workSpec.constraints.requiredNetworkType)
        assertEquals(TimeUnit.MINUTES.toMillis(15), periodic.workSpec.intervalDuration)
        assertNotEquals(SyncScheduler.NOW, SyncScheduler.PERIODIC)
    }

    @Test fun `I0-07 worker ejecuta solo el esqueleto sin afirmar que envio datos`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        val result = worker.doWork()
        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        assertTrue((result as androidx.work.ListenableWorker.Result.Success).outputData.getBoolean("skeletonExecuted", false))
    }
}
