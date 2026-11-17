package org.unishiasy.vehiclelog.data.backup

import android.content.ComponentName
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.unishiasy.vehiclelog.MainActivity
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.VehicleRepository
import org.unishiasy.vehiclelog.data.local.EntryEntity
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.data.local.reminderConstraints
import org.unishiasy.vehiclelog.reminders.NotificationChannels
import org.unishiasy.vehiclelog.reminders.ReminderNotifications
import org.unishiasy.vehiclelog.ui.home.HomeViewModel
import org.unishiasy.vehiclelog.ui.ledger.LedgerHost
import org.unishiasy.vehiclelog.ui.ledger.LedgerViewModel
import org.unishiasy.vehiclelog.ui.settings.BackupSettingsViewModel
import org.unishiasy.vehiclelog.ui.theme.VehicleLogTheme
import org.unishiasy.vehiclelog.ui.vehicle.VehicleViewModel

@RunWith(AndroidJUnit4::class)
class BackupDocumentPickerTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun savesAndOpensRealDocumentInIsolatedLedgers() {
        val name = "m45-vehicle-ledger-${UUID.randomUUID()}"
        val fileName = "$name.json"
        val source = fixture("$name-source")
        val target = fixture("$name-target")
        val vehicle = VehicleEntity(UUID.randomUUID().toString(), "car", "خودروی آزمون")
        val entry = EntryEntity(UUID.randomUUID().toString(), vehicle.id, "repair", "2026-11-13", 1, null, Long.MAX_VALUE, null)
        var activity: MainActivity? = null
        try {
            runBlocking {
                source.database.vehicleDao().insert(vehicle)
                source.database.ledgerDao().insert(entry)
            }
            val component = ComponentName(context, MainActivity::class.java)
            assertTrue(shell("am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n ${component.flattenToString()}").contains("Status: ok"))
            val current = resumedActivity()
            activity = current
            val selected = mutableStateOf(source)
            instrumentation.runOnMainSync {
                current.setContent {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        VehicleLogTheme {
                            val fixture = selected.value
                            key(fixture) { LedgerHost(fixture.vehicles, fixture.ledger, fixture.home, backupSettings = fixture.settings) }
                        }
                    }
                }
            }

            settings()
            click("backupExport")
            val input = awaitSystemNode { it.isEditable && it.text?.toString()?.contains("vehicle-ledger-backup") == true }
            assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, fileName)
            }))
            val save = awaitSystemNode { it.viewIdResourceName == "android:id/button1" && it.isEnabled }
            assertTrue(save.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            awaitMessage(R.string.backup_exported)
            android.util.Log.i("M45Test", "Document picker export succeeded")
            runBlocking { source.database.ledgerDao().insert(entry.copy(id = UUID.randomUUID().toString())) }

            instrumentation.runOnMainSync { selected.value = target }
            awaitMessage(R.string.vehicle_empty)
            settings()
            click("backupRestore")
            openDocument(fileName)
            awaitMessage(R.string.backup_restored)
            runBlocking {
                assertEquals(listOf(vehicle), target.database.backupDao().vehicles(10))
                assertEquals(listOf(entry), target.database.backupDao().entries(10))
            }
            android.util.Log.i("M45Test", "Document picker restore preserved export-time records")

            click("backupRestore")
            openDocument(fileName)
            awaitMessage(R.string.backup_populated)
            runBlocking { assertEquals(listOf(entry), target.database.backupDao().entries(10)) }

            compose.onNodeWithText(context.getString(R.string.ledger_back)).performScrollTo().performClick()
            awaitMessage(vehicle.displayLabel)

            settings()
            click("backupExport")
            awaitSystemNode { it.isEditable }
            assertTrue(instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK))
            awaitMessage(R.string.backup_canceled)
        } finally {
            instrumentation.runOnMainSync {
                activity?.finish()
                source.store.clear()
                target.store.clear()
            }
            source.close()
            target.close()
            // The picker probes use Downloads on the Redmi. Delete only this test's random filename.
            shell("rm -f /sdcard/Download/$fileName")
        }
    }

    private fun openDocument(name: String) {
        val node = awaitSystemNode { it.text?.toString() == name }
        var selectable: AccessibilityNodeInfo? = node
        while (selectable != null && !selectable.isClickable) selectable = selectable.parent
        assertTrue(checkNotNull(selectable).performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun awaitSystemNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + 8_000
        fun find(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (predicate(node)) return node
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { child -> find(child)?.let { return it } }
            }
            return null
        }
        val automation = instrumentation.uiAutomation
        while (SystemClock.uptimeMillis() < deadline) {
            val root = automation.rootInActiveWindow
            if (root != null && root.packageName?.contains("documentsui") == true) find(root)?.let { return it }
            SystemClock.sleep(100)
        }
        error("Expected control missing from Android DocumentsUI")
    }

    private fun settings() = click("homeOpenSettings")

    private fun click(tag: String) = compose.onNodeWithTag(tag).performScrollTo().performClick()

    private fun awaitMessage(id: Int) = awaitMessage(context.getString(id))

    private fun awaitMessage(text: String) {
        compose.waitUntil(8_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()
    }

    private fun resumedActivity(): MainActivity {
        var activity: MainActivity? = null
        instrumentation.runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>().singleOrNull()
        }
        return checkNotNull(activity)
    }

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .bufferedReader().use { it.readText() }

    private fun fixture(name: String) = Fixture(name)

    private inner class Fixture(private val name: String) {
        val store = ViewModelStore()
        val database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java)
            .addCallback(reminderConstraints).build()
        private val vehicleRepository = VehicleRepository(database.vehicleDao())
        private val ledgerRepository = LedgerRepository(database)
        val vehicles = VehicleViewModel(vehicleRepository)
        val ledger = LedgerViewModel(ledgerRepository)
        val home = HomeViewModel(vehicleRepository, ledgerRepository)
        private val directory = File(context.noBackupFilesDir, name)
        private val notifications = ReminderNotifications(context, database.reminderDao()::activeDateReminders,
            preferencesName = name, deliveryDirectory = directory, channels = NotificationChannels(context, "$name:"))
        val settings = BackupSettingsViewModel(BackupRepository(database, notifications, {}), context.contentResolver)
        init {
            store.put("vehicles", vehicles)
            store.put("ledger", ledger)
            store.put("home", home)
            store.put("settings", settings)
        }

        fun close() {
            database.close()
            directory.deleteRecursively()
            context.deleteSharedPreferences(name)
        }
    }
}
