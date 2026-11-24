package org.unishiasy.vehiclelog.ui.home

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.unishiasy.vehiclelog.MainActivity
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.OdometerInput
import org.unishiasy.vehiclelog.data.VehicleRepository
import org.unishiasy.vehiclelog.data.local.HistoryEntry
import org.unishiasy.vehiclelog.data.local.ReminderEntity
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.ui.theme.VehicleLogTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeScreenRobolectricTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val clock = Clock.fixed(Instant.parse("2026-11-23T10:00:00Z"), ZoneOffset.UTC)
    private lateinit var database: VehicleDatabase
    private lateinit var vehicleRepo: VehicleRepository
    private lateinit var ledgerRepo: LedgerRepository
    private lateinit var homeVm: HomeViewModel

    private val testVehicle = VehicleEntity(
        id = "vehicle-207",
        kind = "car",
        displayLabel = "پژو ۲۰۷",
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        vehicleRepo = VehicleRepository(database.vehicleDao())
        ledgerRepo = LedgerRepository(database)
        homeVm = HomeViewModel(vehicleRepo, ledgerRepo, clock = clock)
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    private fun renderHomeScreen(
        state: HomeUiState? = null,
        dueReminders: List<ReminderEntity>? = null,
        onOpenDashboard: (VehicleEntity) -> Unit = {},
        onOpenHistory: (VehicleEntity) -> Unit = {},
        onOpenReminders: (VehicleEntity) -> Unit = {},
        onEditVehicle: (VehicleEntity) -> Unit = {},
        fontScale: Float = 1.0f,
        widthDp: Int? = null,
    ) {
        val act = compose.activity
        act.runOnUiThread {
            act.setContent {
                val currentDensity = LocalDensity.current
                val density = if (fontScale != 1.0f) {
                    Density(currentDensity.density, fontScale = fontScale)
                } else currentDensity
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalDensity provides density,
                ) {
                    Box(
                        modifier = if (widthDp != null) Modifier.width(widthDp.dp).fillMaxHeight() else Modifier
                    ) {
                        VehicleLogTheme {
                            val currentState = if (state != null) {
                                state
                            } else {
                                val s by homeVm.state.collectAsStateWithLifecycle()
                                s
                            }
                            HomeScreen(
                                viewModel = homeVm,
                                state = currentState,
                                onOpenHistory = onOpenHistory,
                                onOpenDashboard = { v, _ -> onOpenDashboard(v) },
                                onAddVehicle = {},
                                onEditVehicle = onEditVehicle,
                                onOpenReminders = onOpenReminders,
                                dueReminders = dueReminders,
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun vehicleSelectorHeaderDisplaysVehicleNameAndFormattedMileage() {
        val state = HomeUiState(
            vehicles = listOf(testVehicle),
            selectedVehicleId = testVehicle.id,
            selectedVehicleMileage = 61641L,
            mileageLoaded = true,
        )

        renderHomeScreen(state = state)

        // Presence of vehicle selector header: پژو ۲۰۷ • ۶۱٬۶۴۱ کیلومتر
        compose.onNodeWithText("پژو ۲۰۷ • ۶۱٬۶۴۱ کیلومتر").assertIsDisplayed()
        compose.onNodeWithTag("homeSelectedVehicleCard").assertIsDisplayed()
        compose.onNodeWithTag("homeOpenVehicleSwitcher").assertIsDisplayed()
    }

    @Test
    fun fuelConsumptionMetricCardShowsCalculatedAverageWhenSufficientData() {
        // 2 full tank entries: delta = 500 km, fuel = 40.0 L -> (40.0 / 500) * 100 = 8.0 L/100km
        val entry1 = HistoryEntry(
            id = "entry-1",
            vehicleId = testVehicle.id,
            category = "fuel",
            occurredOn = "2026-11-01",
            createdAtEpochMillis = 1000L,
            notes = null,
            amountRials = 120_000_000L,
            fuelVolumeMl = 40_000L,
            linkedOdometerKm = 60_000L,
            isFullTank = true,
        )
        val entry2 = HistoryEntry(
            id = "entry-2",
            vehicleId = testVehicle.id,
            category = "fuel",
            occurredOn = "2026-11-10",
            createdAtEpochMillis = 2000L,
            notes = null,
            amountRials = 120_000_000L,
            fuelVolumeMl = 40_000L,
            linkedOdometerKm = 60_500L,
            isFullTank = true,
        )

        val state = HomeUiState(
            vehicles = listOf(testVehicle),
            selectedVehicleId = testVehicle.id,
            selectedVehicleMileage = 60_500L,
            mileageLoaded = true,
            recentHistory = listOf(entry2, entry1),
        )

        renderHomeScreen(state = state)

        compose.onNodeWithText("میانگین مصرف").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("۸٫۰ لیتر").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("در ۱۰۰ کیلومتر").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun fuelConsumptionMetricCardShowsPlaceholderWhenLessThanTwoFullTanks() {
        val state = HomeUiState(
            vehicles = listOf(testVehicle),
            selectedVehicleId = testVehicle.id,
            selectedVehicleMileage = 60_000L,
            mileageLoaded = true,
            recentHistory = emptyList(),
        )

        renderHomeScreen(state = state)

        compose.onNodeWithText("میانگین مصرف").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ثبت ۲ باک پر").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dueRemindersAlertSectionShowsElevatedCardWithStatusPill() {
        val dueReminder = ReminderEntity(
            id = "rem-1",
            vehicleId = testVehicle.id,
            title = "تعویض روغن موتور",
            dueOn = "2026-11-20",
            dueKilometers = null,
            state = "due",
        )

        val state = HomeUiState(
            vehicles = listOf(testVehicle),
            selectedVehicleId = testVehicle.id,
            selectedVehicleMileage = 61641L,
            mileageLoaded = true,
        )

        var remindersOpened = false
        renderHomeScreen(
            state = state,
            dueReminders = listOf(dueReminder),
            onOpenReminders = { remindersOpened = true },
        )

        compose.onNodeWithTag("homeDue:rem-1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("تعویض روغن موتور").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("سررسیدشده").performScrollTo().assertIsDisplayed()

        // Clicking the alert navigates to reminders
        compose.onNodeWithTag("homeDue:rem-1").performClick()
        assertTrue(remindersOpened)
    }

    @Test
    fun quickActionChipsAreDisplayedWithAdequateTouchTargetAndAreClickable() {
        val state = HomeUiState(
            vehicles = listOf(testVehicle),
            selectedVehicleId = testVehicle.id,
            selectedVehicleMileage = 61641L,
            mileageLoaded = true,
        )

        var dashboardOpened = false
        renderHomeScreen(
            state = state,
            onOpenDashboard = { dashboardOpened = true },
        )

        compose.onNodeWithTag("homeQuickActionChips").performScrollTo()
        compose.onNodeWithTag("homeChip:overview").assertIsDisplayed()
        compose.onNodeWithTag("homeChip:monthly").assertIsDisplayed()

        // Minimum 48dp touch target
        compose.onNodeWithTag("homeChip:overview").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("homeChip:monthly").assertHeightIsAtLeast(48.dp)

        // Click quick action chip
        compose.onNodeWithTag("homeChip:monthly").performClick()
        assertTrue(dashboardOpened)
    }

    @Test
    fun longVehicleNamesDifferingAtEndRemainIdentifiableAtNarrowWidth() {
        val vehicle1 = VehicleEntity(
            id = "vehicle-206-1388",
            kind = "car",
            displayLabel = "پژو ۲۰۶ تیپ ۲ سفید مدل ۱۳۸۸",
        )
        val vehicle2 = VehicleEntity(
            id = "vehicle-206-1390",
            kind = "car",
            displayLabel = "پژو ۲۰۶ تیپ ۲ سفید مدل ۱۳۹۰",
        )

        runBlocking {
            database.vehicleDao().insert(vehicle1)
            database.vehicleDao().insert(vehicle2)
            ledgerRepo.createStandaloneReading(
                UUID.randomUUID(),
                OdometerInput(vehicle1.id, "2026-11-23", 123456L)
            )
            ledgerRepo.createStandaloneReading(
                UUID.randomUUID(),
                OdometerInput(vehicle2.id, "2026-11-23", 123456L)
            )
        }

        renderHomeScreen(
            fontScale = 1.5f,
            widthDp = 320,
        )

        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("homeSelectedVehicleCard").fetchSemanticsNodes().isNotEmpty()
        }

        // 1. Assert that the full distinguishing text of the selected vehicle is displayed
        compose.onNodeWithTag("homeSelectedVehicleName", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("پژو ۲۰۶ تیپ ۲ سفید مدل ۱۳۸۸")
        compose.onNodeWithTag("homeVehicleHeaderSummary", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("پژو ۲۰۶ تیپ ۲ سفید مدل ۱۳۸۸", substring = true)
            .assertTextContains("۱۲۳٬۴۵۶ کیلومتر", substring = true)

        // 2. Assert that the switch indicator is displayed
        compose.onNodeWithTag("homeVehicleSwitchIndicator", useUnmergedTree = true)
            .assertIsDisplayed()

        // 3. Assert that the edit button is displayed and has >= 48dp touch target
        compose.onNodeWithTag("homeEditVehicle:${vehicle1.id}")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        // 4. Switch to the second vehicle, verify its distinguishing text is displayed
        compose.onNodeWithTag("homeOpenVehicleSwitcher").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("switchVehicle:${vehicle2.id}").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("switchVehicle:${vehicle2.id}").performClick()

        compose.waitUntil(5_000) {
            homeVm.state.value.selectedVehicle?.id == vehicle2.id
        }

        compose.onNodeWithTag("homeSelectedVehicleName", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("پژو ۲۰۶ تیپ ۲ سفید مدل ۱۳۹۰")
        compose.onNodeWithTag("homeVehicleHeaderSummary", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("پژو ۲۰۶ تیپ ۲ سفید مدل ۱۳۹۰", substring = true)
            .assertTextContains("۱۲۳٬۴۵۶ کیلومتر", substring = true)
        compose.onNodeWithTag("homeEditVehicle:${vehicle2.id}")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }
}
