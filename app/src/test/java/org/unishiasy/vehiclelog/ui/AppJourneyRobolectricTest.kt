package org.unishiasy.vehiclelog.ui

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
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

        // Direct click on Home recent entry opens that entry directly for editing
        compose.onNodeWithTag("homeRecent:$entryId").performScrollTo().performClick()
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
}
