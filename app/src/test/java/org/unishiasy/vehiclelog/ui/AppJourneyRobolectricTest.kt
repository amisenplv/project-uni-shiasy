package org.unishiasy.vehiclelog.ui

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
import org.unishiasy.vehiclelog.domain.JalaliDates
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
                initializer { HomeViewModel(vehicleRepo, ledgerRepo, clock = clock) }
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
    fun newExpenseClosesFromTheLeftOfTheMaterialTopBarBackToHomeWithoutSaving() {
        mountHost()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("entryFab").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("entryFab").performClick()
        compose.onNodeWithTag("selectorOption:expense").performClick()

        val close = compose.onNodeWithContentDescription(context.getString(R.string.ledger_close_new_entry))
        val title = compose.onNodeWithText(context.getString(R.string.home_log_expense))
        close.assertIsDisplayed()
        assertTrue(close.fetchSemanticsNode().boundsInRoot.right <= title.fetchSemanticsNode().boundsInRoot.left)
        compose.onNodeWithText("انصراف").assertDoesNotExist()
        compose.onNodeWithTag("ledgerSave").assertIsDisplayed()

        close.performClick()
        compose.waitUntil(5_000) { ledgerVm.state.value.vehicle == null }
        compose.onNodeWithTag("homeScreen").assertIsDisplayed()
    }

    @Test
    fun historyFabOpensFuelAndBackProtectsTheDraft() {
        mountHost()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("dockTab:history").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("dockTab:history").performClick()
        compose.onNodeWithTag("entryFab").performClick()
        compose.onNodeWithTag("selectorOption:fuel").performClick()
        compose.onNodeWithTag("ledgerAmount").performTextReplacement("۱۵۰۰۰۰")

        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText(context.getString(R.string.ledger_keep_draft)).performClick()
        compose.onNodeWithTag("ledgerAmount").assertTextContains("۱۵۰٬۰۰۰", substring = true)

        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText(context.getString(R.string.ledger_discard_draft_action)).performClick()
        compose.onNodeWithTag("ledgerHistoryList").assertIsDisplayed()
        assertEquals(vehicle.id, ledgerVm.state.value.vehicle?.id)
        assertEquals(null, ledgerVm.state.value.draft)
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
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("dockTab:reminders").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("dockTab:reminders").performClick()
        compose.waitUntil(5_000) { reminderVm.state.value.list?.mileage == 12_000L }

        compose.onNodeWithTag("remindersScreen").performScrollToNode(hasTestTag("reminderMileageType"))
        compose.onNodeWithTag("reminderMileageType").performClick()
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
        compose.waitUntil(5_000) { reminderVm.state.value.list?.rows?.singleOrNull()?.state == "canceled" }
        compose.onNodeWithTag("remindersScreen").performScrollToNode(hasTestTag("reminder:$reminderId"))
        compose.onNodeWithText(context.getString(R.string.reminder_status_canceled)).assertIsDisplayed()
        compose.onNodeWithTag("remindersScreen").performScrollToNode(hasText(context.getString(R.string.reminder_canceled)))
        compose.onNodeWithText(context.getString(R.string.reminder_canceled)).assertIsDisplayed()
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
        compose.onNodeWithTag("ledgerSave").assertIsDisplayed().performClick()

        compose.waitUntil(5_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            compose.onAllNodesWithText(context.getString(R.string.ledger_mileage_conflict_action))
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(EntrySaveResult.MileageConflict, ledgerVm.state.value.result)
        compose.onNodeWithTag("ledgerMileage").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun savedFuelAndOilChangeReconcileInHomeHistoryAndReport() {
        val otherVehicle = vehicle.copy(id = "test-v-2", displayLabel = "خودروی دوم")
        val dueId = UUID.randomUUID()
        mountHost { _, reminders ->
            database.vehicleDao().insert(otherVehicle)
            reminders.create(vehicle.id, dueId, ReminderInput("یادآور نمونه", dueOn = "2026-03-21"))
        }

        fun awaitTag(tag: String) {
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                compose.onAllNodesWithTag(tag, useUnmergedTree = tag == "homeMonthTotal").fetchSemanticsNodes().isNotEmpty()
            }
        }
        fun saveEntry(expectedCount: Int) {
            compose.onNodeWithTag("ledgerSave").assertIsDisplayed().performClick()
            compose.waitUntil(5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                val state = ledgerVm.state.value
                state.draft == null && state.entries?.size == expectedCount
            }
        }

        awaitTag("entryFab")
        compose.onNodeWithTag("entryFab").performClick()
        compose.onNodeWithTag("selectorOption:fuel").performClick()
        awaitTag("ledgerAmount")
        compose.onNodeWithTag("ledgerAmount").performScrollTo().performTextReplacement("۱۵۰٬۰۰۰")
        compose.onNodeWithTag("ledgerVolume").performScrollTo().performTextReplacement("۳۰")
        saveEntry(1)
        compose.activity.runOnUiThread { ledgerVm.back() }

        awaitTag("entryFab")
        compose.onNodeWithTag("entryFab").performClick()
        compose.onNodeWithTag("selectorOption:expense").performClick()
        awaitTag("ledgerCategory")
        compose.onNodeWithTag("ledgerCategory").performScrollTo().performClick()
        compose.onNodeWithTag("ledgerCategory:oil_change").performClick()
        compose.onNodeWithTag("ledgerAmount").performScrollTo().performTextReplacement("۸۵۰٬۰۰۰")
        compose.onNodeWithTag("ledgerDetailsToggle").performScrollTo().performClick()
        compose.onNodeWithTag("ledgerMileage").performScrollTo().performTextReplacement("۵۶۰۰۰")
        saveEntry(2)

        val entries = checkNotNull(ledgerVm.state.value.entries).associateBy { it.category }
        assertEquals(setOf("fuel", "oil_change"), entries.keys)
        assertEquals(setOf("2026-03-21"), entries.values.map { it.occurredOn }.toSet())
        val fuel = entries.getValue("fuel")
        val oil = entries.getValue("oil_change")
        assertEquals(30_000L, fuel.fuelVolumeMl)
        assertEquals(1_500_000L, fuel.amountRials)
        assertEquals(8_500_000L, oil.amountRials)
        assertEquals(56_000L, oil.linkedOdometerKm)
        val oilTag = "ledgerRecent:${oil.id}"
        compose.onNodeWithTag("ledgerHistoryList").performScrollToNode(hasTestTag(oilTag))
        compose.onNodeWithTag(oilTag).assertIsDisplayed()
            .assertTextContains(checkNotNull(JalaliDates.formatGregorian(oil.occurredOn)))
            .assertTextContains("۵۶٬۰۰۰", substring = true)

        val total = "۱٬۰۰۰٬۰۰۰ تومان"
        compose.activity.runOnUiThread { ledgerVm.back() }
        awaitTag("homeMonthTotal")
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(total).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("homeMonthTotal", useUnmergedTree = true).assertTextContains(total)
        compose.onNodeWithTag("homeOpenDashboard").performScrollTo().performClick()
        awaitTag("dashboardTotal")
        compose.onNodeWithTag("dashboardVehicle").assertTextContains(vehicle.displayLabel)
        compose.onNodeWithTag("dashboardMonth").assertTextContains("فروردین ۱۴۰۵")
        compose.onNodeWithTag("dashboardTotal").assertTextContains(total)
        for ((category, subtotal) in listOf("fuel" to "۱۵۰٬۰۰۰ تومان", "oil_change" to "۸۵۰٬۰۰۰ تومان")) {
            val tag = "dashboardCategory:$category"
            compose.onNodeWithTag("monthlyDashboard").performScrollToNode(hasTestTag(tag))
            compose.onNodeWithTag(tag).assertTextContains(subtotal)
        }

        compose.onNodeWithContentDescription(context.getString(R.string.dashboard_back)).performClick()
        awaitTag("homeDue:$dueId")
        awaitTag("homeOpenVehicleSwitcher")
        compose.onNodeWithTag("homeOpenVehicleSwitcher").performScrollTo().performClick()
        awaitTag("switchVehicle:${otherVehicle.id}")
        compose.onNodeWithTag("switchVehicle:${otherVehicle.id}").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("۰ تومان").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("homeMonthTotal", useUnmergedTree = true).assertTextContains("۰ تومان")
        compose.onNodeWithTag("homeRecent:${oil.id}").assertDoesNotExist()
        compose.onNodeWithTag("homeRecent:${fuel.id}").assertDoesNotExist()
        compose.onNodeWithTag("homeDue:$dueId").assertDoesNotExist()
        compose.onNodeWithTag("homeOpenDashboard").performScrollTo().performClick()
        awaitTag("dashboardTotal")
        compose.onNodeWithTag("dashboardVehicle").assertTextContains(otherVehicle.displayLabel)
        compose.onNodeWithTag("dashboardMonth").assertTextContains("فروردین ۱۴۰۵")
        compose.onNodeWithTag("dashboardTotal").assertTextContains("۰ تومان")
    }

    @Test
    fun mandatoryOfflineJourneysRecoverRecordsTotalsAndRemindersAfterReopen() {
        val dbFile = File(context.cacheDir, "offline-journey-${UUID.randomUUID()}.db")
        val oilId = UUID.randomUUID()
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

        fun awaitState(description: String, predicate: () -> Boolean) {
            compose.waitUntil(description, 5_000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                predicate()
            }
        }

        fun awaitTag(tag: String) = awaitState(tag) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }

        fun scrollToReminderTag(tag: String) {
            compose.onNodeWithTag("remindersScreen").performScrollToNode(hasTestTag(tag))
        }

        fun openReminders() {
            awaitTag("dockTab:reminders")
            compose.onNodeWithTag("dockTab:reminders").performClick()
            awaitTag("remindersScreen")
        }

        fun openCreateForm(dueCount: Int) {
            awaitState("$dueCount due reminders") { reminderVm.state.value.list?.due?.size == dueCount }
            compose.onNodeWithTag("reminderFab").performClick()
            awaitTag("reminderTitle")
            compose.onNodeWithTag("reminderTitle").assertIsDisplayed().assertIsFocused()
        }

        fun enterReminderField(tag: String, value: String) {
            scrollToReminderTag(tag)
            compose.onNodeWithTag(tag).performTextReplacement(value)
        }

        fun saveReminder() {
            scrollToReminderTag("reminderSave")
            compose.onNodeWithTag("reminderSave").performClick()
        }

        val total = "۱٬۰۰۰٬۰۰۰ تومان"
        val futureTitle = "سرویس دوره‌ای بعدی"
        val dueTitle = "تعویض فیلتر"
        val extraId = UUID.randomUUID()
        fun verifyHomeTotal() {
            awaitState("Home total $total") {
                compose.onAllNodesWithText(total).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("homeMonthTotal", useUnmergedTree = true).assertTextContains(total)
        }

        try {
            database = openDb()
            runBlocking {
                database.vehicleDao().insert(vehicle)
                assertEquals(
                    EntrySaveResult.Saved,
                    LedgerRepository(database).create(
                        oilId, 1_774_087_000_000L,
                        EntryInput(vehicle.id, "oil_change", "2026-03-21", 8_500_000L),
                        kilometers = 56_000L,
                    ),
                )
            }
            mount("run1", database)

            awaitTag("entryFab")
            compose.onNodeWithTag("entryFab").performClick()
            compose.onNodeWithTag("selectorOption:fuel").performClick()
            awaitTag("ledgerAmount")
            compose.onNodeWithTag("ledgerAmount").performScrollTo().performTextReplacement("۱۵۰٬۰۰۰")
            compose.onNodeWithTag("ledgerVolume").performScrollTo().performTextReplacement("۳۰")
            compose.onNodeWithTag("ledgerSave").assertIsDisplayed().performClick()
            awaitState("two saved ledger entries") {
                ledgerVm.state.value.draft == null && ledgerVm.state.value.entries?.size == 2
            }
            compose.activity.runOnUiThread { ledgerVm.back() }
            verifyHomeTotal()

            openReminders()
            openCreateForm(dueCount = 0)
            enterReminderField("reminderTitle", futureTitle)
            enterReminderField("reminderDate", "۱۴۰۵/۰۲/۰۱")
            saveReminder()
            awaitState("future reminder saved") {
                reminderVm.state.value.list?.rows?.singleOrNull()?.title == futureTitle
            }
            compose.runOnIdle { assertTrue(reminderVm.state.value.list?.due.orEmpty().isEmpty()) }

            openCreateForm(dueCount = 0)
            enterReminderField("reminderTitle", dueTitle)
            scrollToReminderTag("reminderMileageType")
            compose.onNodeWithTag("reminderMileageType").performClick()
            enterReminderField("reminderMileage", "۵۶۰۰۰")
            saveReminder()
            awaitState("mileage reminder due at equality") {
                val list = reminderVm.state.value.list
                list?.mileage == 56_000L && list.due.singleOrNull()?.title == dueTitle
            }
            val dueReminder = checkNotNull(reminderVm.state.value.list).due.single()
            assertEquals(56_000L, dueReminder.dueKilometers)
            val dueTag = "reminder:${dueReminder.id}"
            scrollToReminderTag("reminderDueSection")
            compose.onNodeWithTag("reminderDueSection").assertIsDisplayed()
            scrollToReminderTag(dueTag)
            compose.onNodeWithText(dueTitle).assertIsDisplayed()
            compose.onNodeWithText(context.getString(R.string.reminder_status_due)).assertIsDisplayed()
            openCreateForm(dueCount = 1)

            // A second due fixture exercises the shortcut with a longer due section.
            runBlocking { reminderRepo.create(vehicle.id, extraId, ReminderInput("یادآور آزمایشی", dueOn = "2026-03-21")) }
            openCreateForm(dueCount = 2)
            runBlocking { reminderRepo.cancel(vehicle.id, extraId) }
            awaitState("only the mileage reminder is due") { reminderVm.state.value.list?.due?.singleOrNull()?.id == dueReminder.id }

            compose.activity.runOnUiThread { reminderVm.back() }
            awaitTag("homeDue:${dueReminder.id}")
            openReminders()
            val completeTag = "reminderComplete:${dueReminder.id}"
            scrollToReminderTag(completeTag)
            compose.onNodeWithText("انجام شد").assertIsDisplayed()
            compose.onNodeWithTag(completeTag).performClick()
            awaitState("mileage reminder completed") {
                val list = reminderVm.state.value.list
                list?.due?.isEmpty() == true && list.rows.any { it.id == dueReminder.id && it.state == "completed" }
            }
            compose.onNodeWithTag("reminderDueSection").assertDoesNotExist()
            scrollToReminderTag(dueTag)
            compose.onNodeWithText(context.getString(R.string.reminder_status_completed)).assertIsDisplayed()
            compose.activity.runOnUiThread { reminderVm.back() }
            verifyHomeTotal()
            compose.onNodeWithTag("homeDue:${dueReminder.id}").assertDoesNotExist()

            database.close()
            database = openDb()
            mount("run2", database)
            verifyHomeTotal()
            compose.onNodeWithTag("homeDue:${dueReminder.id}").assertDoesNotExist()
            compose.onNodeWithTag("homeOpenDashboard").performScrollTo().performClick()
            awaitTag("dashboardTotal")
            compose.onNodeWithTag("dashboardTotal").assertTextContains(total)
            compose.activity.runOnUiThread {
                ledgerVm.back()
                ledgerVm.back()
            }
            openReminders()
            awaitState("all reminder states reloaded") { reminderVm.state.value.list?.rows?.size == 3 }
            val list = checkNotNull(reminderVm.state.value.list)
            assertTrue(list.due.isEmpty())
            assertEquals("completed", list.rows.single { it.id == dueReminder.id }.state)
            val future = list.rows.single { it.title == futureTitle }
            assertEquals("active", future.state)
            assertEquals("2026-04-21", future.dueOn)
            scrollToReminderTag("reminder:${future.id}")
            compose.onNodeWithText(futureTitle).assertIsDisplayed()
        } finally {
            if (::database.isInitialized) database.close()
            dbFile.delete()
            File(dbFile.absolutePath + "-wal").delete()
            File(dbFile.absolutePath + "-shm").delete()
        }
    }

    @Test
    fun navigateToToolsTabOpenTripCalculatorAndReturn() {
        mountHost()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("dockTab:tools").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("dockTab:tools").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("toolsHubScreen").assertIsDisplayed()
        compose.onNodeWithTag("toolsCard:tripCalculator").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("tripCalculatorScreen").assertIsDisplayed()
        compose.onNodeWithTag("tripBackButton").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("toolsHubScreen").assertIsDisplayed()
        compose.onNodeWithTag("tripCalculatorScreen").assertDoesNotExist()

        compose.onNodeWithTag("dockTab:home").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("homeScreen").assertIsDisplayed()
    }
}
