package org.unishiasy.vehiclelog.ui

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
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
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
import org.unishiasy.vehiclelog.data.ReminderWriteResult
import org.unishiasy.vehiclelog.data.VehicleRepository
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.data.local.reminderConstraints
import org.unishiasy.vehiclelog.reminders.ReminderDeliveryStore
import org.unishiasy.vehiclelog.ui.home.HomeViewModel
import org.unishiasy.vehiclelog.ui.ledger.LedgerHost
import org.unishiasy.vehiclelog.ui.ledger.LedgerViewModel
import org.unishiasy.vehiclelog.ui.reminders.ReminderThreshold
import org.unishiasy.vehiclelog.ui.reminders.RemindersViewModel
import org.unishiasy.vehiclelog.ui.theme.VehicleLogTheme
import org.unishiasy.vehiclelog.ui.vehicle.VehicleViewModel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReminderEditAndRecurringRobolectricTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val clock = Clock.fixed(Instant.parse("2026-03-21T10:00:00Z"), ZoneOffset.UTC)
    private val vehicle = VehicleEntity("rem-edit-v1", "car", "رانا پلاس")
    private lateinit var database: VehicleDatabase
    private lateinit var reminderRepo: ReminderRepository
    private lateinit var ledgerRepo: LedgerRepository
    private lateinit var reminderVm: RemindersViewModel

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    private fun mountHost(seed: suspend (LedgerRepository, ReminderRepository) -> Unit = { _, _ -> }) {
        database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(reminderConstraints)
            .build()
        val vehicleRepo = VehicleRepository(database.vehicleDao())
        ledgerRepo = LedgerRepository(database)
        reminderRepo = ReminderRepository(database, ledgerRepo, clock)
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
            val homeVm = provider["remedit-home", HomeViewModel::class.java]
            val ledgerVm = provider["remedit-ledger", LedgerViewModel::class.java]
            val vehicleVm = provider["remedit-vehicles", VehicleViewModel::class.java]
            reminderVm = provider["remedit-reminders", RemindersViewModel::class.java]
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
    fun reminderRepositoryUpdateModifiesActiveOnlyAndDeliveryStoreClearsMarker() = runBlocking<Unit> {
        val db = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(reminderConstraints)
            .build()
        try {
            db.vehicleDao().insert(vehicle)
            val deliveryDir = File(context.cacheDir, "delivery-test-${UUID.randomUUID()}").apply { mkdirs() }
            val store = ReminderDeliveryStore(deliveryDir)
            val ledger = LedgerRepository(db)
            val activeId = UUID.randomUUID()
            store.markDelivered(activeId)
            assertTrue(store.wasDelivered(activeId))

            val repo = ReminderRepository(
                db,
                ledger,
                clock,
                updateWithCleanup = { id, update ->
                    val changed = update()
                    if (changed == 1) store.clear(id)
                    changed
                },
            )
            assertEquals(ReminderWriteResult.Saved, repo.create(vehicle.id, activeId, ReminderInput("بیمه شخص ثالث", dueOn = "2026-04-01")))
            assertEquals(ReminderWriteResult.Saved, repo.update(vehicle.id, activeId, ReminderInput("بیمه بدنه", dueOn = "2026-05-01")))
            assertFalse(store.wasDelivered(activeId))

            val updatedRow = repo.observeForVehicle(vehicle.id).first().single { it.id == activeId.toString() }
            assertEquals("بیمه بدنه", updatedRow.title)
            assertEquals("2026-05-01", updatedRow.dueOn)

            assertEquals(ReminderWriteResult.Saved, repo.complete(vehicle.id, activeId))
            assertEquals(
                ReminderWriteResult.NoActiveReminder,
                repo.update(vehicle.id, activeId, ReminderInput("تغییر غیرمجاز", dueOn = "2026-06-01")),
            )
            deliveryDir.deleteRecursively()
        } finally {
            db.close()
        }
    }

    @Test
    fun editActiveReminderAndScheduleRecurringNextAfterCompletion() {
        val reminderId = UUID.randomUUID()
        mountHost { ledger, reminders ->
            ledger.createStandaloneReading(UUID.randomUUID(), OdometerInput(vehicle.id, "2026-03-20", 12_000L))
            reminders.create(vehicle.id, reminderId, ReminderInput("تعویض روغن", dueKilometers = 15_000L))
        }

        compose.waitUntil(5_000) {
            ShadowLooper.idleMainLooper()
            compose.onAllNodesWithTag("homeOpenReminders").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("homeOpenReminders").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            ShadowLooper.idleMainLooper()
            reminderVm.state.value.list?.rows?.isNotEmpty() == true
        }

        // Edit active reminder
        compose.onNodeWithTag("remindersScreen").performScrollToNode(hasTestTag("reminderEdit:$reminderId"))
        compose.onNodeWithTag("reminderEdit:$reminderId").performClick()
        compose.waitUntil(5_000) { reminderVm.state.value.draft.editing }

        compose.onNodeWithTag("reminderTitle").performTextReplacement("تعویض روغن موتور")
        compose.onNodeWithTag("reminderMileage").performTextReplacement("۱۶۰۰۰")
        compose.onNodeWithTag("reminderSave").performScrollTo().performClick()

        compose.waitUntil(5_000) {
            ShadowLooper.idleMainLooper()
            compose.onAllNodesWithText(context.getString(R.string.reminder_updated)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("remindersScreen").performScrollToNode(hasTestTag("reminder:$reminderId"))
        compose.onNodeWithText("تعویض روغن موتور").assertIsDisplayed()

        // Complete reminder -> recurring next-interval banner -> prefill +5,000 km over current mileage (12,000 -> 17,000)
        compose.onNodeWithTag("remindersScreen").performScrollToNode(hasTestTag("reminderComplete:$reminderId"))
        compose.onNodeWithTag("reminderComplete:$reminderId").performClick()
        compose.waitUntil(5_000) {
            ShadowLooper.idleMainLooper()
            compose.onAllNodesWithTag("reminderRecurringBanner").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("reminderRecurringNext").performClick()
        compose.waitUntil(5_000) {
            reminderVm.state.value.lastCompletedForRecurring == null &&
                reminderVm.state.value.draft.title == "تعویض روغن موتور"
        }
        assertFalse(reminderVm.state.value.draft.editing)
        assertEquals(ReminderThreshold.Mileage, reminderVm.state.value.draft.threshold)
        assertEquals(17_000L, reminderVm.state.value.draft.input()?.dueKilometers)
    }
}
