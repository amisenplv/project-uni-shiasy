package org.unishiasy.vehiclelog.ui

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.unishiasy.vehiclelog.MainActivity
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.EntryInput
import org.unishiasy.vehiclelog.data.EntrySaveResult
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.OdometerInput
import org.unishiasy.vehiclelog.data.ReminderInput
import org.unishiasy.vehiclelog.data.ReminderRepository
import org.unishiasy.vehiclelog.data.VehicleRepository
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.data.local.reminderConstraints
import org.unishiasy.vehiclelog.ui.home.HomeViewModel
import org.unishiasy.vehiclelog.ui.ledger.LedgerHost
import org.unishiasy.vehiclelog.ui.ledger.LedgerViewModel
import org.unishiasy.vehiclelog.ui.reminders.RemindersViewModel
import org.unishiasy.vehiclelog.ui.theme.VehicleLogTheme
import org.unishiasy.vehiclelog.ui.vehicle.VehicleViewModel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppJourneyRobolectricTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val clock = Clock.fixed(Instant.parse("2026-03-21T10:00:00Z"), ZoneOffset.UTC)
    private val vehicle = VehicleEntity("test-v-1", "car", "سمند LX", productionYear = 1400, productionYearCalendar = "solar_hijri")
    private lateinit var database: VehicleDatabase
    private lateinit var ledgerRepo: LedgerRepository
    private lateinit var reminderRepo: ReminderRepository
    private lateinit var ledgerVm: LedgerViewModel
    private lateinit var reminderVm: RemindersViewModel

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    private fun mountHost(
        seedVehicle: Boolean = true,
        beforeMount: suspend (LedgerRepository, ReminderRepository) -> Unit = { _, _ -> },
    ) {
        database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(reminderConstraints)
            .build()
        val vehicleRepo = VehicleRepository(database.vehicleDao())
        ledgerRepo = LedgerRepository(database)
        reminderRepo = ReminderRepository(database, ledgerRepo, clock)
        runBlocking {
            if (seedVehicle) {
                database.vehicleDao().insert(vehicle)
            }
            beforeMount(ledgerRepo, reminderRepo)
        }
        val act = compose.activity
        act.runOnUiThread {
            val factory = viewModelFactory {
                initializer { HomeViewModel(vehicleRepo, ledgerRepo) }
                initializer { LedgerViewModel(ledgerRepo, clock) }
                initializer { VehicleViewModel(vehicleRepo) }
                initializer { RemindersViewModel(reminderRepo, ledgerRepo, clock) }
            }
            val provider = ViewModelProvider(act, factory)
            val homeVm = provider["robo-home", HomeViewModel::class.java]
            ledgerVm = provider["robo-ledger", LedgerViewModel::class.java]
            val vehicleVm = provider["robo-vehicles", VehicleViewModel::class.java]
            reminderVm = provider["robo-reminders", RemindersViewModel::class.java]

            act.setContent {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    VehicleLogTheme {
                        LedgerHost(vehicleVm, ledgerVm, homeVm, reminderVm)
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun homeVehicleSwitcherOpensVehicleManagementAndReturnsToHome() {
        mountHost()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("homeScreen").fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("homeOpenVehicleSwitcher").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("manageVehiclesButton").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("manageVehiclesButton").performClick()
        compose.waitForIdle()

        compose.onNodeWithText(context.getString(R.string.vehicle_list_title)).assertIsDisplayed()
        compose.onNodeWithTag("vehicleAddButton").assertIsDisplayed()

        compose.onNodeWithTag("vehicleManageBack").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("homeScreen").assertIsDisplayed()
    }

    @Test
    fun remindersHideMissingMileageWarningWhenMileageExistsAndConfirmCancel() {
        val reminderId = UUID.randomUUID()
        mountHost { ledger, reminders ->
            ledger.createStandaloneReading(UUID.randomUUID(), OdometerInput(vehicle.id, "2026-03-20", 12_000L))
            reminders.create(vehicle.id, reminderId, ReminderInput("تعویض روغن", dueKilometers = 15_000L))
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("homeOpenReminders").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("homeOpenReminders").performScrollTo().performClick()
        compose.waitUntil(5_000) { reminderVm.state.value.list?.mileage == 12_000L }

        compose.onNodeWithTag("reminderMileageType").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText(context.getString(R.string.reminder_missing_mileage)).assertDoesNotExist()
        compose.onNodeWithTag("remindersScreen").performScrollToNode(hasText("آخرین کارکرد ثبت‌شده", substring = true))
        compose.onNodeWithText("آخرین کارکرد ثبت‌شده", substring = true).assertIsDisplayed()

        compose.onNodeWithTag("remindersScreen").performScrollToNode(hasTestTag("reminderCancel:$reminderId"))
        compose.onNodeWithTag("reminderCancel:$reminderId").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("reminderDismissCancel").assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("remindersScreen").performScrollToNode(hasTestTag("reminderCancel:$reminderId"))
        compose.onNodeWithTag("reminderCancel:$reminderId").assertIsDisplayed()

        compose.onNodeWithTag("reminderCancel:$reminderId").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("reminderConfirmCancel").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(context.getString(R.string.reminder_canceled)).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(reminderVm.state.value.list?.rows?.single()?.state == "canceled")
    }

    @Test
    fun entryFormCollapsesOptionalDetailsByDefaultAndExpandsOnMileageConflict() {
        val entryId = UUID.randomUUID()
        mountHost { ledger, _ ->
            ledger.createStandaloneReading(UUID.randomUUID(), OdometerInput(vehicle.id, "2026-03-22", 20_000L))
            ledger.create(entryId, 1_794_000_000_000L, EntryInput(vehicle.id, "repair", "2026-03-21", 500_000L))
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("homeRecent:$entryId").fetchSemanticsNodes().isNotEmpty() }

        // Direct click on Home recent entry opens read-only EntryDetailSheet, then Edit enters form
        compose.onNodeWithTag("homeRecent:$entryId").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            compose.onAllNodesWithTag("entryDetailEdit").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("entryDetailEdit").performClick()
        compose.waitUntil(5_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            ledgerVm.state.value.draft?.id == entryId
        }
        compose.waitForIdle()
        compose.onNodeWithTag("ledgerMileage").assertDoesNotExist()
        compose.onNodeWithTag("ledgerDetailsToggle").performScrollTo().performClick()
        compose.onNodeWithTag("ledgerMileage").performScrollTo().assertIsDisplayed()

        // Navigate back to new entry and confirm optional details start collapsed
        compose.activity.runOnUiThread {
            ledgerVm.back()
            ledgerVm.newEntry()
        }
        compose.waitForIdle()
        compose.onNodeWithTag("ledgerMileage").assertDoesNotExist()
        compose.onNodeWithTag("ledgerAmount").performTextReplacement("۱۲۰۰۰۰")
        compose.onNodeWithTag("ledgerVolume").performTextReplacement("۳۰")
        compose.onNodeWithTag("ledgerDetailsToggle").performScrollTo().performClick()
        compose.onNodeWithTag("ledgerMileage").performScrollTo().performTextReplacement("۲۵۰۰۰")
        compose.onNodeWithTag("ledgerSave").performScrollTo().performClick()

        compose.waitUntil(5_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            compose.onAllNodesWithText(context.getString(R.string.ledger_mileage_conflict_action))
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(EntrySaveResult.MileageConflict, ledgerVm.state.value.result)
        compose.onNodeWithTag("ledgerMileage").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun mandatoryOfflineJourneysRecoverRecordsTotalsAndRemindersAfterReopen() {
        val dbFile = File(context.cacheDir, "offline-journey-${UUID.randomUUID()}.db")
        val oilId = UUID.randomUUID()
        val dueReminderId = UUID.randomUUID()

        fun openDb(): VehicleDatabase = Room.databaseBuilder(context, VehicleDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .addCallback(reminderConstraints)
            .build()

        fun mount(keyPrefix: String, db: VehicleDatabase) {
            val vRepo = VehicleRepository(db.vehicleDao())
            ledgerRepo = LedgerRepository(db)
            reminderRepo = ReminderRepository(db, ledgerRepo, clock)
            val act = compose.activity
            act.runOnUiThread {
                val factory = viewModelFactory {
                    initializer { HomeViewModel(vRepo, ledgerRepo, clock = clock) }
                    initializer { LedgerViewModel(ledgerRepo, clock) }
                    initializer { VehicleViewModel(vRepo) }
                    initializer { RemindersViewModel(reminderRepo, ledgerRepo, clock) }
                }
                val provider = ViewModelProvider(act, factory)
                val homeVm = provider["$keyPrefix-home", HomeViewModel::class.java]
                ledgerVm = provider["$keyPrefix-ledger", LedgerViewModel::class.java]
                val vehicleVm = provider["$keyPrefix-vehicles", VehicleViewModel::class.java]
                reminderVm = provider["$keyPrefix-reminders", RemindersViewModel::class.java]
                act.setContent {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        VehicleLogTheme {
                            LedgerHost(vehicleVm, ledgerVm, homeVm, reminderVm)
                        }
                    }
                }
            }
            compose.waitForIdle()
        }

        try {
            database = openDb()
            runBlocking {
                database.vehicleDao().insert(vehicle)
                val seedLedger = LedgerRepository(database)
                val seedReminders = ReminderRepository(database, seedLedger, clock)
                seedLedger.create(
                    oilId,
                    1_774_087_000_000L,
                    EntryInput(vehicle.id, "oil_change", "2026-03-21", 500_000L),
                    kilometers = 42_000L,
                )
                seedReminders.create(
                    vehicle.id,
                    dueReminderId,
                    ReminderInput("بیمه شخص ثالث", dueOn = "2026-03-21"),
                )
            }
            mount("run1", database)

            // Journey A: Record fuel from Home shortcut
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                compose.onAllNodesWithTag("homeQuickAdd").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("homeQuickAdd").performScrollTo().performClick()
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                compose.onAllNodesWithTag("ledgerAmount").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("ledgerAmount").performScrollTo().performTextReplacement("۲۰۰٬۰۰۰")
            compose.onNodeWithTag("ledgerVolume").performScrollTo().performTextReplacement("۳۵")
            compose.onNodeWithTag("ledgerDate").performScrollTo().performTextReplacement("۱۴۰۵/۰۱/۰۱")
            compose.onNodeWithTag("ledgerSave").performScrollTo().performClick()
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                compose.onAllNodesWithText(context.getString(R.string.ledger_saved)).fetchSemanticsNodes().isNotEmpty()
            }
            compose.activity.runOnUiThread { ledgerVm.back() }
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                compose.onAllNodesWithTag("homeAddExpense").fetchSemanticsNodes().isNotEmpty()
            }

            // Journey A: Record expense from Home shortcut
            compose.onNodeWithTag("homeAddExpense").performScrollTo().performClick()
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                compose.onAllNodesWithTag("ledgerAmount").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("ledgerAmount").performScrollTo().performTextReplacement("۱۰۰٬۰۰۰")
            compose.onNodeWithTag("ledgerDate").performScrollTo().performTextReplacement("۱۴۰۵/۰۱/۰۱")
            compose.onNodeWithTag("ledgerSave").performScrollTo().performClick()
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                compose.onAllNodesWithText(context.getString(R.string.ledger_saved)).fetchSemanticsNodes().isNotEmpty()
            }

            // Verify oil change date & odometer + both new entries in History (Journey A & B)
            compose.onNodeWithTag("ledgerHistoryList").performScrollToNode(hasTestTag("ledgerRecent:$oilId"))
            compose.onNodeWithTag("ledgerRecent:$oilId").assertIsDisplayed().assertTextContains("۴۲۰۰۰", substring = true)

            // Verify Monthly Dashboard total = 50,000 + 200,000 + 100,000 = 350,000 Toman
            compose.activity.runOnUiThread { ledgerVm.openDashboard() }
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                compose.onAllNodesWithTag("dashboardTotal").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("dashboardTotal").assertTextContains("۳۵۰٬۰۰۰ تومان")
            compose.activity.runOnUiThread {
                ledgerVm.back()
                ledgerVm.back()
            }

            // Journey B: See separate due reminder on Home, open Reminders, create future reminder, mark due reminder done
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                compose.onAllNodesWithTag("homeDue:$dueReminderId").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("homeOpenReminders").performScrollTo().performClick()
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                compose.onAllNodesWithTag("reminderTitle").fetchSemanticsNodes().isNotEmpty()
            }

            compose.onNodeWithTag("reminderTitle").performTextReplacement("سرویس دوره‌ای بعدی")
            compose.onNodeWithTag("reminderDate").performTextReplacement("۱۴۰۵/۰۲/۰۱")
            compose.onNodeWithTag("reminderSave").performScrollTo().performClick()
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                compose.onAllNodesWithText(context.getString(R.string.reminder_created)).fetchSemanticsNodes().isNotEmpty()
            }

            compose.onNodeWithTag("remindersScreen").performScrollToNode(hasTestTag("reminderComplete:$dueReminderId"))
            compose.onNodeWithTag("reminderComplete:$dueReminderId").performClick()
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                compose.onAllNodesWithTag("reminderRecurringBanner").fetchSemanticsNodes().isNotEmpty() &&
                    reminderVm.state.value.list?.rows?.singleOrNull { it.id == dueReminderId.toString() }?.state == "completed"
            }

            // Simulate offline app close & reopen with fresh DB instance and fresh ViewModels
            database.close()
            database = openDb()
            mount("run2", database)

            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                compose.onAllNodesWithTag("homeOpenHistory").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("homeDue:$dueReminderId").assertDoesNotExist()

            // Confirm recovered records & monthly total
            compose.onNodeWithTag("homeOpenHistory").performScrollTo().performClick()
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                compose.onAllNodesWithTag("ledgerHistoryList").fetchSemanticsNodes().isNotEmpty() &&
                    ledgerVm.state.value.entries?.size == 3
            }
            compose.onNodeWithTag("ledgerHistoryList").performScrollToNode(hasTestTag("ledgerRecent:$oilId"))
            compose.onNodeWithTag("ledgerRecent:$oilId").assertIsDisplayed().assertTextContains("۴۲۰۰۰", substring = true)

            compose.activity.runOnUiThread { ledgerVm.openDashboard() }
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                compose.onAllNodesWithTag("dashboardTotal").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("dashboardTotal").assertTextContains("۳۵۰٬۰۰۰ تومان")
            compose.activity.runOnUiThread {
                ledgerVm.back()
                ledgerVm.back()
            }

            // Confirm recovered reminders (future active + due marked completed)
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                compose.onAllNodesWithTag("homeOpenReminders").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("homeOpenReminders").performScrollTo().performClick()
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                reminderVm.state.value.list?.rows?.size == 2
            }
            val rows = reminderVm.state.value.list?.rows.orEmpty()
            assertEquals("completed", rows.single { it.id == dueReminderId.toString() }.state)
            assertEquals("active", rows.single { it.title == "سرویس دوره‌ای بعدی" }.state)
        } finally {
            if (::database.isInitialized) database.close()
            dbFile.delete()
            File(dbFile.absolutePath + "-wal").delete()
            File(dbFile.absolutePath + "-shm").delete()
        }
    }
}
