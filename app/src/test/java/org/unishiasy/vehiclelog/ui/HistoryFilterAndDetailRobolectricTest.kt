package org.unishiasy.vehiclelog.ui

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
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
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.unishiasy.vehiclelog.MainActivity
import org.unishiasy.vehiclelog.data.EntryInput
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
class HistoryFilterAndDetailRobolectricTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val clock = Clock.fixed(Instant.parse("2026-03-21T10:00:00Z"), ZoneOffset.UTC)
    private val vehicle = VehicleEntity("hist-v-1", "car", "پژو ۲۰۷")
    private lateinit var database: VehicleDatabase
    private lateinit var ledgerRepo: LedgerRepository
    private lateinit var ledgerVm: LedgerViewModel

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    private fun mountHost(seed: suspend (LedgerRepository) -> Unit) {
        database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(reminderConstraints)
            .build()
        val vehicleRepo = VehicleRepository(database.vehicleDao())
        ledgerRepo = LedgerRepository(database)
        val reminderRepo = ReminderRepository(database, ledgerRepo, clock)
        runBlocking {
            database.vehicleDao().insert(vehicle)
            seed(ledgerRepo)
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
            val homeVm = provider["hist-home", HomeViewModel::class.java]
            ledgerVm = provider["hist-ledger", LedgerViewModel::class.java]
            val vehicleVm = provider["hist-vehicles", VehicleViewModel::class.java]
            val reminderVm = provider["hist-reminders", RemindersViewModel::class.java]
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
    fun historySearchCategoryFilterMonthGroupingAndDetailSheetFlowsWorkEndToEnd() {
        val fuelId = UUID.randomUUID()
        val oilId = UUID.randomUUID()
        mountHost { ledger ->
            ledger.create(
                fuelId,
                2_000L,
                EntryInput(
                    vehicleId = vehicle.id,
                    category = "fuel",
                    occurredOn = "2026-03-21", // 1405-01-01
                    amountRials = 2_500_000L,
                    fuelVolumeLiters = "35",
                    notes = "بنزین جایگاه آزادی",
                ),
            )
            ledger.create(
                oilId,
                1_000L,
                EntryInput(
                    vehicleId = vehicle.id,
                    category = "oil_change",
                    occurredOn = "2026-03-10", // 1404-12-19
                    amountRials = 9_500_000L,
                    notes = "تعويض روغن بهران",
                ),
            )
        }

        compose.waitUntil(5_000) {
            ShadowLooper.idleMainLooper()
            compose.onAllNodesWithTag("homeOpenHistory").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("homeOpenHistory").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            ShadowLooper.idleMainLooper()
            compose.onAllNodesWithTag("ledgerHistoryList").fetchSemanticsNodes().isNotEmpty()
        }

        // Both Jalali month headers appear
        compose.onNodeWithTag("ledgerHistoryList").performScrollToNode(hasTestTag("historyMonthHeader:1405-1"))
        compose.onNodeWithTag("historyMonthHeader:1405-1").assertIsDisplayed()
        compose.onNodeWithTag("ledgerHistoryList").performScrollToNode(hasTestTag("historyMonthHeader:1404-12"))
        compose.onNodeWithTag("historyMonthHeader:1404-12").assertIsDisplayed()

        // Filter by Fuel category -> oil entry disappears
        compose.onNodeWithTag("historyFilter:fuel").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("ledgerHistoryList").performScrollToNode(hasTestTag("ledgerRecent:$fuelId"))
        compose.onNodeWithTag("ledgerRecent:$fuelId").assertIsDisplayed()
        compose.onNodeWithTag("ledgerRecent:$oilId").assertDoesNotExist()

        // Query mismatch shows empty-filter reset card
        compose.onNodeWithTag("historySearchInput").performTextReplacement("پیدا نشود")
        compose.waitForIdle()
        compose.onNodeWithTag("historyFilterEmpty").assertIsDisplayed()
        compose.onNodeWithTag("historyClearFilters").performClick()
        compose.waitForIdle()

        // Query using Persian digits + Arabic/Persian letter normalization matches oil entry
        compose.onNodeWithTag("historySearchInput").performTextReplacement("تعویض")
        compose.waitForIdle()
        compose.onNodeWithTag("ledgerHistoryList").performScrollToNode(hasTestTag("ledgerRecent:$oilId"))
        compose.onNodeWithTag("ledgerRecent:$oilId").assertIsDisplayed()
        compose.onNodeWithTag("ledgerRecent:$fuelId").assertDoesNotExist()

        // Clear search brings back all entries
        compose.onNodeWithTag("historySearchClear").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("ledgerHistoryList").performScrollToNode(hasTestTag("ledgerRecent:$fuelId"))
        compose.onNodeWithTag("ledgerRecent:$fuelId").assertIsDisplayed()

        // Tap Fuel card -> opens read-only EntryDetailSheet (not directly edit draft)
        compose.onNodeWithTag("ledgerRecent:$fuelId").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("entryDetailSheet").fetchSemanticsNodes().isNotEmpty() }
        assertNull(ledgerVm.state.value.draft)

        // Tap Edit inside detail sheet -> enters EntryForm with loaded draft
        compose.onNodeWithTag("entryDetailEdit").performClick()
        compose.waitUntil(5_000) {
            ShadowLooper.idleMainLooper()
            ledgerVm.state.value.draft?.id == fuelId
        }
        compose.activity.runOnUiThread { ledgerVm.back() }
        compose.waitForIdle()

        // Tap Oil Change card -> open detail sheet -> Delete -> confirm dialog deletes row
        compose.onNodeWithTag("ledgerHistoryList").performScrollToNode(hasTestTag("ledgerRecent:$oilId"))
        compose.onNodeWithTag("ledgerRecent:$oilId").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("entryDetailDelete").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("entryDetailDelete").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("ledgerConfirmDelete").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("ledgerConfirmDelete").performClick()

        compose.waitUntil(5_000) {
            ShadowLooper.idleMainLooper()
            ledgerVm.state.value.entries?.size == 1
        }
        assertEquals(fuelId.toString(), ledgerVm.state.value.entries?.single()?.id)
    }
}
