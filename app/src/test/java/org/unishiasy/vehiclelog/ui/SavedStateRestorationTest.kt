package org.unishiasy.vehiclelog.ui

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.unishiasy.vehiclelog.data.EntryInput
import org.unishiasy.vehiclelog.data.EntrySaveResult
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.ReminderRepository
import org.unishiasy.vehiclelog.data.VehicleRepository
import org.unishiasy.vehiclelog.data.backup.BackupRepository
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.data.local.reminderConstraints
import org.unishiasy.vehiclelog.domain.JalaliMonth
import org.unishiasy.vehiclelog.reminders.ReminderNotifications
import org.unishiasy.vehiclelog.ui.ledger.EntryCategory
import org.unishiasy.vehiclelog.ui.ledger.EntryField
import org.unishiasy.vehiclelog.ui.ledger.LedgerViewModel
import org.unishiasy.vehiclelog.ui.reminders.ReminderThreshold
import org.unishiasy.vehiclelog.ui.reminders.RemindersViewModel
import org.unishiasy.vehiclelog.ui.settings.BackupAction
import org.unishiasy.vehiclelog.ui.settings.BackupSettingsViewModel
import org.unishiasy.vehiclelog.ui.settings.CloudSettingsViewModel
import org.unishiasy.vehiclelog.ui.vehicle.VehicleViewModel

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SavedStateRestorationTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val clock = Clock.fixed(Instant.parse("2026-03-21T10:00:00Z"), ZoneOffset.UTC)
    private val vehicle = VehicleEntity("restored-v-1", "car", "دنا پلاس", productionYear = 1401, productionYearCalendar = "solar_hijri")
    private lateinit var database: VehicleDatabase

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(reminderConstraints)
            .build()
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun SavedStateHandle.recreateViaBundle(): SavedStateHandle {
        val bundle: Bundle = savedStateProvider().saveState()
        return SavedStateHandle.createHandle(bundle, null)
    }

    @Test
    fun vehicleViewModelRestoresManagementAndUnsavedDraft() {
        val repo = VehicleRepository(database.vehicleDao())
        val originalHandle = SavedStateHandle()
        val firstVm = VehicleViewModel(repo, savedStateHandle = originalHandle)

        firstVm.openManagement()
        firstVm.addVehicle()
        firstVm.changeInput {
            it.copy(
                displayLabel = "پژو ۲۰۷",
                catalogKey = "peugeot_207",
                productionYear = "۱۴۰۲",
                productionYearCalendar = "solar_hijri",
            )
        }

        val restoredHandle = originalHandle.recreateViaBundle()
        val restoredVm = VehicleViewModel(repo, savedStateHandle = restoredHandle)
        val restored = restoredVm.state.value

        assertTrue(restored.managingList)
        assertNotNull(restored.draft)
        assertNull(restored.draft?.id)
        assertEquals("پژو ۲۰۷", restored.draft?.input?.displayLabel)
        assertEquals("peugeot_207", restored.draft?.input?.catalogKey)
        assertEquals("۱۴۰۲", restored.draft?.input?.productionYear)
        assertEquals("solar_hijri", restored.draft?.input?.productionYearCalendar)
    }

    @Test
    fun ledgerViewModelRestoresVehicleDraftAndResumesRoomObservation() = runBlocking {
        val ledgerRepo = LedgerRepository(database)
        database.vehicleDao().insert(vehicle)
        val existingId = UUID.randomUUID()
        assertEquals(
            EntrySaveResult.Saved,
            ledgerRepo.create(existingId, 1_774_087_200_000L, EntryInput(vehicle.id, "fuel", "2026-03-21", 2_500_000L, "40")),
        )

        val originalHandle = SavedStateHandle()
        val firstVm = LedgerViewModel(ledgerRepo, clock, originalHandle)
        firstVm.openQuickAdd(vehicle, EntryCategory.OilChange)
        firstVm.changeField(EntryField.Amount, "۷۵۰۰۰۰")
        firstVm.changeField(EntryField.Date, "۱۴۰۵/۰۱/۰۱")
        firstVm.changeField(EntryField.Mileage, "۳۴۰۰۰")
        firstVm.changeField(EntryField.Notes, "فیلتر هوا هم عوض شد")

        val expectedDraftId = firstVm.state.value.draft?.id
        val restoredHandle = originalHandle.recreateViaBundle()
        val restoredVm = LedgerViewModel(ledgerRepo, clock, restoredHandle)

        val entries = withTimeout(5_000) { restoredVm.state.first { !it.entries.isNullOrEmpty() } }.entries
        assertEquals(vehicle, restoredVm.state.value.vehicle)
        assertEquals(expectedDraftId, restoredVm.state.value.draft?.id)
        assertEquals(EntryCategory.OilChange, restoredVm.state.value.draft?.category)
        assertEquals("۷۵۰۰۰۰", restoredVm.state.value.draft?.amount)
        assertEquals("۱۴۰۵/۰۱/۰۱", restoredVm.state.value.draft?.date)
        assertEquals("۳۴۰۰۰", restoredVm.state.value.draft?.mileage)
        assertEquals("فیلتر هوا هم عوض شد", restoredVm.state.value.draft?.notes)
        assertEquals(listOf(existingId.toString()), entries?.map { it.id })
    }

    @Test
    fun ledgerViewModelRestoresDashboardSelectedMonthAndReport() = runBlocking {
        val ledgerRepo = LedgerRepository(database)
        database.vehicleDao().insert(vehicle)
        assertEquals(
            EntrySaveResult.Saved,
            ledgerRepo.create(UUID.randomUUID(), 1_774_087_200_000L, EntryInput(vehicle.id, "repair", "2026-03-20", 1_000_000L)),
        )

        val originalHandle = SavedStateHandle()
        val firstVm = LedgerViewModel(ledgerRepo, clock, originalHandle)
        firstVm.openDashboardForVehicle(vehicle)
        withTimeout(5_000) { firstVm.state.first { it.dashboard?.report != null } }
        val targetMonth = JalaliMonth(1404, 12)
        firstVm.selectDashboardMonth(targetMonth)

        val restoredHandle = originalHandle.recreateViaBundle()
        val restoredVm = LedgerViewModel(ledgerRepo, clock, restoredHandle)
        val restoredState = withTimeout(5_000) { restoredVm.state.first { it.dashboard?.report != null } }

        assertEquals(vehicle, restoredState.vehicle)
        assertEquals(targetMonth, restoredState.dashboard?.selectedMonth)
        assertNotNull(restoredState.dashboard?.report)
    }

    @Test
    fun ledgerViewModelRestoresInspectingEntryAndPendingDeletion() {
        val ledgerRepo = LedgerRepository(database)
        val originalHandle = SavedStateHandle()
        val firstVm = LedgerViewModel(ledgerRepo, clock, originalHandle)
        val targetId = UUID.randomUUID().toString()

        firstVm.openVehicle(vehicle)
        firstVm.inspectEntry(targetId)
        firstVm.requestDelete(targetId)

        val restoredHandle = originalHandle.recreateViaBundle()
        val restoredVm = LedgerViewModel(ledgerRepo, clock, restoredHandle)

        assertEquals(vehicle, restoredVm.state.value.vehicle)
        assertEquals(targetId, restoredVm.state.value.inspectingEntryId)
        assertEquals(targetId, restoredVm.state.value.entryPendingDeletion)
    }

    @Test
    fun remindersViewModelRestoresVehicleDraftAndValidation() {
        val ledgerRepo = LedgerRepository(database)
        val reminderRepo = ReminderRepository(database, ledgerRepo, clock)
        val originalHandle = SavedStateHandle()
        val firstVm = RemindersViewModel(reminderRepo, ledgerRepo, clock, originalHandle)

        firstVm.open(vehicle)
        firstVm.edit {
            it.copy(
                title = "تمدید بیمه شخص ثالث",
                threshold = ReminderThreshold.Mileage,
                mileage = "۴۰۰۰۰",
            )
        }
        val expectedDraftId = firstVm.state.value.draft.id

        val restoredHandle = originalHandle.recreateViaBundle()
        val restoredVm = RemindersViewModel(reminderRepo, ledgerRepo, clock, restoredHandle)
        val restored = restoredVm.state.value

        assertEquals(vehicle, restored.vehicle)
        assertEquals(expectedDraftId, restored.draft.id)
        assertEquals("تمدید بیمه شخص ثالث", restored.draft.title)
        assertEquals(ReminderThreshold.Mileage, restored.draft.threshold)
        assertEquals("۴۰۰۰۰", restored.draft.mileage)
        assertFalse(restored.showValidation)
    }

    @Test
    fun backupSettingsViewModelRestoresOpenAndPendingPickerAction() {
        val ledgerRepo = LedgerRepository(database)
        val reminderRepo = ReminderRepository(database, ledgerRepo, clock)
        val notifications = ReminderNotifications(context, reminderRepo::activeDateReminders, clock)
        val backupRepo = BackupRepository(database, notifications, {})
        val originalHandle = SavedStateHandle()
        val firstVm = BackupSettingsViewModel(backupRepo, context.contentResolver, originalHandle)

        firstVm.open()
        assertTrue(firstVm.startPicker(BackupAction.Export))

        val restoredHandle = originalHandle.recreateViaBundle()
        val restoredVm = BackupSettingsViewModel(backupRepo, context.contentResolver, restoredHandle)

        assertTrue(restoredVm.state.value.open)
        assertEquals(BackupAction.Export, restoredVm.state.value.action)
    }

    @Test
    fun cloudSettingsViewModelRemainsExcludedFromSavedStateHandle() {
        val constructors = CloudSettingsViewModel::class.java.declaredConstructors
        assertTrue(
            constructors.none { ctor -> ctor.parameterTypes.any { it == SavedStateHandle::class.java } }
        )
    }
}
