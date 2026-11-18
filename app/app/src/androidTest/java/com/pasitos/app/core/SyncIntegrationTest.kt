package com.pasitos.app.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.pasitos.app.NsApplication
import com.pasitos.app.core.sync.SyncReason
import com.pasitos.app.core.sync.SyncScheduler
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncIntegrationTest {
    @Test fun i0_07_CA2WorkManagerUsaHiltYEjecutaElPedidoManualConRed() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(app is NsApplication)
        assertNotNull((app as NsApplication).workerFactory)
        val scheduler = SyncScheduler(app)
        val manager = WorkManager.getInstance(app)
        try {
            scheduler.schedulePeriodic().result.get()
            val periodic = manager.getWorkInfosForUniqueWork(SyncScheduler.PERIODIC).get().single()
            assertEquals(WorkInfo.State.ENQUEUED, periodic.state)
            scheduler.requestSync(SyncReason.MANUAL).result.get()
            val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(45)
            var result: WorkInfo? = null
            while (System.nanoTime() < deadline) {
                result = manager.getWorkInfosForUniqueWork(SyncScheduler.NOW).get().lastOrNull()
                if (result?.state?.isFinished == true) break
                Thread.sleep(200)
            }
            assertEquals(WorkInfo.State.SUCCEEDED, result?.state)
            assertTrue(result!!.outputData.getBoolean("skeletonExecuted", false))
        } finally { scheduler.cancelSync() }
    }
}
