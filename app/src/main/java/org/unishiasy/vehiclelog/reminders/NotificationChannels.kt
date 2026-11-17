package org.unishiasy.vehiclelog.reminders

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import org.unishiasy.vehiclelog.MainActivity
import org.unishiasy.vehiclelog.R

internal class NotificationChannels(
    private val context: Context,
    private val tagPrefix: String = "date-reminder:",
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun available(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return manager.areNotificationsEnabled() &&
            manager.getNotificationChannel(DATE_CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun post(reminderId: String, title: String): Boolean {
        manager.createNotificationChannel(
            NotificationChannel(
                DATE_CHANNEL,
                context.getString(R.string.reminder_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.reminder_notification_channel_description) },
        )
        if (!available()) return false
        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return try {
            manager.notify(
                tag(reminderId),
                NOTIFICATION_ID,
                Notification.Builder(context, DATE_CHANNEL)
                    .setSmallIcon(R.drawable.ic_reminder_notification)
                    .setContentTitle(context.getString(R.string.reminder_notification_due))
                    .setContentText(title)
                    .setContentIntent(openApp)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(Notification.VISIBILITY_PRIVATE)
                    .build(),
            )
            available()
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancel(reminderId: String) = manager.cancel(tag(reminderId), NOTIFICATION_ID)

    fun activeReminderIds(): Set<String> = manager.activeNotifications
        .filter { it.id == NOTIFICATION_ID && it.tag?.startsWith(tagPrefix) == true }
        .mapTo(mutableSetOf()) { it.tag.removePrefix(tagPrefix) }

    private fun tag(reminderId: String) = tagPrefix + reminderId

    companion object {
        const val DATE_CHANNEL = "date_reminders"
        private const val NOTIFICATION_ID = 1
    }
}
