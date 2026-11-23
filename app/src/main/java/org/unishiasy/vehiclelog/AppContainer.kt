package org.unishiasy.vehiclelog

import android.content.Context
import android.util.Log
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.ReminderRepository
import org.unishiasy.vehiclelog.data.VehicleRepository
import org.unishiasy.vehiclelog.data.backup.BackupRepository
import org.unishiasy.vehiclelog.data.catalog.VehicleCatalog
import org.unishiasy.vehiclelog.data.cloud.CloudRepository
import org.unishiasy.vehiclelog.data.cloud.KeystoreCloudStore
import org.unishiasy.vehiclelog.data.cloud.LocalCloudApi
import org.unishiasy.vehiclelog.data.catalog.VehicleCatalogLoader
import org.unishiasy.vehiclelog.data.local.MIGRATION_1_2
import org.unishiasy.vehiclelog.data.local.MIGRATION_2_3
import org.unishiasy.vehiclelog.data.local.MIGRATION_3_4
import org.unishiasy.vehiclelog.data.local.MIGRATION_4_5
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.data.local.reminderConstraints
import org.unishiasy.vehiclelog.reminders.ReminderNotifications
import org.unishiasy.vehiclelog.reminders.ReminderScheduling

// Set an exact bundled asset path here only after its redistribution is approved.
private val approvedCatalogAsset: String? = null

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    private val database by lazy {
        Room.databaseBuilder(applicationContext, VehicleDatabase::class.java, "vehicle-ledger.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .addCallback(reminderConstraints)
            .build()
    }

    val appearancePreferences by lazy { org.unishiasy.vehiclelog.ui.settings.AppearancePreferences(applicationContext) }
    val backupPreferences by lazy { org.unishiasy.vehiclelog.ui.settings.BackupPreferences(applicationContext) }
    val selectedVehiclePreferences by lazy { org.unishiasy.vehiclelog.ui.home.SelectedVehiclePreferences(applicationContext) }
    val vehicleRepository by lazy { VehicleRepository(database.vehicleDao()) }
    val ledgerRepository by lazy { LedgerRepository(database) }
    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(
            database,
            ledgerRepository,
            onDateCreated = { if (reminderNotifications.optedIn()) reconcileReminderSchedule() },
            finishWithCleanup = { id, finish -> reminderNotifications.finishAndCancel(id, finish) },
            updateWithCleanup = { id, update -> reminderNotifications.updateAndResetDelivery(id, update) },
        )
    }
    internal val reminderNotifications: ReminderNotifications by lazy {
        ReminderNotifications(
            applicationContext,
            reminderRepository::activeDateReminders,
            onPreferenceChanged = ::reconcileReminderSchedule,
        )
    }
    internal val backupRepository by lazy {
        BackupRepository(database, reminderNotifications, ::reconcileReminderSchedule)
    }
    internal val cloudRepository by lazy {
        if (BuildConfig.LOCAL_CLOUD_ENABLED) {
            CloudRepository(
                LocalCloudApi(), KeystoreCloudStore(applicationContext),
                backupRepository::captureSnapshot, backupRepository::bindIfEmpty,
                backup = backupRepository,
            )
        } else null
    }
    private val reminderScheduling by lazy { ReminderScheduling(applicationContext) }

    internal fun reconcileReminderSchedule() {
        try {
            reminderScheduling.update(reminderNotifications.optedIn())
        } catch (_: IllegalStateException) {
            Log.w("ReminderScheduling", "WorkManager unavailable for reminder reconciliation")
        }
    }

    internal suspend fun loadVehicleCatalog(): VehicleCatalog? = withContext(Dispatchers.IO) {
        VehicleCatalogLoader.loadIfAvailable(approvedCatalogAsset) { path ->
            applicationContext.assets.open(path).bufferedReader(Charsets.UTF_8)
        }
    }
}
