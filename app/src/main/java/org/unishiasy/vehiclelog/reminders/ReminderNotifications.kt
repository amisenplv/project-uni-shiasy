package org.unishiasy.vehiclelog.reminders

import android.content.Context
import androidx.core.content.edit
import java.io.File
import java.io.IOException
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.unishiasy.vehiclelog.data.isDue
import org.unishiasy.vehiclelog.data.local.ReminderEntity
import org.unishiasy.vehiclelog.domain.JalaliDates

internal enum class RestoreNotificationResult { LedgerPopulated, Reset, CleanupFailed }

internal class ReminderNotifications(
    context: Context,
    private val activeDateReminders: suspend () -> List<ReminderEntity>,
    private val clock: Clock = Clock.systemUTC(),
    private val preferencesName: String = "date_reminder_notifications",
    private val onPreferenceChanged: () -> Unit = {},
    private val deliveryDirectory: File = File(context.noBackupFilesDir, "reminder-delivery"),
    private val channels: NotificationChannels = NotificationChannels(context),
) {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val delivery = ReminderDeliveryStore(deliveryDirectory)
    private val mutex = Mutex()

    fun optedIn(): Boolean = preferences.getBoolean("enabled", false)

    fun systemAllowsNotifications(): Boolean = channels.available()

    @Suppress("UseKtx") // commit() Boolean return value is required to roll back failed writes.
    suspend fun setOptedIn(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val previous = optedIn()
            if (!preferences.edit().putBoolean("enabled", enabled).commit()) {
                // SharedPreferences updates its in-memory value even when persistence fails.
                preferences.edit { putBoolean("enabled", previous) }
                return@withLock false
            }
            if (!enabled) channels.activeReminderIds().forEach(channels::cancel)
            onPreferenceChanged()
            true
        }
    }

    suspend fun finishAndCancel(id: UUID, finish: suspend () -> Int): Int = mutex.withLock {
        val changed = finish()
        if (changed == 1) channels.cancel(id.toString())
        changed
    }

    suspend fun updateAndResetDelivery(id: UUID, update: suspend () -> Int): Int = mutex.withLock {
        val changed = update()
        if (changed == 1) {
            channels.cancel(id.toString())
            withContext(Dispatchers.IO + NonCancellable) {
                runCatching { delivery.clear(id) }
            }
        }
        changed
    }

    // Serialize the commit and marker reset with notification posting.
    suspend fun restoreAndReset(
        reconcileSchedule: () -> Unit,
        restore: suspend () -> Boolean,
    ): RestoreNotificationResult = mutex.withLock {
        withContext(Dispatchers.IO + NonCancellable) {
            if (!restore()) return@withContext RestoreNotificationResult.LedgerPopulated
            try {
                channels.activeReminderIds().forEach(channels::cancel)
                delivery.reset()
                reconcileSchedule()
                RestoreNotificationResult.Reset
            } catch (_: IOException) {
                RestoreNotificationResult.CleanupFailed
            } catch (_: SecurityException) {
                RestoreNotificationResult.CleanupFailed
            } catch (_: IllegalStateException) {
                RestoreNotificationResult.CleanupFailed
            }
        }
    }

    suspend fun reconcile() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!optedIn() || !channels.available()) {
                channels.activeReminderIds().forEach(channels::cancel)
                return@withLock
            }
            val today = JalaliDates.captureDefaultDate(clock)
            val due = activeDateReminders().filter { it.dueOn != null && it.isDue(today, currentMileage = null) }
            val dueIds = due.mapTo(mutableSetOf()) { it.id }
            (channels.activeReminderIds() - dueIds).forEach(channels::cancel)
            for (reminder in due) {
                val id = UUID.fromString(reminder.id)
                if (!delivery.wasDelivered(id) && channels.post(reminder.id, reminder.title)) {
                    // A crash here can repeat a dismissed notification. An active one uses the same tag.
                    delivery.markDelivered(id)
                }
            }
        }
    }
}
