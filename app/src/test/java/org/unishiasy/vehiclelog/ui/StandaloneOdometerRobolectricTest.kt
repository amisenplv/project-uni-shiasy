package org.unishiasy.vehiclelog.ui

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.unishiasy.vehiclelog.MainActivity
import org.unishiasy.vehiclelog.R
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
class StandaloneOdometerRobolectricTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val clock = Clock.fixed(Instant.parse("2026-03-22T10:00:00Z"), ZoneOffset.UTC)
    private val vehicle = VehicleEntity("odom-v-1", "car", "تیبا ۲")
    private lateinit var database: VehicleDatabase
    private lateinit var ledgerRepo: LedgerRepository
    private lateinit var homeVm: HomeViewModel

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    private fun mountHome(seed: suspend (LedgerRepository, ReminderRepository) -> Unit = { _, _ -> }) {
        database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(reminderConstraints)
            .build()
        val vehicleRepo = VehicleRepository(database.vehicleDao())
        ledgerRepo = LedgerRepository(database)
        val reminderRepo = ReminderRepository(database, ledgerRepo, clock)
        runBlocking {
            database.vehicleDao().insert(vehicle)
            seed(ledgerRepo, reminderRepo)
        }
        val act = compose.activity
        act.runOnUiThread {
            val factory = viewModelFactory {
                initializer { HomeViewModel(vehicleRepo, ledgerRepo, clock = clock) }
                initializer { LedgerViewModel(ledgerRepo, clock) }
                initializer { VehicleViewModel(vehicleRepo) }
                initializer { RemindersViewModel(reminderRepo, ledgerRepo, clock) }
            }
            val provider = ViewModelProvider(act, factory)
            homeVm = provider["odom-home", HomeViewModel::class.java]
            val ledgerVm = provider["odom-ledger", LedgerViewModel::class.java]
            val vehicleVm = provider["odom-vehicles", VehicleViewModel::class.java]
            val reminderVm = provider["odom-reminders", RemindersViewModel::class.java]
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
    fun standaloneOdometerSavesRejectsConflictTriggersReminderAndDeletes() {
        val reminderId = UUID.randomUUID()
        val olderReadingId = UUID.randomUUID()
        mountHome { ledger, reminders ->
            ledger.createStandaloneReading(olderReadingId, OdometerInput(vehicle.id, "2026-03-20", 14_000L))
            reminders.create(vehicle.id, reminderId, ReminderInput("تعویض فیلتر", dueKilometers = 15_000L))
        }
        compose.waitUntil(5_000) {
            ShadowLooper.idleMainLooper()
            homeVm.state.value.selectedVehicleMileage == 14_000L
        }

        // Open standalone odometer sheet and test chronological conflict
        compose.onNodeWithTag("homeUpdateMileage").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("odometerSheet").fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("odometerKilometersInput").performTextReplacement("۱۲۰۰۰")
        compose.onNodeWithTag("odometerSave").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            ShadowLooper.idleMainLooper()
            compose.onAllNodesWithText(context.getString(R.string.ledger_mileage_conflict)).fetchSemanticsNodes().isNotEmpty()
        }

        // Valid higher mileage crosses 15,000 km reminder threshold
        compose.onNodeWithTag("odometerKilometersInput").performTextReplacement("۱۵۵۰۰")
        compose.onNodeWithTag("odometerSave").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            ShadowLooper.idleMainLooper()
            homeVm.state.value.odometerDraft == null && homeVm.state.value.selectedVehicleMileage == 15_500L
        }
        compose.waitUntil(5_000) {
            ShadowLooper.idleMainLooper()
            compose.onAllNodesWithTag("homeDue:$reminderId").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("homeDue:$reminderId").performScrollTo().assertIsDisplayed()

        // Edit and delete newly created standalone reading
        val latestStandalone = runBlocking {
            ledgerRepo.observeStandaloneReadings(vehicle.id).first().first { it.kilometers == 15_500L }
        }
        compose.onNodeWithTag("homeUpdateMileage").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("odometerDelete:${latestStandalone.id}").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("odometerDelete:${latestStandalone.id}").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("odometerConfirmDelete").performClick()
        compose.waitUntil(5_000) {
            ShadowLooper.idleMainLooper()
            runBlocking { ledgerRepo.findReadingById(UUID.fromString(latestStandalone.id)) == null }
        }
        assertEquals(14_000L, runBlocking { ledgerRepo.observeCurrentMileage(vehicle.id).first() })
        assertNull(runBlocking { ledgerRepo.findReadingById(UUID.fromString(latestStandalone.id)) })
    }
}
