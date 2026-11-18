package org.unishiasy.vehiclelog.ui.home

import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.unishiasy.vehiclelog.MainActivity
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.EntryInput
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.OdometerInput
import org.unishiasy.vehiclelog.data.ReminderRepository
import org.unishiasy.vehiclelog.data.ReminderInput
import org.unishiasy.vehiclelog.data.local.reminderConstraints
import org.unishiasy.vehiclelog.ui.reminders.RemindersViewModel
import org.unishiasy.vehiclelog.data.VehicleRepository
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.ui.ledger.LedgerHost
import org.unishiasy.vehiclelog.ui.ledger.LedgerViewModel
import org.unishiasy.vehiclelog.ui.theme.VehicleLogTheme
import org.unishiasy.vehiclelog.ui.vehicle.VehicleViewModel

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val vehicleA = VehicleEntity("test-v-a", "car", "سمند LX", productionYear = 1400, productionYearCalendar = "solar_hijri")
    private val vehicleB = VehicleEntity("test-v-b", "car", "پژو ۲۰۶ SD", productionYear = 1402, productionYearCalendar = "solar_hijri")
    private val instant = Instant.parse("2026-03-20T20:30:00Z")
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)

    @Volatile private var reminderInstant = instant
    private val reminderClock = object : Clock() {
        override fun instant(): Instant = reminderInstant
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(instant(), zone)
    }
    private lateinit var reminderRepo: ReminderRepository
    private lateinit var reminderVm: RemindersViewModel
    private lateinit var database: VehicleDatabase
    private lateinit var vehicleRepo: VehicleRepository
    private lateinit var ledgerRepo: LedgerRepository
    private lateinit var homeVm: HomeViewModel
    private lateinit var ledgerVm: LedgerViewModel
    private lateinit var vehicleVm: VehicleViewModel
    private var activity: MainActivity? = null

    @After
    fun cleanUp() {
        instrumentation.runOnMainSync {
            activity?.viewModelStore?.clear()
            activity?.finish()
        }
        if (::database.isInitialized) database.close()
    }

    private fun prepare(seed: Boolean = true, twoVehicles: Boolean = true, preSeedReadings: Boolean = false) {
        database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java).addCallback(reminderConstraints).build()
        vehicleRepo = VehicleRepository(database.vehicleDao())
        ledgerRepo = LedgerRepository(database)
        reminderRepo = ReminderRepository(database, ledgerRepo, reminderClock)

        if (seed) runBlocking {
            database.vehicleDao().insert(vehicleA)
            if (twoVehicles) database.vehicleDao().insert(vehicleB)
            if (preSeedReadings) {
                ledgerRepo.createStandaloneReading(UUID.randomUUID(), OdometerInput(vehicleA.id, "2026-03-20", 1200L))
                if (twoVehicles) {
                    ledgerRepo.createStandaloneReading(UUID.randomUUID(), OdometerInput(vehicleB.id, "2026-03-20", 2500L))
                }
            }
        }

        launchActivity()
        activity = checkNotNull(resumedActivity())
        attach()
    }

    private fun launchActivity() {
        val component = ComponentName(context, MainActivity::class.java)
        val command = "am start -W -a ${Intent.ACTION_MAIN} -c ${Intent.CATEGORY_LAUNCHER} -n ${component.flattenToString()}"
        val launch = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command),
        ).bufferedReader().use { it.readText() }
        assertTrue(launch, launch.lineSequence().any { it.trim() == "Status: ok" })

    }

    private fun attach(fontScale: Float? = null) {
        instrumentation.runOnMainSync {
            val act = checkNotNull(activity)
            val factory = viewModelFactory {
                initializer { HomeViewModel(vehicleRepo, ledgerRepo) }
                initializer { LedgerViewModel(ledgerRepo, clock) }
                initializer { VehicleViewModel(vehicleRepo) }
                initializer { RemindersViewModel(reminderRepo, ledgerRepo, reminderClock) }
            }
            val provider = ViewModelProvider(act, factory)
            reminderVm = provider["test-reminders", RemindersViewModel::class.java]
            homeVm = provider["test-home", HomeViewModel::class.java]
            ledgerVm = provider["test-ledger", LedgerViewModel::class.java]
            vehicleVm = provider["test-vehicles", VehicleViewModel::class.java]

            act.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalDensity provides Density(density.density, fontScale ?: density.fontScale),
                ) {
                    VehicleLogTheme {
                        LedgerHost(vehicleVm, ledgerVm, homeVm, reminderVm)
                    }
                }
            }
        }
    }

    private fun resumedActivity(): MainActivity? {
        var resumed: MainActivity? = null
        instrumentation.runOnMainSync {
            resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>().singleOrNull()
        }
        return resumed
    }

    @Test
    fun zeroVehiclesShowsSetupExperience() {
        prepare(seed = false)
        compose.waitUntil(5_000) { compose.onAllNodesWithText(context.getString(R.string.vehicle_empty)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(context.getString(R.string.vehicle_empty)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.vehicle_add)).assertIsDisplayed()
        compose.onNodeWithTag("homeScreen").assertDoesNotExist()
        compose.onNodeWithTag("homeQuickAdd").assertDoesNotExist()
    }

    @Test
    fun oneVehicleShowsMileageAndDirectNavigation() {
        prepare(seed = true, twoVehicles = false)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("homeScreen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("homeScreen").assertIsDisplayed()
        compose.onNodeWithText(vehicleA.displayLabel).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.ledger_mileage_reference_empty)).assertIsDisplayed()
        compose.onNodeWithTag("homeQuickAdd").assertIsDisplayed()
        compose.onNodeWithTag("homeOpenHistory").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("homeOpenDashboard").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("homeVehicleSelector").assertDoesNotExist()
    }

    @Test
    fun multipleVehiclesSwitchesSelectionAndCurrentMileage() {
        prepare(seed = true, twoVehicles = true, preSeedReadings = true)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("homeOpenVehicleSwitcher").fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("homeSelectedVehicleName", useUnmergedTree = true).assertTextContains(vehicleA.displayLabel)
        compose.onNodeWithTag("homeMileageValue", useUnmergedTree = true).assertTextContains("۱۲۰۰", substring = true)

        compose.onNodeWithTag("homeOpenVehicleSwitcher").performScrollTo().performClick()
        compose.onNodeWithTag("switchVehicle:${vehicleB.id}").performClick()
        compose.waitUntil(5_000) { homeVm.state.value.selectedVehicle?.id == vehicleB.id }

        compose.onNodeWithTag("homeSelectedVehicleName", useUnmergedTree = true).assertTextContains(vehicleB.displayLabel)
        compose.onNodeWithTag("homeMileageValue", useUnmergedTree = true).assertTextContains("۲۵۰۰", substring = true)

        compose.onNodeWithTag("homeOpenVehicleSwitcher").performScrollTo().performClick()
        compose.onNodeWithTag("switchVehicle:${vehicleA.id}").performClick()
        compose.waitUntil(5_000) { homeVm.state.value.selectedVehicle?.id == vehicleA.id }
        compose.onNodeWithTag("homeSelectedVehicleName", useUnmergedTree = true).assertTextContains(vehicleA.displayLabel)
        compose.onNodeWithTag("homeMileageValue", useUnmergedTree = true).assertTextContains("۱۲۰۰", substring = true)
    }

    @Test
    fun homeShortcutsAndSaveWorkWithSoftwareKeyboard() {
        prepare()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("homeQuickAdd").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("homeQuickAdd").assertIsDisplayed()
        compose.onNodeWithTag("homeAddExpense").assertIsDisplayed().performClick()
        compose.onNodeWithTag("ledgerCategory").assertIsDisplayed()
        compose.onNodeWithTag("ledgerVolume").assertDoesNotExist()
        assertEquals("other", ledgerVm.state.value.draft?.category?.key)
        instrumentation.runOnMainSync {
            ledgerVm.back()
            ledgerVm.back()
        }

        compose.onNodeWithTag("homeQuickAdd").assertIsDisplayed().performClick()
        assertEquals("fuel", ledgerVm.state.value.draft?.category?.key)
        compose.onNodeWithTag("ledgerVolume").assertIsDisplayed()
        val amount = compose.onNodeWithTag("ledgerAmount").assertIsDisplayed()
        amount.performTouchInput { click() }
        compose.waitUntil(10_000) {
            compose.runOnIdle {
                ViewCompat.getRootWindowInsets(checkNotNull(activity).window.decorView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            }
        }
        amount.performTextReplacement("۰")
        val save = compose.onNodeWithTag("ledgerSave").assertIsDisplayed()
        val width = checkNotNull(activity).resources.configuration.screenWidthDp
        val screenshot = File(checkNotNull(context.getExternalFilesDir(null)), "stage2-fuel-ime-${width}dp.png")
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        screenshot.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        save.performTouchInput { click() }
        compose.runOnIdle {
            assertTrue(ledgerVm.state.value.fieldErrors.isNotEmpty())
            assertEquals("۰", ledgerVm.state.value.draft?.amount)
        }
    }

    @Test
    fun directNavigationOpensQuickAddHistoryAndDashboard() {
        prepare(seed = true, twoVehicles = false)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("homeScreen").fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("homeQuickAdd").performClick()
        compose.onNodeWithText(context.getString(R.string.ledger_add)).assertIsDisplayed()
        compose.onNodeWithText(vehicleA.displayLabel).assertIsDisplayed()
        compose.onNodeWithTag("ledgerSave").assertIsDisplayed()

        instrumentation.runOnMainSync { ledgerVm.back() }
        instrumentation.runOnMainSync { ledgerVm.back() }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("homeScreen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("homeScreen").assertIsDisplayed()

        compose.onNodeWithTag("homeOpenHistory").performScrollTo().performClick()
        compose.onNodeWithTag("ledgerHistoryList").assertIsDisplayed()

        compose.onNodeWithText(context.getString(R.string.ledger_back)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("homeScreen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("homeScreen").assertIsDisplayed()

        compose.onNodeWithTag("homeOpenDashboard").performScrollTo().performClick()
        compose.onNodeWithTag("monthlyDashboard").assertIsDisplayed()

        compose.onNodeWithText(context.getString(R.string.dashboard_back)).performClick()
        compose.onNodeWithText(context.getString(R.string.ledger_back)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("homeScreen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("homeScreen").assertIsDisplayed()
    }

    @Test
    fun activityRecreationPreservesSelectedVehicleContext() {
        prepare(seed = true, twoVehicles = true, preSeedReadings = true)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("homeOpenVehicleSwitcher").fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("homeOpenVehicleSwitcher").performScrollTo().performClick()
        compose.onNodeWithTag("switchVehicle:${vehicleB.id}").performClick()
        compose.waitUntil(5_000) { homeVm.state.value.selectedVehicle?.id == vehicleB.id }
        assertEquals(vehicleB.id, homeVm.state.value.selectedVehicle?.id)

        instrumentation.runOnMainSync {
            activity?.recreate()
        }
        instrumentation.waitForIdleSync()

        compose.waitUntil(5_000) {
            val resumed = resumedActivity()
            resumed != null && resumed != activity
        }
        activity = checkNotNull(resumedActivity())
        attach()

        compose.waitUntil(5_000) { compose.onAllNodesWithTag("homeScreen").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(vehicleB.id, homeVm.state.value.selectedVehicle?.id)
        compose.onNodeWithTag("homeSelectedVehicleName", useUnmergedTree = true).assertTextContains(vehicleB.displayLabel)
        compose.onNodeWithTag("homeMileageValue", useUnmergedTree = true).assertTextContains("۲۵۰۰", substring = true)
    }

    @Test
    fun quickAddAndDashboardRegressionsPassFromHome() {
        prepare(seed = true, twoVehicles = false)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("homeScreen").fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("homeQuickAdd").performClick()
        compose.onNodeWithTag("ledgerAmount").performScrollTo().performTextReplacement("۱۵٬۰۰۰")
        compose.onNodeWithTag("ledgerVolume").performScrollTo().performTextReplacement("۳۰")
        if (compose.onAllNodesWithTag("ledgerMileage").fetchSemanticsNodes().isEmpty()) {
            compose.onNodeWithTag("ledgerDetailsToggle").performScrollTo().performClick()
        }
        compose.onNodeWithTag("ledgerMileage").performScrollTo().performTextReplacement("۱۲۵۰")
        compose.onNodeWithTag("ledgerDate").performScrollTo().performTextReplacement("۱۴۰۵/۰۱/۰۱")
        compose.onNodeWithTag("ledgerSave").assertIsDisplayed().performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithText(context.getString(R.string.ledger_saved)).fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithText(context.getString(R.string.ledger_back)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("homeScreen").fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("homeMileageValue", useUnmergedTree = true).assertTextContains("۱۲۵۰", substring = true)

        compose.onNodeWithTag("homeOpenDashboard").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("monthlyDashboard").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("dashboardTotal").assertTextContains("۱۵٬۰۰۰ تومان")
    }

    @Test
    fun remindersCreateValidateAndChangeLifecycleForSelectedVehicle() {
        prepare()
        openReminders()
        clickReminder("reminderSave")
        compose.onNodeWithText(context.getString(R.string.reminder_title_required)).assertExists()
        compose.onNodeWithText(context.getString(R.string.reminder_date_invalid)).assertExists()
        enterReminder("reminderTitle", "تعویض روغن")
        enterReminder("reminderDate", "۱۴۰۴/۱۲/۳۰")
        clickReminder("reminderSave")
        assertTrue(runBlocking { reminderRepo.observeForVehicle(vehicleA.id).first().isEmpty() })
        enterReminder("reminderDate", "۱۴۰۵/۰۱/۰۱")
        clickReminder("reminderSave")
        awaitReminder(R.string.reminder_created)
        val date = runBlocking { reminderRepo.observeForVehicle(vehicleA.id).first().single() }
        assertEquals("2026-03-21", date.dueOn)
        compose.onNodeWithTag("remindersScreen").performScrollToNode(hasTestTag("reminder:${date.id}"))
        compose.onNodeWithText(context.getString(R.string.reminder_status_due)).assertExists()

        enterReminder("reminderTitle", "تعویض فیلتر")
        clickReminder("reminderMileageType")
        clickReminder("reminderSave")
        compose.onNodeWithText(context.getString(R.string.reminder_mileage_invalid)).assertExists()
        enterReminder("reminderMileage", "۱٢٠۰")
        clickReminder("reminderSave")
        awaitReminder(R.string.reminder_created)
        val mileage = runBlocking {
            reminderRepo.observeForVehicle(vehicleA.id).first().single { it.id != date.id }
        }
        assertEquals(1200L, mileage.dueKilometers)
        compose.onNodeWithTag("remindersScreen").performScrollToNode(hasTestTag("reminder:${mileage.id}"))
        compose.onNodeWithText(context.getString(R.string.reminder_status_pending)).assertExists()
        assertTrue(compose.onAllNodesWithText(context.getString(R.string.reminder_missing_mileage)).fetchSemanticsNodes().isNotEmpty())

        clickReminder("remindersBack")
        compose.onNodeWithTag("homeDue:${date.id}").assertExists()
        compose.onNodeWithTag("homeDue:${mileage.id}").assertDoesNotExist()
        runBlocking { ledgerRepo.createStandaloneReading(UUID.randomUUID(), OdometerInput(vehicleA.id, "2026-03-21", 1200)) }
        awaitTag("homeDue:${mileage.id}")

        compose.onNodeWithTag("homeOpenVehicleSwitcher").performScrollTo().performClick()
        compose.onNodeWithTag("switchVehicle:${vehicleB.id}").performClick()
        compose.waitUntil(5_000) { reminderVm.state.value.observedVehicleId == vehicleB.id && reminderVm.state.value.list != null }
        compose.onNodeWithTag("homeDue:${date.id}").assertDoesNotExist()
        compose.onNodeWithTag("homeDue:${mileage.id}").assertDoesNotExist()
        openReminders()
        compose.onNodeWithTag("remindersScreen").performScrollToNode(
            androidx.compose.ui.test.hasText(context.getString(R.string.reminders_empty)),
        )
        clickReminder("remindersBack")

        compose.onNodeWithTag("homeOpenVehicleSwitcher").performScrollTo().performClick()
        compose.onNodeWithTag("switchVehicle:${vehicleA.id}").performClick()
        awaitTag("homeDue:${mileage.id}")
        openReminders()
        clickReminder("reminderComplete:${date.id}")
        awaitReminder(R.string.reminder_completed)
        compose.onNodeWithText(context.getString(R.string.reminder_status_completed)).assertExists()
        clickReminder("reminderCancel:${mileage.id}")
        compose.onNodeWithTag("reminderConfirmCancel").performClick()
        awaitReminder(R.string.reminder_canceled)
        compose.onNodeWithText(context.getString(R.string.reminder_status_canceled)).assertExists()
        clickReminder("remindersBack")
        compose.onNodeWithTag("homeDue:${date.id}").assertDoesNotExist()
        compose.onNodeWithTag("homeDue:${mileage.id}").assertDoesNotExist()
    }

    @Test
    fun failedSaveAndRecreationKeepTheDraftAndRepeatedSaveWritesOnce() {
        prepare(twoVehicles = false)
        openReminders()
        enterReminder("reminderTitle", "بازبینی ترمز")
        enterReminder("reminderDate", "۱۴۰۵/۰۱/۰۱")
        val draftId = reminderVm.state.value.draft.id
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_test_reminder BEFORE INSERT ON reminders BEGIN SELECT RAISE(ABORT, 'test failure'); END",
        )
        clickReminder("reminderSave")
        awaitReminder(R.string.reminder_write_failed)

        instrumentation.runOnMainSync { activity?.recreate() }
        compose.waitUntil(5_000) { resumedActivity()?.let { it != activity } == true }
        activity = checkNotNull(resumedActivity())
        attach()
        awaitTag("remindersScreen")
        assertEquals(draftId, reminderVm.state.value.draft.id)
        compose.onNodeWithTag("reminderTitle").assertTextContains("بازبینی ترمز")
        compose.onNodeWithTag("reminderDate").assertTextContains("۱۴۰۵/۰۱/۰۱")

        database.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_test_reminder")
        instrumentation.runOnMainSync {
            reminderVm.save()
            reminderVm.save()
        }
        awaitReminder(R.string.reminder_created)
        val rows = runBlocking { reminderRepo.observeForVehicle(vehicleA.id).first() }
        assertEquals(listOf(draftId.toString()), rows.map { it.id })
    }

    @Test
    fun homeRefreshesDateReminderOnResumeAndAcrossForegroundMidnight() {
        reminderInstant = Instant.parse("2026-03-20T20:29:00Z")
        prepare(twoVehicles = false)
        val resumeId = UUID.randomUUID()
        val foregroundId = UUID.randomUUID()
        runBlocking {
            reminderRepo.create(vehicleA.id, resumeId, ReminderInput("روز اول", "2026-03-21"))
            reminderRepo.create(vehicleA.id, foregroundId, ReminderInput("روز دوم", "2026-03-22"))
        }
        compose.waitUntil(5_000) { reminderVm.state.value.list?.rows?.size == 2 }
        compose.onNodeWithTag("homeDue:$resumeId").assertDoesNotExist()
        instrumentation.runOnMainSync { checkNotNull(activity).moveTaskToBack(true) }
        compose.waitUntil(5_000) { resumedActivity() == null }
        instrumentation.waitForIdleSync()
        reminderInstant = Instant.parse("2026-03-21T20:29:59Z")
        launchActivity()
        awaitTag("homeDue:$resumeId")
        compose.onNodeWithTag("homeDue:$foregroundId").assertDoesNotExist()
        reminderInstant = Instant.parse("2026-03-21T20:30:00Z")
        awaitTag("homeDue:$foregroundId")
    }

    private fun openReminders() {
        awaitTag("homeOpenReminders")
        compose.onNodeWithTag("homeOpenReminders").performScrollTo().performClick()
        awaitTag("remindersScreen")
    }

    private fun enterReminder(tag: String, text: String) {
        compose.onNodeWithTag("remindersScreen").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performScrollTo().performTextReplacement(text)
    }

    private fun clickReminder(tag: String) {
        compose.onNodeWithTag("remindersScreen").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performScrollTo().performClick()
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitReminder(message: Int) {
        val text = context.getString(message)
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

}
