package org.unishiasy.vehiclelog.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

internal class ReminderScheduling(private val context: Context) {
    fun update(enabled: Boolean) {
        val manager = WorkManager.getInstance(context)
        if (!enabled) {
            manager.cancelUniqueWork(PERIODIC_WORK)
            manager.cancelUniqueWork(RECONCILE_WORK)
            return
        }
        manager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build(),
        )
        manager.enqueueUniqueWork(
            RECONCILE_WORK,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>().build(),
        )
    }

    companion object {
        const val PERIODIC_WORK = "date-reminders-periodic"
        const val RECONCILE_WORK = "date-reminders-reconcile"
    }
}
