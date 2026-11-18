package org.unishiasy.vehiclelog.ui.settings

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowLooper
import org.unishiasy.vehiclelog.BuildConfig
import org.unishiasy.vehiclelog.MainActivity
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.EntryInput
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.ReminderRepository
import org.unishiasy.vehiclelog.data.VehicleRepository
import org.unishiasy.vehiclelog.data.backup.BackupOutcome
import org.unishiasy.vehiclelog.data.backup.BackupRepository
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.data.local.reminderConstraints
import org.unishiasy.vehiclelog.domain.withPersianDigits
import org.unishiasy.vehiclelog.reminders.ReminderNotifications
import org.unishiasy.vehiclelog.ui.home.HomeViewModel
import org.unishiasy.vehiclelog.ui.ledger.LedgerHost
import org.unishiasy.vehiclelog.ui.ledger.LedgerViewModel
import org.unishiasy.vehiclelog.ui.reminders.RemindersViewModel
import org.unishiasy.vehiclelog.ui.theme.VehicleLogTheme
import org.unishiasy.vehiclelog.ui.vehicle.VehicleViewModel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupPreferencesRobolectricTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val context: Context = RuntimeEnvironment.getApplication()
    private lateinit var database: VehicleDatabase

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
        context.deleteSharedPreferences("vehicle_log_backup_meta")
    }

    @Test
    fun backupPreferencesTracksLastExportAnd14DayStaleness() {
        context.deleteSharedPreferences("vehicle_log_backup_meta")
        val prefs = BackupPreferences(context)
        assertNull(prefs.lastExportEpochMillis.value)
        assertTrue(prefs.isBackupStale(nowMillis = 1_774_087_200_000L))

        val exportedAt = 1_774_087_200_000L // 2026-03-21
        prefs.recordExport(exportedAt)
        assertEquals(exportedAt, prefs.lastExportEpochMillis.value)
        assertFalse(prefs.isBackupStale(nowMillis = exportedAt + 13L * 86_400_000L))
        assertTrue(prefs.isBackupStale(nowMillis = exportedAt + 14L * 86_400_000L))
    }

    @Test
    fun exportUpdatesLastBackupTimestampAndHomeBannerDismissesCleanly() {
        context.deleteSharedPreferences("vehicle_log_backup_meta")
        val backupFile = File(context.cacheDir, "export-test-${UUID.randomUUID()}.json")
        val authority = "org.unishiasy.vehiclelog.testbackup"
        val provider = object : ContentProvider() {
            override fun onCreate() = true
            override fun query(u: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
            override fun getType(u: Uri) = "application/json"
            override fun insert(u: Uri, v: ContentValues?): Uri? = null
            override fun delete(u: Uri, s: String?, a: Array<out String>?) = 0
            override fun update(u: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
            override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor =
                ParcelFileDescriptor.open(
                    backupFile,
                    ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_READ_WRITE,
                )
        }
        provider.attachInfo(context, ProviderInfo().apply { this.authority = authority })
        ShadowContentResolver.registerProviderInternal(authority, provider)

        database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(reminderConstraints)
            .build()
        val vehicle = VehicleEntity(UUID.randomUUID().toString(), "car", "سمند سورن")
        val vehicleRepo = VehicleRepository(database.vehicleDao())
        val ledgerRepo = LedgerRepository(database)
        val reminderRepo = ReminderRepository(database, ledgerRepo)
        val notifications = ReminderNotifications(context, { emptyList() })
        val backupRepo = BackupRepository(database, notifications, {})
        val backupPrefs = BackupPreferences(context)
        val exportTimestamp = Instant.parse("2026-03-21T10:00:00Z").toEpochMilli()

        runBlocking {
            database.vehicleDao().insert(vehicle)
            ledgerRepo.create(UUID.randomUUID(), 1_000L, EntryInput(vehicle.id, "other", "2026-03-21", 500_000L))
        }

        lateinit var settingsVm: BackupSettingsViewModel
        val act = compose.activity
        act.runOnUiThread {
            val factory = viewModelFactory {
                initializer { HomeViewModel(vehicleRepo, ledgerRepo) }
                initializer { LedgerViewModel(ledgerRepo) }
                initializer { VehicleViewModel(vehicleRepo) }
                initializer { RemindersViewModel(reminderRepo, ledgerRepo) }
                initializer {
                    BackupSettingsViewModel(
                        repository = backupRepo,
                        resolver = context.contentResolver,
                        onExportRecorded = { backupPrefs.recordExport(exportTimestamp) },
                    )
                }
            }
            val provider = ViewModelProvider(act, factory)
            val homeVm = provider["bp-home", HomeViewModel::class.java]
            val ledgerVm = provider["bp-ledger", LedgerViewModel::class.java]
            val vehicleVm = provider["bp-vehicles", VehicleViewModel::class.java]
            val reminderVm = provider["bp-reminders", RemindersViewModel::class.java]
            settingsVm = provider["bp-settings", BackupSettingsViewModel::class.java]
            act.setContent {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    VehicleLogTheme {
                        LedgerHost(
                            vehicles = vehicleVm,
                            ledger = ledgerVm,
                            home = homeVm,
                            reminders = reminderVm,
                            backupSettings = settingsVm,
                            backupPreferences = backupPrefs,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()

        // Home stale backup banner visible because entries exist & never backed up
        compose.waitUntil(5_000) {
            ShadowLooper.idleMainLooper()
            compose.onAllNodesWithTag("homeStaleBackupBanner").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("homeStaleBackupBanner").assertIsDisplayed()

        // Open Settings -> shows "never backed up" and About/Version card
        compose.onNodeWithTag("homeStaleBackupAction").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("backupSettings").fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("backupLastExportDate")
            .performScrollTo()
            .assertTextContains(context.getString(R.string.backup_last_never))
        compose.onNodeWithTag("settingsAboutCard").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("settingsVersionText")
            .performScrollTo()
            .assertTextContains(BuildConfig.VERSION_NAME.withPersianDigits(), substring = true)

        // Trigger export via ViewModel -> records timestamp -> displays Jalali date
        act.runOnUiThread {
            settingsVm.startPicker(BackupAction.Export)
            settingsVm.selected(BackupAction.Export, Uri.parse("content://$authority/backup.json"))
        }
        compose.waitUntil(5_000) {
            ShadowLooper.idleMainLooper()
            settingsVm.state.value.outcome != null && !settingsVm.state.value.working
        }
        assertEquals(BackupOutcome.Exported, settingsVm.state.value.outcome)
        assertEquals(exportTimestamp, backupPrefs.lastExportEpochMillis.value)
        compose.onNodeWithTag("backupLastExportDate")
            .performScrollTo()
            .assertTextContains("۱ فروردین ۱۴۰۵", substring = true)

        // Back to Home -> banner still dismissed/hidden if exported recently (check via non-stale now)
        backupPrefs.recordExport(System.currentTimeMillis())
        act.runOnUiThread { settingsVm.back() }
        compose.waitForIdle()
        compose.onNodeWithTag("homeStaleBackupBanner").assertDoesNotExist()

        backupFile.delete()
    }
}
