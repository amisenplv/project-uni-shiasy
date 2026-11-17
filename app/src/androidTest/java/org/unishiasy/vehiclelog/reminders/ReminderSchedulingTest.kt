package org.unishiasy.vehiclelog.reminders

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.unishiasy.vehiclelog.VehicleLedgerApplication

@RunWith(AndroidJUnit4::class)
class ReminderSchedulingTest {
    @Test
    fun repeatedReconciliationKeepsOnePeriodicJobAndOptOutCancelsIt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as VehicleLedgerApplication
        assumeFalse("Leave existing opt-in and jobs alone", app.container.reminderNotifications.optedIn())
        val scheduler = ReminderScheduling(context)
        val manager = WorkManager.getInstance(context)
        fun jobs(name: String) = manager.getWorkInfosForUniqueWork(name).get(10, TimeUnit.SECONDS)
        try {
            scheduler.update(true)
            scheduler.update(true)
            await { jobs(ReminderScheduling.PERIODIC_WORK).count { !it.state.isFinished } == 1 }
            val periodic = jobs(ReminderScheduling.PERIODIC_WORK).single { !it.state.isFinished }
            assertEquals(WorkInfo.State.ENQUEUED, periodic.state)
            await { jobs(ReminderScheduling.RECONCILE_WORK).count { it.state == WorkInfo.State.SUCCEEDED } >= 2 }
            scheduler.update(false)
            await { jobs(ReminderScheduling.PERIODIC_WORK).none { !it.state.isFinished } }
        } finally {
            scheduler.update(false)
        }
    }

    private fun await(condition: () -> Boolean) {
        repeat(100) {
            if (condition()) return
            Thread.sleep(100)
        }
        assertTrue("WorkManager state did not settle", condition())
    }
}
