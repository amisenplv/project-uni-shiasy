package org.unishiasy.vehiclelog.ui.ledger

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.room.Room
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.unishiasy.vehiclelog.MainActivity
import org.unishiasy.vehiclelog.data.EntryInput
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.VehicleRepository
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.ui.theme.VehicleLogTheme
import org.unishiasy.vehiclelog.ui.vehicle.VehicleViewModel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LedgerScreenRobolectricTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val clock = Clock.fixed(Instant.parse("2026-11-23T10:00:00Z"), ZoneOffset.UTC)
    private lateinit var database: VehicleDatabase
    private lateinit var vehicleRepo: VehicleRepository
    private lateinit var ledgerRepo: LedgerRepository
    private lateinit var vehicleVm: VehicleViewModel
    private lateinit var ledgerVm: LedgerViewModel

    private val testVehicle = VehicleEntity(
        id = "vehicle-207",
        kind = "car",
        displayLabel = "پژو ۲۰۷",
    )

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.vehicleDao().insert(testVehicle)
        vehicleRepo = VehicleRepository(database.vehicleDao())
        ledgerRepo = LedgerRepository(database)
        vehicleVm = VehicleViewModel(vehicleRepo)
        ledgerVm = LedgerViewModel(ledgerRepo, clock = clock)
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    private fun renderLedgerHost() {
        val act = compose.activity
        act.runOnUiThread {
            act.setContent {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    VehicleLogTheme {
                        LedgerHost(
                            vehicles = vehicleVm,
                            ledger = ledgerVm,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun displaysMonthlyGroupHeadersWithSummary(): Unit = runBlocking {
        // Entry 1: Aban 1405 (2026-10-25)
        ledgerRepo.create(
            id = UUID.randomUUID(),
            createdAtEpochMillis = 1000L,
            input = EntryInput(
                vehicleId = testVehicle.id,
                category = "fuel",
                occurredOn = "2026-10-25",
                amountRials = 1200000L, // 120,000 Toman
                fuelVolumeLiters = "40",
                stationName = "پمپ بنزین کاوه",
                isFullTank = true,
            ),
            kilometers = 61641L,
        )

        // Entry 2: Khordad 1405 (2026-06-10)
        ledgerRepo.create(
            id = UUID.randomUUID(),
            createdAtEpochMillis = 2000L,
            input = EntryInput(
                vehicleId = testVehicle.id,
                category = "oil_change",
                occurredOn = "2026-06-10",
                amountRials = 5000000L, // 500,000 Toman
            ),
            kilometers = 55000L,
        )

        ledgerVm.openVehicle(testVehicle)
        renderLedgerHost()

        // Month headers should be displayed
        compose.onNodeWithTag("historyMonthHeader:1405-8").assertIsDisplayed().assertTextContains("آبان ۱۴۰۵")
        compose.onNodeWithTag("historyMonthSummary:1405-8").assertIsDisplayed().assertTextContains("۱۲۰٬۰۰۰ تومان")

        // Scroll to and verify Khordad month group
        compose.onNodeWithTag("ledgerHistoryList").performScrollToNode(hasTestTag("historyMonthHeader:1405-3"))
        compose.onNodeWithTag("historyMonthHeader:1405-3").assertIsDisplayed().assertTextContains("خرداد ۱۴۰۵")
        compose.onNodeWithTag("historyMonthSummary:1405-3").assertIsDisplayed().assertTextContains("۵۰۰٬۰۰۰ تومان")
    }

    @Test
    fun displaysRichLogCardWithStationNameRateAndFullTankBadge(): Unit = runBlocking {
        val entryId = UUID.randomUUID()
        ledgerRepo.create(
            id = entryId,
            createdAtEpochMillis = 1000L,
            input = EntryInput(
                vehicleId = testVehicle.id,
                category = "fuel",
                occurredOn = "2026-10-25",
                amountRials = 1200000L, // 120,000 Toman
                fuelVolumeLiters = "40",
                stationName = "پمپ بنزین کاوه",
                isFullTank = true,
            ),
            kilometers = 61641L,
        )

        ledgerVm.openVehicle(testVehicle)
        renderLedgerHost()

        // Check category label inside card
        compose.onNodeWithTag("ledgerRecent:$entryId").assertTextContains("سوخت")

        // Check amount inside card
        compose.onNodeWithTag("ledgerRecent:$entryId").assertIsDisplayed().assertTextContains("۱۲۰٬۰۰۰ تومان")

        // Check rate calculation (contains "لیتر ←" and "تومان/لیتر")
        compose.onNode(hasText("لیتر ←", substring = true) and hasText("تومان/لیتر", substring = true))
            .assertIsDisplayed()

        // Check station name inside card
        compose.onNodeWithTag("ledgerRecent:$entryId").assertTextContains("پمپ بنزین کاوه")

        // Check full tank badge inside card
        compose.onNodeWithTag("ledgerRecent:$entryId").assertTextContains("باک پر")
    }

    @Test
    fun handlesZeroRateAndUnpricedFuelGracefullyWithoutCrashing(): Unit = runBlocking {
        val entryId = UUID.randomUUID()
        ledgerRepo.create(
            id = entryId,
            createdAtEpochMillis = 1000L,
            input = EntryInput(
                vehicleId = testVehicle.id,
                category = "fuel",
                occurredOn = "2026-10-25",
                amountRials = 1L, // Extremely small amount that yields ratePerLiterToman == 0
                fuelVolumeLiters = "4000",
            ),
        )

        ledgerVm.openVehicle(testVehicle)
        renderLedgerHost()

        // Card should display safely with liters without crashing
        compose.onNodeWithTag("ledgerRecent:$entryId").assertIsDisplayed()
        compose.onNode(hasText("۴۰۰۰", substring = true) and hasText("لیتر", substring = true)).assertIsDisplayed()
    }

    @Test
    fun inspectingEntryInDetailSheetShowsStationAndFullTank(): Unit = runBlocking {
        val entryId = UUID.randomUUID()
        ledgerRepo.create(
            id = entryId,
            createdAtEpochMillis = 1000L,
            input = EntryInput(
                vehicleId = testVehicle.id,
                category = "fuel",
                occurredOn = "2026-10-25",
                amountRials = 1200000L,
                fuelVolumeLiters = "40",
                stationName = "پمپ بنزین کاوه",
                isFullTank = true,
            ),
            kilometers = 61641L,
        )

        ledgerVm.openVehicle(testVehicle)
        renderLedgerHost()

        // Click card to inspect
        compose.onNodeWithTag("ledgerRecent:$entryId").performClick()
        compose.waitForIdle()

        // Detail sheet should be shown
        compose.onNodeWithTag("entryDetailSheet").assertIsDisplayed()
        compose.onNodeWithTag("entryDetailStationName").assertExists().assertTextContains("پمپ بنزین کاوه")
        compose.onNodeWithTag("entryDetailFullTank").assertIsDisplayed()
    }

    @Test
    fun entryFormSupportsFullTankToggleAndStationName(): Unit = runBlocking {
        ledgerVm.openQuickAdd(testVehicle, EntryCategory.Fuel)
        renderLedgerHost()

        // Top bar action buttons: Close (ledgerCloseEntry) and Save (ledgerSave)
        compose.onNodeWithTag("ledgerCloseEntry").assertIsDisplayed()
        compose.onNodeWithTag("ledgerSave").assertIsDisplayed()

        // Full tank switch should be present and toggleable
        val fullTankSwitch = compose.onNodeWithTag("ledgerFullTankSwitch")
        fullTankSwitch.assertIsDisplayed()
        fullTankSwitch.assertIsOff()
        fullTankSwitch.performClick()
        fullTankSwitch.assertIsOn()

        // Station name field should be present
        val stationField = compose.onNodeWithTag("ledgerStationName")
        stationField.assertIsDisplayed()
        stationField.performTextInput("پمپ بنزین آزادی")

        // Input required fields to save
        compose.onNodeWithTag("ledgerAmount").performTextInput("120000")
        compose.onNodeWithTag("ledgerVolume").performTextInput("40")

        // Save
        compose.onNodeWithTag("ledgerSave").performClick()
        compose.waitForIdle()

        // Verify in database that stationName and isFullTank were persisted
        val history = database.ledgerDao().observeHistoryForVehicle(testVehicle.id).first()
        val saved = history.firstOrNull()
        assertNotNull("Entry should be saved", saved)
        assertEquals("پمپ بنزین آزادی", saved?.stationName)
        assertEquals(true, saved?.isFullTank)
    }

    @Test
    fun optionalMileageDisclosureExposesAccessibilityStateAndPersistsInputAcrossToggle(): Unit = runBlocking {
        ledgerVm.openQuickAdd(testVehicle, EntryCategory.Fuel)
        renderLedgerHost()

        val toggle = compose.onNodeWithTag("ledgerDetailsToggle").performScrollTo()
        toggle.assertIsDisplayed()

        // 1. Initially collapsed: exposes Role.Button and stateDescription «بسته‌شده»
        toggle.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        toggle.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "بسته‌شده"))
        toggle.assertTextContains("کارکرد و یادداشت (اختیاری)")

        // 2. Triangle indicator is decorative (no separate accessibility node with "▼" or "▲")
        compose.onNodeWithText("▼").assertDoesNotExist()
        compose.onNodeWithText("▲").assertDoesNotExist()

        // 3. Collapsed fields leave the accessibility hierarchy
        compose.onNodeWithTag("ledgerMileage").assertDoesNotExist()
        compose.onNodeWithTag("ledgerNotes").assertDoesNotExist()

        // 4. Expand disclosure
        toggle.performClick()
        compose.waitForIdle()

        // Exposes stateDescription «بازشده» when expanded
        toggle.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "بازشده"))

        // Triangle still decorative
        compose.onNodeWithText("▼").assertDoesNotExist()
        compose.onNodeWithText("▲").assertDoesNotExist()

        // Fields are now in the accessibility hierarchy
        compose.onNodeWithTag("ledgerMileage").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("ledgerNotes").performScrollTo().assertIsDisplayed()

        // 5. Enter mileage
        compose.onNodeWithTag("ledgerMileage").performScrollTo().performTextInput("123456")
        compose.waitForIdle()
        assertEquals("123456", ledgerVm.state.value.draft?.mileage)

        // 6. Collapse and verify hidden fields leave accessibility hierarchy
        toggle.performScrollTo().performClick()
        compose.waitForIdle()

        toggle.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "بسته‌شده"))
        compose.onNodeWithTag("ledgerMileage").assertDoesNotExist()
        compose.onNodeWithTag("ledgerNotes").assertDoesNotExist()

        // 7. Reopen and verify value persists across collapse and reopen
        toggle.performScrollTo().performClick()
        compose.waitForIdle()

        toggle.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "بازشده"))
        compose.onNodeWithTag("ledgerMileage").performScrollTo().assertIsDisplayed()
        assertEquals("123456", ledgerVm.state.value.draft?.mileage)
    }
}
