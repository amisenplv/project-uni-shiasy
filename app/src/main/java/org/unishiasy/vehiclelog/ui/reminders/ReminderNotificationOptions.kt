package org.unishiasy.vehiclelog.ui.reminders

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.reminders.ReminderNotifications
import org.unishiasy.vehiclelog.reminders.NotificationChannels

@Composable
internal fun ReminderNotificationOptions(notifications: ReminderNotifications, reconcile: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(notifications.optedIn()) }
    var allowed by remember { mutableStateOf(notifications.systemAllowsNotifications()) }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = notifications.systemAllowsNotifications()
        reconcile()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        enabled = notifications.optedIn()
        allowed = notifications.systemAllowsNotifications()
    }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.reminder_notification_channel), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.reminder_notification_explanation), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(when {
                !enabled -> R.string.reminder_notification_off
                !allowed -> R.string.reminder_notification_blocked
                else -> R.string.reminder_notification_on
            }))
            if (failed) Text(stringResource(R.string.reminder_write_failed), color = MaterialTheme.colorScheme.error)
            OutlinedButton(
                onClick = {
                    saving = true
                    failed = false
                    val requested = !enabled
                    scope.launch {
                        val saved = notifications.setOptedIn(requested)
                        enabled = notifications.optedIn()
                        allowed = notifications.systemAllowsNotifications()
                        saving = false
                        failed = !saved
                        if (saved && requested && Build.VERSION.SDK_INT >= 33 &&
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                },
                modifier = Modifier.heightIn(min = 48.dp),
                enabled = !saving,
            ) {
                Text(stringResource(if (enabled) R.string.reminder_notification_disable else R.string.reminder_notification_enable))
            }
            if (enabled && !allowed) {
                TextButton(
                    onClick = {
                        val channel = context.getSystemService(NotificationManager::class.java)
                            .getNotificationChannel(NotificationChannels.DATE_CHANNEL)
                        val action = if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
                            Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                        } else {
                            Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        }
                        context.startActivity(
                            Intent(action).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                .putExtra(Settings.EXTRA_CHANNEL_ID, NotificationChannels.DATE_CHANNEL),
                        )
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.reminder_notification_settings)) }
            }
        }
    }
}
