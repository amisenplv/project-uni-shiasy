package org.unishiasy.vehiclelog.ui

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
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
import org.unishiasy.vehiclelog.data.LedgerRepository
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
class P13AccessibilityAndDatePickerRobolectricTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val context: Context = RuntimeEnvironment.getApplication()
    // 2026-03-21 == 1405/01/01 in Asia/Tehran
    private val clock = Clock.fixed(Instant.parse("2026-03-21T10:00:00Z"), ZoneOffset.UTC)
    private val vehicle = VehicleEntity("p13-v-1", "car", "سمند LX")
    private lateinit var database: VehicleDatabase
    private lateinit var ledgerVm: LedgerViewModel
    private lateinit var reminderVm: RemindersViewModel

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    private fun mountHost(fontScale: Float = 1f) {
        database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(reminderConstraints)
            .build()
        val vehicleRepo = VehicleRepository(database.vehicleDao())
        val ledgerRepo = LedgerRepository(database)
        val reminderRepo = ReminderRepository(database, ledgerRepo, clock)
        runBlocking { database.vehicleDao().insert(vehicle) }

        val act = compose.activity
        act.runOnUiThread {
            val factory = viewModelFactory {
                initializer { HomeViewModel(vehicleRepo, ledgerRepo) }
                initializer { LedgerViewModel(ledgerRepo, clock) }
                initializer { VehicleViewModel(vehicleRepo) }
                initializer { RemindersViewModel(reminderRepo, ledgerRepo, clock) }
            }
            val provider = ViewModelProvider(act, factory)
            val homeVm = provider["p13-home", HomeViewModel::class.java]
            ledgerVm = provider["p13-ledger", LedgerViewModel::class.java]
            val vehicleVm = provider["p13-vehicles", VehicleViewModel::class.java]
            reminderVm = provider["p13-reminders", RemindersViewModel::class.java]

            act.setContent {
                val baseDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalDensity provides Density(baseDensity.density, fontScale),
                ) {
                    VehicleLogTheme {
                        LedgerHost(vehicleVm, ledgerVm, homeVm, reminderVm)
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun highFontScaleStacksHomeAndFuelFieldsAndEntryPickerSelectsLeapEsfand30() {
        mountHost(fontScale = 1.5f)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("homeQuickAdd").fetchSemanticsNodes().isNotEmpty() }

        val fuelBounds = compose.onNodeWithTag("homeQuickAdd").getUnclippedBoundsInRoot()
        val expenseBounds = compose.onNodeWithTag("homeAddExpense").performScrollTo().getUnclippedBoundsInRoot()
        assertTrue("Expected stacked action buttons at 1.5x fontScale", expenseBounds.top >= fuelBounds.bottom)

        compose.onNodeWithTag("homeQuickAdd").performScrollTo().performClick()
        compose.waitForIdle()

        val amountBounds = compose.onNodeWithTag("ledgerAmount").getUnclippedBoundsInRoot()
        val volumeBounds = compose.onNodeWithTag("ledgerVolume").performScrollTo().getUnclippedBoundsInRoot()
        assertTrue("Expected stacked fuel inputs at 1.5x fontScale", volumeBounds.top >= amountBounds.bottom)

        // Quick chip Yesterday selects 1404/12/29
        compose.onNodeWithTag("ledgerDateYesterday").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("۱۴۰۴/۱۲/۲۹", ledgerVm.state.value.draft?.date)

        // Open Jalali calendar picker, step back 1 year from 1404/12 -> 1403/12 (leap year), select day 30
        compose.onNodeWithTag("ledgerDatePickerOpen").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("jalaliDatePickerDialog").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("jalaliPickerPrevYear").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("jalaliPickerDay:30").assertIsDisplayed().performClick()
        compose.waitForIdle()

        assertEquals("۱۴۰۳/۱۲/۳۰", ledgerVm.state.value.draft?.date)
    }

    @Test
    fun reminderQuickPresetAndDatePickerUpdateDraft() {
        mountHost(fontScale = 1f)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("homeOpenReminders").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("homeOpenReminders").performScrollTo().performClick()
        compose.waitForIdle()

        // +3 months from 1405/01/01 -> 1405/04/01
        compose.onNodeWithTag("reminderQuick3m").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("۱۴۰۵/۰۴/۰۱", reminderVm.state.value.draft.date)

        // Open picker and tap Today -> 1405/01/01
        compose.onNodeWithTag("reminderDatePickerOpen").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("jalaliDatePickerDialog").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("jalaliPickerToday").performClick()
        compose.waitForIdle()
        assertEquals("۱۴۰۵/۰۱/۰۱", reminderVm.state.value.draft.date)
    }
}
