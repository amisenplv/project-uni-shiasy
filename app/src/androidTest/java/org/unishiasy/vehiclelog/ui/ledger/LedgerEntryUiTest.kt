package org.unishiasy.vehiclelog.ui.ledger

import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.unishiasy.vehiclelog.MainActivity
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.EntryInput
import org.unishiasy.vehiclelog.data.EntrySaveResult
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.OdometerInput
import org.unishiasy.vehiclelog.data.VehicleRepository
import org.unishiasy.vehiclelog.data.local.EntryEntity
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.ui.theme.VehicleLogTheme
import org.unishiasy.vehiclelog.ui.vehicle.VehicleViewModel

@RunWith(AndroidJUnit4::class)
class LedgerEntryUiTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val vehicle = VehicleEntity("ledger-test-a", "car", "سمند LX")
    private val other = vehicle.copy(id = "ledger-test-b", displayLabel = "Toyota کمری")
    private val instant = Instant.parse("2026-03-20T20:30:00Z")
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)
    private lateinit var database: VehicleDatabase
    private lateinit var repository: LedgerRepository
    private lateinit var ledger: LedgerViewModel
    private var activity: MainActivity? = null

    @After
    fun closeStorage() {
        instrumentation.runOnMainSync {
            activity?.viewModelStore?.clear()
            activity?.finish()
        }
        if (::database.isInitialized) database.close()
    }

    @Test
    fun dashboardDefaultsToEmptyTehranMonthForSelectedVehicleAndSwitchesMonths() {
        prepare()
        saveDashboardEntry(vehicle, "2026-03-20", "repair", 120001)
        saveDashboardEntry(other, "2026-03-21", "repair", 500)
        openDashboard()

        compose.onNodeWithTag("dashboardVehicle").assertTextContains(vehicle.displayLabel).assertIsDisplayed()
        compose.onNodeWithText(other.displayLabel).assertDoesNotExist()
        compose.onNodeWithTag("dashboardMonth").assertTextContains("فروردین ۱۴۰۵")
        assertDashboardTotal("۰ تومان")
        dashboardItem("dashboardEntryCount").assertTextContains("تعداد ثبت‌ها: ۰")
        compose.onNodeWithText(context.getString(R.string.dashboard_empty)).assertExists()

        selectReportMonth("1404-12")
        compose.onNodeWithTag("dashboardMonth").assertTextContains("اسفند ۱۴۰۴")
        assertDashboardTotal("۱۲٬۰۰۰٫۱ تومان")
        dashboardItem("dashboardEntryCount").assertTextContains("تعداد ثبت‌ها: ۱")
        selectReportMonth("1405-1")
        assertDashboardTotal("۰ تومان")
        compose.onNodeWithTag("monthlyDashboard").performScrollToIndex(0)
        compose.onNodeWithContentDescription(context.getString(R.string.dashboard_back)).performClick()
        compose.onNodeWithText(context.getString(R.string.ledger_add)).assertIsDisplayed()
        compose.onNodeWithTag("ledgerHistoryList").assertIsDisplayed()
    }

    @Test
    fun dashboardDisplaysExactWideTotalSubtotalsAndBothUnassignedReasons() {
        prepare(fontScale = 1.5f)
        saveDashboardEntry(vehicle, "2026-03-21", "fuel", Long.MAX_VALUE)
        saveDashboardEntry(vehicle, "2026-03-22", "repair", Long.MAX_VALUE)
        saveDashboardEntry(vehicle, "2026-03-23", "parts", null)
        val outside = saveDashboardEntry(vehicle, "2120-03-21", "repair", 400)
        val malformed = UUID.randomUUID()
        runBlocking {
            database.ledgerDao().insert(
                EntryEntity(malformed.toString(), vehicle.id, "repair", "2026-02-30", 1, null, 500, null),
            )
        }
        openDashboard()
        assertDashboardTotal("۱٬۸۴۴٬۶۷۴٬۴۰۷٬۳۷۰٬۹۵۵٬۱۶۱٫۴ تومان")
        dashboardItem("dashboardEntryCount").assertTextContains("تعداد ثبت‌ها: ۳")
        dashboardItem("dashboardUnpricedCount").assertTextContains("ثبت‌های بدون مبلغ: ۱")
        captureDashboard("m34b-wide-total.png")

        val subtotal = "۹۲۲٬۳۳۷٬۲۰۳٬۶۸۵٬۴۷۷٬۵۸۰٫۷ تومان"
        dashboardItem("dashboardCategory:fuel").assertTextContains(subtotal)
        dashboardItem("dashboardCategory:repair").assertTextContains(subtotal)
        dashboardItem("dashboardCategory:parts").assertTextContains("۰ تومان")
        dashboardItem("dashboardUnassigned:$outside")
        compose.onNodeWithText("تاریخ میلادی ذخیره‌شده: \u20662120-03-21\u2069").assertExists()
        compose.onNodeWithText(context.getString(R.string.dashboard_date_outside_range)).assertExists()
        dashboardItem("dashboardUnassigned:$malformed")
        compose.onNodeWithText("تاریخ میلادی ذخیره‌شده: \u20662026-02-30\u2069").assertExists()
        compose.onNodeWithText(context.getString(R.string.dashboard_date_malformed)).assertExists()
        assertEquals("2120-03-21", runBlocking { repository.findById(outside)?.occurredOn })
        assertEquals("2026-02-30", runBlocking { repository.findById(malformed)?.occurredOn })
    }

    @Test
    fun dashboardKeepsSelectedMonthAcrossReactiveUpdateAndActivityRecreation() {
        prepare()
        saveDashboardEntry(vehicle, "2026-03-20", "parts", null)
        openDashboard()
        selectReportMonth("1404-12")
        assertDashboardTotal("۰ تومان")
        dashboardItem("dashboardEntryCount").assertTextContains("تعداد ثبت‌ها: ۱")
        dashboardItem("dashboardUnpricedCount").assertTextContains("ثبت‌های بدون مبلغ: ۱")
        dashboardItem("dashboardCategory:parts").assertTextContains("۰ تومان")
        compose.onNodeWithText(context.getString(R.string.dashboard_empty)).assertDoesNotExist()

        saveDashboardEntry(vehicle, "2026-03-20", "repair", 111)
        waitFor("۱۱٫۱ تومان")
        assertDashboardTotal("۱۱٫۱ تومان")
        dashboardItem("dashboardEntryCount").assertTextContains("تعداد ثبت‌ها: ۲")
        val previous = checkNotNull(activity)
        instrumentation.runOnMainSync { previous.recreate() }
        compose.waitUntil(5_000) { resumedActivity()?.let { it !== previous } == true }
        activity = resumedActivity()
        attach(fontScale = null)
        dashboardItem("dashboardMonth").assertTextContains("اسفند ۱۴۰۴")
        assertDashboardTotal("۱۱٫۱ تومان")
    }

    private fun saveDashboardEntry(target: VehicleEntity, date: String, category: String, rials: Long?): UUID {
        val id = UUID.randomUUID()
        assertEquals(EntrySaveResult.Saved, runBlocking {
            repository.create(
                id, instant.toEpochMilli(),
                EntryInput(target.id, category, date, rials, if (category == "fuel") "1" else null),
            )
        })
        return id
    }

    private fun openDashboard() {
        openVehicle()
        compose.onNodeWithTag("ledgerOpenDashboard").performClick()
        compose.waitUntil(5_000) { ledger.state.value.dashboard?.report != null }
    }

    private fun dashboardItem(tag: String) = compose.onNodeWithTag("monthlyDashboard")
        .performScrollToNode(hasTestTag(tag)).let { compose.onNodeWithTag(tag).assertIsDisplayed() }

    private fun selectReportMonth(tag: String) {
        dashboardItem("dashboardMonth").performClick()
        compose.onNodeWithTag("dashboardMonth:$tag").assertIsDisplayed().performClick()
    }

    private fun assertDashboardTotal(expected: String) {
        dashboardItem("dashboardTotal").assertTextContains(expected)
    }

    private fun captureDashboard(name: String) {
        compose.waitForIdle()
        val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(context.filesDir, name).outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            screenshot.recycle()
        }
    }

    @Test
    fun createsFuelForSelectedVehicleWithExactValuesAndOneCommit() {
        prepare()
        runBlocking {
            assertEquals(EntrySaveResult.Saved, repository.createStandaloneReading(
                UUID.randomUUID(), OdometerInput(vehicle.id, "2026-03-20", 1000),
            ))
        }
        openNew()
        compose.onNodeWithText(vehicle.displayLabel).assertIsDisplayed()
        field("Date").assertTextContains("۱۴۰۵/۰۱/۰۱")
        compose.onNodeWithTag("ledgerDetailsToggle").performScrollTo().performClick()
        field("Mileage").performScrollTo().assertTextContains("")
        compose.onNodeWithText("آخرین کارکرد ثبت‌شده", substring = true).assertExists()
        enterFuel()
        val draft = ledger.state.value.draft!!
        compose.runOnIdle {
            ledger.save()
            ledger.save()
        }
        await(R.string.ledger_saved)
        val saved = savedEntry()
        assertEquals(draft.id.toString(), saved.entry.id)
        assertEquals(instant.toEpochMilli(), saved.entry.createdAtEpochMillis)
        assertEquals(vehicle.id, saved.entry.vehicleId)
        assertEquals("2026-03-21", saved.entry.occurredOn)
        assertEquals(120001L, saved.entry.amountRials)
        assertEquals(32450L, saved.entry.fuelVolumeMl)
        assertEquals(1200L, saved.reading?.kilometers)
        assertTrue(runBlocking { repository.observeForVehicle(other.id).first().isEmpty() })
    }

    @Test
    fun retainsDraftAndErrorsAcrossActivityRecreationWithEnlargedText() {
        prepare(fontScale = 1.5f)
        openNew()
        replace("Amount", "۱۲٫۰۰")
        replace("Date", "۱۴۰۴/۱۲/۳۰")
        compose.onNodeWithTag("ledgerDetailsToggle").performScrollTo().performClick()
        replace("Notes", "پیش‌نویس LX")
        field("Notes").performScrollTo().performTouchInput { click() }
        compose.waitUntil(5_000) { imeVisible() }
        val save = compose.onNodeWithTag("ledgerSave").assertIsDisplayed()
        assertTrue(imeVisible())
        save.performTouchInput { click() }
        await(R.string.ledger_check_fields)
        val before = ledger.state.value
        val previous = checkNotNull(activity)
        instrumentation.runOnMainSync { previous.recreate() }
        compose.waitUntil(5_000) {
            resumedActivity()?.let { it !== previous } == true
        }
        activity = resumedActivity()
        attach(fontScale = 1.5f)
        assertEquals(before.draft, ledger.state.value.draft)
        assertEquals(before.fieldErrors, ledger.state.value.fieldErrors)
        field("Amount").performScrollTo().assertTextContains("۱۲٫۰۰")
        await(R.string.ledger_amount_invalid)
        field("Date").performScrollTo().assertTextContains("۱۴۰۴/۱۲/۳۰")
        await(R.string.ledger_date_invalid)
        field("Notes").performScrollTo().assertTextContains("پیش‌نویس LX")
        compose.onNodeWithTag("ledgerSave").assertIsDisplayed()
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        if (screenshot != null) {
            File(context.filesDir, "m33a-enlarged.png").outputStream().use {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            screenshot.recycle()
        }
        assertTrue(runBlocking { repository.observeForVehicle(vehicle.id).first().isEmpty() })
    }

    @Test
    fun editsFromRecentAndKeepsSetsAndExplicitlyRemovesLinkedReading() {
        prepare()
        val id = UUID.randomUUID()
        runBlocking {
            assertEquals(EntrySaveResult.Saved, repository.create(
                id, 123L, EntryInput(vehicle.id, "parts", "2120-03-21"), 1100,
            ))
        }
        openVehicle()
        openRecent(id)
        compose.onNodeWithText("میلادی", substring = true).assertIsDisplayed()
        field("Volume").assertDoesNotExist()
        val original = savedEntry()
        replace("Notes", "قطعهٔ جدید")
        replace("Mileage", "")
        save()
        assertEquals(original.reading, savedEntry().reading)
        assertEquals("2120-03-21", savedEntry().entry.occurredOn)
        assertNull(savedEntry().entry.amountRials)

        openRecent(id)
        replace("Date", "۱۴۰۵/۰۱/۰۱")
        replace("Mileage", "۱۲۳۰")
        save()
        val edited = savedEntry()
        assertEquals(id.toString(), edited.entry.id)
        assertEquals(123L, edited.entry.createdAtEpochMillis)
        assertEquals("2026-03-21", edited.entry.occurredOn)
        assertEquals(original.reading?.id, edited.reading?.id)
        assertEquals(1230L, edited.reading?.kilometers)

        openRecent(id)
        compose.onNodeWithTag("ledgerCategory").performScrollTo().performClick()
        compose.onNodeWithTag("ledgerCategory:oil_change").performClick()
        click(R.string.ledger_remove_mileage)
        save()
        assertNull(savedEntry().reading)
        assertEquals(edited.entry.copy(category = "oil_change"), savedEntry().entry)
    }

    @Test
    fun storageFailureKeepsDraftAndRetryUsesSameIdentity() {
        prepare()
        execSql("""
            CREATE TRIGGER fail_entry_insert BEFORE INSERT ON entries
            BEGIN INSERT INTO absent_for_test VALUES (1); END
        """.trimIndent())
        openNew()
        enterFuel()
        val draft = ledger.state.value.draft!!
        compose.onNodeWithTag("ledgerSave").assertIsDisplayed().performClick()
        await(R.string.ledger_storage_failure)
        assertEquals(draft, ledger.state.value.draft)
        assertTrue(runBlocking { repository.observeForVehicle(vehicle.id).first().isEmpty() })
        execSql("DROP TRIGGER fail_entry_insert")
        save()
        assertEquals(draft.id.toString(), savedEntry().entry.id)
        assertEquals(draft.createdAt, savedEntry().entry.createdAtEpochMillis)
    }

    @Test
    fun mileageConflictAllowsExplicitSaveWithoutReading() {
        prepare()
        runBlocking {
            assertEquals(EntrySaveResult.Saved, repository.createStandaloneReading(
                UUID.randomUUID(), OdometerInput(vehicle.id, "2026-03-22", 1000),
            ))
        }
        openNew()
        enterFuel()
        val draft = ledger.state.value.draft!!
        compose.onNodeWithTag("ledgerSave").assertIsDisplayed().performClick()
        await(R.string.ledger_mileage_conflict_action)
        assertEquals(draft, ledger.state.value.draft)
        assertTrue(runBlocking { repository.observeForVehicle(vehicle.id).first().isEmpty() })
        click(R.string.ledger_save_without_mileage)
        await(R.string.ledger_saved)
        assertNull(savedEntry().reading)
        assertEquals(draft.id.toString(), savedEntry().entry.id)
    }

    @Test
    fun displaysCompleteHistoryBeyondFiveEntriesInDeterministicOrder() {
        prepare()
        val entryIds = (1..7).map { UUID.fromString("8d44fed9-6136-4e3b-a1a5-f2a58fe5000$it") }
        runBlocking {
            entryIds.forEachIndexed { index, uuid ->
                val day = if (index < 2) "2026-11-10" else "2026-11-0${9 - index + 2}"
                val amount = if (index == 2) null else (index + 1) * 100_000L
                val category = if (index == 0) "fuel" else if (index == 2) "repair" else "wash"
                val volume = if (index == 0) "30.000" else null
                val km = if (index == 0) 1200L else null
                repository.create(
                    uuid,
                    1_794_268_800_000L - index * 1000L,
                    EntryInput(vehicle.id, category, day, amount, volume, "یادداشت $index"),
                    km,
                )
            }
        }
        openVehicle()
        compose.waitUntil(5_000) { ledger.state.value.entries?.size == 7 }
        compose.onNodeWithText(context.getString(R.string.ledger_history_title)).assertIsDisplayed()
        entryIds.forEachIndexed { index, id ->
            compose.onNodeWithTag("ledgerHistoryList").performScrollToIndex(index + 1)
            compose.onNodeWithTag("ledgerRecent:$id").assertIsDisplayed()
        }
        compose.onNodeWithTag("ledgerHistoryList").performScrollToIndex(1)
        compose.onNodeWithText("۳۰", substring = true).assertIsDisplayed()
        compose.onNodeWithText("لیتر", substring = true).assertIsDisplayed()
        compose.onNodeWithText("۱۲۰۰", substring = true).assertIsDisplayed()
        compose.onNodeWithText("کیلومتر", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("ledgerHistoryList").performScrollToIndex(3)
        compose.onNodeWithText("مبلغ ثبت نشده").assertIsDisplayed()
    }

    @Test
    fun deletesEntryFromHistoryWithConfirmationAndCascadesLinkedReading() {
        prepare()
        val entryId = UUID.randomUUID()
        val standaloneId = UUID.randomUUID()
        runBlocking {
            repository.create(
                entryId,
                1_794_268_800_000L,
                EntryInput(vehicle.id, "fuel", "2026-11-10", 120000L, "30.000"),
                1500L,
            )
            repository.createStandaloneReading(
                standaloneId,
                OdometerInput(vehicle.id, "2026-11-08", 1000L),
            )
        }
        openVehicle()
        compose.waitUntil(5_000) { ledger.state.value.entries?.any { it.id == entryId.toString() } == true }
        compose.onNodeWithTag("ledgerHistoryList").performScrollToIndex(1)
        compose.onNodeWithTag("ledgerDelete:$entryId").performClick()

        compose.onNodeWithText(context.getString(R.string.ledger_delete_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.ledger_delete_confirm)).assertIsDisplayed()

        compose.onNodeWithTag("ledgerCancelDelete").performClick()
        compose.onNodeWithTag("ledgerDelete:$entryId").assertIsDisplayed()
        assertTrue(runBlocking { repository.findById(entryId) != null })

        compose.onNodeWithTag("ledgerDelete:$entryId").performClick()
        compose.onNodeWithTag("ledgerConfirmDelete").performClick()

        await(R.string.ledger_deleted)
        compose.waitUntil(5_000) { ledger.state.value.entries?.any { it.id == entryId.toString() } == false }
        compose.onNodeWithTag("ledgerDelete:$entryId").assertDoesNotExist()

        runBlocking {
            assertNull(repository.findById(entryId))
            assertNull(repository.findWithReading(entryId))
            assertNotNull(repository.findReadingById(standaloneId))
            assertNotNull(database.vehicleDao().findById(vehicle.id))
            assertEquals(1000L, repository.observeCurrentMileage(vehicle.id).first())
        }
    }

    @Test
    fun deletesEntryFromEditFormWithConfirmation() {
        prepare()
        val entryId = UUID.randomUUID()
        runBlocking {
            repository.create(
                entryId,
                1_794_268_800_000L,
                EntryInput(vehicle.id, "repair", "2026-11-10", 50000L),
                null,
            )
        }
        openVehicle()
        openRecent(entryId)

        compose.onNodeWithTag("ledgerDeleteEntry").performScrollTo().performClick()
        compose.onNodeWithTag("ledgerConfirmDelete").assertIsDisplayed().performClick()

        await(R.string.ledger_deleted)
        compose.waitUntil(5_000) { ledger.state.value.entries?.any { it.id == entryId.toString() } == false }
        compose.onNodeWithTag("ledgerDelete:$entryId").assertDoesNotExist()
        assertTrue(runBlocking { repository.findById(entryId) == null })
    }

    @Test
    fun m35CompleteEndToEndAcceptanceJourney() {
        prepare(seed = false)
        waitFor(context.getString(R.string.vehicle_empty))
        compose.onNodeWithText(context.getString(R.string.vehicle_add)).performClick()
        compose.onNodeWithTag("vehicleName").performScrollTo().performTextInput("پژو ۲۰۶ SD")
        compose.onNodeWithTag("vehicleYear").performScrollTo().performTextInput("۱۴۰۰")
        compose.onNodeWithText(context.getString(R.string.vehicle_solar_hijri)).performClick()
        compose.onNodeWithText(context.getString(R.string.vehicle_save)).performClick()
        await(R.string.vehicle_saved)
        val createdVehicle = runBlocking {
            database.vehicleDao().observeAll().first().single { it.displayLabel == "پژو ۲۰۶ SD" }
        }

        val standaloneId = UUID.randomUUID()
        runBlocking {
            assertEquals(
                EntrySaveResult.Saved,
                repository.createStandaloneReading(standaloneId, OdometerInput(createdVehicle.id, "2026-03-20", 1000L)),
            )
        }

        val openLabel = context.getString(R.string.ledger_open, "\u2068${createdVehicle.displayLabel}\u2069")
        compose.onNodeWithText(openLabel).performScrollTo().performClick()
        compose.onNodeWithTag("ledgerHistoryList").assertIsDisplayed()

        compose.onNodeWithText(context.getString(R.string.ledger_add)).performClick()
        replace("Amount", "١٢٬۰۰۰٫۱")
        replace("Volume", "۳٢٫۴۵۰")
        compose.onNodeWithTag("ledgerDetailsToggle").performScrollTo().performClick()
        replace("Mileage", "۱۲۰۰")
        compose.onNodeWithTag("ledgerSave").assertIsDisplayed().performClick()
        await(R.string.ledger_saved)

        val fuelEntry = runBlocking {
            val list = repository.observeForVehicle(createdVehicle.id).first()
            assertEquals(1, list.size)
            repository.findWithReading(UUID.fromString(list.single().id))!!
        }
        assertEquals(120001L, fuelEntry.entry.amountRials)
        assertEquals(32450L, fuelEntry.entry.fuelVolumeMl)
        assertEquals("2026-03-21", fuelEntry.entry.occurredOn)
        assertEquals(1200L, fuelEntry.reading?.kilometers)
        assertEquals(1200L, runBlocking { repository.observeCurrentMileage(createdVehicle.id).first() })

        val fuelId = UUID.fromString(fuelEntry.entry.id)
        openRecent(fuelId)
        replace("Amount", "۱۵٬۰۰۰")
        replace("Mileage", "۱۲۵۰")
        compose.onNodeWithTag("ledgerSave").assertIsDisplayed().performClick()
        await(R.string.ledger_saved)

        val editedFuel = runBlocking { repository.findWithReading(fuelId)!! }
        assertEquals(150000L, editedFuel.entry.amountRials)
        assertEquals(1250L, editedFuel.reading?.kilometers)
        assertEquals(1250L, runBlocking { repository.observeCurrentMileage(createdVehicle.id).first() })

        val extraIds = (1..5).map { UUID.randomUUID() }
        runBlocking {
            extraIds.forEachIndexed { idx, id ->
                repository.create(
                    id, instant.toEpochMilli() - (idx + 1) * 1000L,
                    EntryInput(createdVehicle.id, "repair", "2026-03-21", 100000L * (idx + 1)),
                )
            }
        }
        compose.waitUntil(5_000) { ledger.state.value.entries?.size == 6 }
        extraIds.forEachIndexed { idx, id ->
            compose.onNodeWithTag("ledgerHistoryList").performScrollToIndex(idx + 2)
            compose.onNodeWithTag("ledgerRecent:$id").assertIsDisplayed()
        }

        compose.onNodeWithTag("ledgerHistoryList").performScrollToIndex(0)
        compose.onNodeWithTag("ledgerOpenDashboard").performClick()
        compose.waitUntil(5_000) { ledger.state.value.dashboard?.report != null }
        assertDashboardTotal("۱۶۵٬۰۰۰ تومان")
        dashboardItem("dashboardEntryCount").assertTextContains("تعداد ثبت‌ها: ۶")

        compose.onNodeWithTag("monthlyDashboard").performScrollToIndex(0)
        compose.onNodeWithContentDescription(context.getString(R.string.dashboard_back)).performClick()
        compose.onNodeWithTag("ledgerHistoryList").assertIsDisplayed()

        compose.waitUntil(5_000) { ledger.state.value.entries?.any { it.id == fuelId.toString() } == true }
        compose.onNodeWithTag("ledgerHistoryList").performScrollToIndex(1)
        compose.onNodeWithTag("ledgerDelete:$fuelId").performClick()

        compose.onNodeWithText(context.getString(R.string.ledger_delete_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.ledger_delete_confirm)).assertIsDisplayed()
        compose.onNodeWithTag("ledgerConfirmDelete").performClick()
        await(R.string.ledger_deleted)
        compose.waitUntil(5_000) { ledger.state.value.entries?.any { it.id == fuelId.toString() } == false }

        runBlocking {
            assertNull(repository.findById(fuelId))
            assertNull(repository.findReadingById(UUID.fromString(editedFuel.reading!!.id)))
            assertNotNull(repository.findReadingById(standaloneId))
            assertNotNull(database.vehicleDao().findById(createdVehicle.id))
            assertEquals(1000L, repository.observeCurrentMileage(createdVehicle.id).first())
        }

        compose.onNodeWithTag("ledgerHistoryList").performScrollToIndex(0)
        compose.onNodeWithTag("ledgerOpenDashboard").performClick()
        compose.waitUntil(5_000) { ledger.state.value.dashboard?.report != null }
        assertDashboardTotal("۱۵۰٬۰۰۰ تومان")
        dashboardItem("dashboardEntryCount").assertTextContains("تعداد ثبت‌ها: ۵")
    }

    @Test
    fun preservesHistoryScrollPositionAcrossEntryEdit() {
        prepare(fontScale = 1.5f)
        openVehicle()
        val entryIds = (1..10).map { UUID.randomUUID() }
        runBlocking {
            entryIds.forEachIndexed { idx, id ->
                repository.create(
                    id,
                    instant.toEpochMilli() - idx * 60_000L,
                    EntryInput(vehicle.id, "repair", "2026-03-21", 10_000L * (idx + 1)),
                )
            }
        }
        compose.waitUntil(5_000) { ledger.state.value.entries?.size == 10 }

        val targetId = entryIds[5]
        compose.onNodeWithTag("ledgerHistoryList").performScrollToIndex(6)
        compose.onNodeWithTag("ledgerRecent:$targetId").assertIsDisplayed()

        compose.onNodeWithTag("ledgerRecent:$targetId").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("entryDetailEdit").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("entryDetailEdit").performClick()
        compose.waitUntil(5_000) { ledger.state.value.draft?.id == targetId }
        compose.onNodeWithTag("ledgerSave").assertIsDisplayed()

        compose.onNodeWithTag("ledgerCloseEntry").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { ledger.state.value.draft == null }

        compose.onNodeWithTag("ledgerHistoryList").assertIsDisplayed()
        compose.onNodeWithTag("ledgerRecent:$targetId").assertIsDisplayed()
    }

    @Test
    fun savesAndReopensFileBackedEntryAcrossForceStop() {
        val args = InstrumentationRegistry.getArguments()
        val phase = args.getString("ledgerPersistencePhase")
        val name = args.getString("ledgerPersistenceName")
        assumeTrue((phase == "write" || phase == "read") && name?.startsWith("m33a-offline-") == true)
        val fixtureName = checkNotNull(name)
        prepare(name = fixtureName, seed = phase == "write")
        if (phase == "write") {
            openNew()
            enterFuel()
            save()
        } else {
            openVehicle()
            val saved = savedEntry()
            openRecent(UUID.fromString(saved.entry.id))
            field("Amount").assertTextContains("۱۲٬۰۰۰٫۱")
            field("Date").assertTextContains("۱۴۰۵/۰۱/۰۱")
            field("Volume").assertTextContains("۳۲٫۴۵")
            field("Mileage").performScrollTo().assertTextContains("۱۲۰۰")
        }
        val saved = savedEntry()
        assertEquals(120001L, saved.entry.amountRials)
        assertEquals(32450L, saved.entry.fuelVolumeMl)
        assertEquals("2026-03-21", saved.entry.occurredOn)
        assertEquals(1200L, saved.reading?.kilometers)
    }

    private fun prepare(fontScale: Float? = null, name: String? = null, seed: Boolean = true) {
        database = if (name == null) Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java).build()
        else Room.databaseBuilder(context, VehicleDatabase::class.java, name).build()
        repository = LedgerRepository(database)
        if (seed) runBlocking {
            database.vehicleDao().insert(vehicle)
            database.vehicleDao().insert(other)
        }
        val component = ComponentName(context, MainActivity::class.java)
        // MIUI may block ActivityScenario's background activity launch.
        val command = "am start -W -a ${Intent.ACTION_MAIN} -c ${Intent.CATEGORY_LAUNCHER} -n ${component.flattenToString()}"
        val launch = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command),
        ).bufferedReader().use { it.readText() }
        assertTrue(launch, launch.lineSequence().any { it.trim() == "Status: ok" })
        activity = checkNotNull(resumedActivity())
        attach(fontScale)
    }

    private fun attach(fontScale: Float?) {
        instrumentation.runOnMainSync {
            val activity = checkNotNull(activity)
            val factory = viewModelFactory {
                initializer { LedgerViewModel(repository, clock) }
                initializer { VehicleViewModel(VehicleRepository(database.vehicleDao())) }
            }
            val provider = ViewModelProvider(activity, factory)
            ledger = provider["test-ledger", LedgerViewModel::class.java]
            val vehicles = provider["test-vehicles", VehicleViewModel::class.java]
            activity.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalDensity provides Density(density.density, fontScale ?: density.fontScale),
                ) {
                    VehicleLogTheme { LedgerHost(vehicles, ledger) }
                }
            }
        }
    }

    private fun openVehicle() {
        val label = context.getString(R.string.ledger_open, "\u2068${vehicle.displayLabel}\u2069")
        waitFor(label)
        compose.onNodeWithText(label).performScrollTo().performClick()
    }

    private fun openNew() {
        openVehicle()
        click(R.string.ledger_add)
    }

    private fun openRecent(id: UUID) {
        val tag = "ledgerRecent:$id"
        compose.waitUntil(5_000) { ledger.state.value.recent?.any { it.id == id.toString() } == true }
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("entryDetailEdit").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("entryDetailEdit").performClick()
        compose.waitUntil(5_000) { ledger.state.value.draft?.id == id }
    }

    private fun field(name: String) = compose.onNodeWithTag("ledger$name")

    private fun replace(name: String, value: String) {
        field(name).performScrollTo().performTextReplacement(value)
    }

    private fun enterFuel() {
        replace("Amount", "١٢٬۰۰۰٫۱")
        replace("Volume", "۳٢٫۴۵۰")
        if (compose.onAllNodesWithTag("ledgerMileage").fetchSemanticsNodes().isEmpty()) {
            compose.onNodeWithTag("ledgerDetailsToggle").performScrollTo().performClick()
        }
        replace("Mileage", "۱٢0۰")
    }

    private fun save() {
        compose.onNodeWithTag("ledgerSave").assertIsDisplayed().performClick()
        await(R.string.ledger_saved)
    }

    private fun savedEntry() = runBlocking {
        val entry = repository.observeForVehicle(vehicle.id).first().single()
        checkNotNull(repository.findWithReading(UUID.fromString(entry.id)))
    }

    private fun click(resource: Int) {
        compose.onNodeWithText(context.getString(resource)).performScrollTo().performClick()
    }

    private fun await(resource: Int) = waitFor(context.getString(resource))

    private fun waitFor(text: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun execSql(sql: String) = runBlocking {
        withContext(Dispatchers.IO) { database.openHelper.writableDatabase.execSQL(sql) }
    }

    private fun imeVisible(): Boolean {
        var visible = false
        instrumentation.runOnMainSync {
            val decor = activity?.window?.decorView
            visible = decor?.let(ViewCompat::getRootWindowInsets)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        return visible
    }

    private fun resumedActivity(): MainActivity? {
        var resumed: MainActivity? = null
        instrumentation.runOnMainSync {
            resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>().singleOrNull()
        }
        return resumed
    }
}
