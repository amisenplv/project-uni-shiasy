package org.unishiasy.vehiclelog.ui.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BackupPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("vehicle_log_backup_meta", Context.MODE_PRIVATE)
    private val _lastExportEpochMillis = MutableStateFlow(readLastExport())
    val lastExportEpochMillis: StateFlow<Long?> = _lastExportEpochMillis.asStateFlow()

    fun recordExport(epochMillis: Long = System.currentTimeMillis()) {
        val safeMillis = epochMillis.coerceAtLeast(0L)
        prefs.edit { putLong(KEY_LAST_EXPORT_EPOCH_MILLIS, safeMillis) }
        _lastExportEpochMillis.value = safeMillis
    }

    fun isBackupStale(
        nowMillis: Long = System.currentTimeMillis(),
        staleDays: Long = DEFAULT_STALE_DAYS,
    ): Boolean {
        val last = _lastExportEpochMillis.value ?: return true
        val thresholdMillis = staleDays.coerceAtLeast(1L) * MILLIS_PER_DAY
        return nowMillis - last >= thresholdMillis
    }

    private fun readLastExport(): Long? =
        if (prefs.contains(KEY_LAST_EXPORT_EPOCH_MILLIS)) {
            prefs.getLong(KEY_LAST_EXPORT_EPOCH_MILLIS, -1L).takeIf { it >= 0L }
        } else {
            null
        }

    companion object {
        private const val KEY_LAST_EXPORT_EPOCH_MILLIS = "last_export_epoch_millis"
        private const val MILLIS_PER_DAY = 86_400_000L
        const val DEFAULT_STALE_DAYS = 14L
    }
}
