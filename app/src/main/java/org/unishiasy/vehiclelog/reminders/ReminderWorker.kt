package org.unishiasy.vehiclelog.reminders

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.IOException
import org.unishiasy.vehiclelog.VehicleLedgerApplication

class ReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = reconcileReminders(
        (applicationContext as VehicleLedgerApplication).container.reminderNotifications,
    )
}

internal suspend fun reconcileReminders(notifications: ReminderNotifications): androidx.work.ListenableWorker.Result =
    try {
        notifications.reconcile()
        androidx.work.ListenableWorker.Result.success()
    } catch (_: IOException) {
        androidx.work.ListenableWorker.Result.retry()
    } catch (_: SQLiteException) {
        androidx.work.ListenableWorker.Result.retry()
    }
