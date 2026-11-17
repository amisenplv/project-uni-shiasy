package org.unishiasy.vehiclelog.ui.audit

import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
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
import java.io.FileOutputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.unishiasy.vehiclelog.MainActivity
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.VehicleLedgerApplication
import org.unishiasy.vehiclelog.data.EntryInput
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.OdometerInput
import org.unishiasy.vehiclelog.data.ReminderInput
import org.unishiasy.vehiclelog.data.ReminderRepository
import org.unishiasy.vehiclelog.data.VehicleRepository
import org.unishiasy.vehiclelog.data.backup.BackupRepository
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.data.local.reminderConstraints
import org.unishiasy.vehiclelog.ui.home.HomeViewModel
import org.unishiasy.vehiclelog.ui.ledger.LedgerHost
import org.unishiasy.vehiclelog.ui.ledger.LedgerViewModel
import org.unishiasy.vehiclelog.ui.reminders.RemindersViewModel
import org.unishiasy.vehiclelog.ui.settings.BackupSettingsViewModel
import org.unishiasy.vehiclelog.ui.settings.ThemeMode
import org.unishiasy.vehiclelog.ui.theme.VehicleLogTheme
import org.unishiasy.vehiclelog.ui.vehicle.VehicleViewModel

@RunWith(AndroidJUnit4::class)
class VisualAuditCaptureTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val vehicleA = VehicleEntity("audit-v-a", "car", "سمند LX", productionYear = 1400, productionYearCalendar = "solar_hijri")
    private val vehicleB = VehicleEntity("audit-v-b", "car", "پژو ۲۰۶ SD", productionYear = 1402, productionYearCalendar = "solar_hijri")
    private val instant = Instant.parse("2026-03-20T20:30:00Z")
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)

    private val reminderClock = object : Clock() {
        override fun instant(): Instant = instant
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(instant(), zone)
    }

    private lateinit var database: VehicleDatabase
    private lateinit var vehicleRepo: VehicleRepository
    private lateinit var ledgerRepo: LedgerRepository
    private lateinit var reminderRepo: ReminderRepository
    private lateinit var backupRepo: BackupRepository

    private lateinit var homeVm: HomeViewModel
    private lateinit var ledgerVm: LedgerViewModel
    private lateinit var vehicleVm: VehicleViewModel
    private lateinit var reminderVm: RemindersViewModel
    private lateinit var backupVm: BackupSettingsViewModel

    private var activity: MainActivity? = null

    @After
    fun tearDown() {
        instrumentation.runOnMainSync {
            activity?.viewModelStore?.clear()
            activity?.finish()
        }
        if (::database.isInitialized) database.close()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        Thread.sleep(500) // allow UI animations and rendering to settle
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val dir = File(context.getExternalFilesDir(null), "visual_audit")
        dir.mkdirs()
        val file = File(dir, "$name.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun launchActivity() {
        val component = ComponentName(context, MainActivity::class.java)
        val command = "am start -W -a ${Intent.ACTION_MAIN} -c ${Intent.CATEGORY_LAUNCHER} -n ${component.flattenToString()}"
        val launch = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command),
        ).bufferedReader().use { it.readText() }
        assertTrue(launch, launch.lineSequence().any { it.trim() == "Status: ok" })
    }

    private fun resumedActivity(): MainActivity? {
        var resumed: MainActivity? = null
        instrumentation.runOnMainSync {
            resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>().singleOrNull()
        }
        return resumed
    }

    private fun setupEnvironment(
        seedVehicles: Int = 1,
        seedLedger: Boolean = true,
        fontScale: Float? = null,
        themeMode: ThemeMode = ThemeMode.LIGHT,
    ) {
        val app = context.applicationContext as VehicleLedgerApplication
        database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java).addCallback(reminderConstraints).build()
        vehicleRepo = VehicleRepository(database.vehicleDao())
        ledgerRepo = LedgerRepository(database)
        reminderRepo = ReminderRepository(database, ledgerRepo, reminderClock)
        backupRepo = BackupRepository(database, app.container.reminderNotifications, app.container::reconcileReminderSchedule, clock)

        runBlocking {
            if (seedVehicles >= 1) {
                database.vehicleDao().insert(vehicleA)
            }
            if (seedVehicles >= 2) {
                database.vehicleDao().insert(vehicleB)
            }
            if (seedVehicles >= 1 && seedLedger) {
                // Standalone baseline mileage
                ledgerRepo.createStandaloneReading(UUID.randomUUID(), OdometerInput(vehicleA.id, "2026-03-01", 120000L))

                // Fuel expense
                ledgerRepo.create(
                    id = UUID.randomUUID(),
                    createdAtEpochMillis = instant.toEpochMilli(),
                    input = EntryInput(
                        vehicleId = vehicleA.id,
                        category = "fuel",
                        occurredOn = "2026-03-10",
                        amountRials = 15000000L, // 1,500,000 Toman
                        fuelVolumeLiters = "45",
                        notes = "بنزین آزاد پمپ میدان آزادی",
                    ),
                    kilometers = 120450L,
                )

                // Oil change
                ledgerRepo.create(
                    id = UUID.randomUUID(),
                    createdAtEpochMillis = instant.toEpochMilli(),
                    input = EntryInput(
                        vehicleId = vehicleA.id,
                        category = "oil_change",
                        occurredOn = "2026-03-12",
                        amountRials = 8500000L, // 850,000 Toman
                        notes = "تعویض روغن بهران سوپر رانا و فیلترها",
                    ),
                    kilometers = 120500L,
                )

                // Repair
                ledgerRepo.create(
                    id = UUID.randomUUID(),
                    createdAtEpochMillis = instant.toEpochMilli(),
                    input = EntryInput(
                        vehicleId = vehicleA.id,
                        category = "repair",
                        occurredOn = "2026-03-15",
                        amountRials = 32000000L, // 3,200,000 Toman
                        notes = "تعویض لنت ترمز جلو و بلبرینگ چرخ",
                    ),
                    kilometers = null,
                )

                // Due reminder
                reminderRepo.create(
                    vehicleId = vehicleA.id,
                    id = UUID.randomUUID(),
                    input = ReminderInput(
                        title = "بیمه شخص ثالث",
                        dueOn = "2026-03-20",
                        dueKilometers = null,
                    ),
                )

                // Pending reminder
                reminderRepo.create(
                    vehicleId = vehicleA.id,
                    id = UUID.randomUUID(),
                    input = ReminderInput(
                        title = "تسمه تایم",
                        dueOn = null,
                        dueKilometers = 130000L,
                    ),
                )
            }
        }

        launchActivity()
        activity = checkNotNull(resumedActivity())

        instrumentation.runOnMainSync {
            val act = checkNotNull(activity)
            val factory = viewModelFactory {
                initializer { HomeViewModel(vehicleRepo, ledgerRepo) }
                initializer { LedgerViewModel(ledgerRepo, clock) }
                initializer { VehicleViewModel(vehicleRepo) }
                initializer { RemindersViewModel(reminderRepo, ledgerRepo, reminderClock) }
                initializer { BackupSettingsViewModel(backupRepo, context.contentResolver) }
            }
            val provider = ViewModelProvider(act, factory)
            reminderVm = provider["audit-reminders", RemindersViewModel::class.java]
            homeVm = provider["audit-home", HomeViewModel::class.java]
            ledgerVm = provider["audit-ledger", LedgerViewModel::class.java]
            vehicleVm = provider["audit-vehicles", VehicleViewModel::class.java]
            backupVm = provider["audit-backup", BackupSettingsViewModel::class.java]

            act.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalDensity provides Density(density.density, fontScale ?: density.fontScale),
                ) {
                    VehicleLogTheme(themeMode = themeMode) {
                        LedgerHost(vehicleVm, ledgerVm, homeVm, reminderVm, backupVm)
                    }
                }
            }
        }
    }

    @Test
    fun captureAllScreens() {
        // 1. Single vehicle Home screen
        setupEnvironment(seedVehicles = 1, seedLedger = true)
        compose.waitUntil(5_000) { compose.onAllNodesWithText("سمند LX").fetchSemanticsNodes().isNotEmpty() }
        capture("03_home_single_vehicle")

        // 2. Open Quick Add
        compose.onNodeWithTag("homeQuickAdd").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(context.getString(R.string.ledger_add)).fetchSemanticsNodes().isNotEmpty() }
        capture("05_quick_add_form")

        // 3. Quick Add with validation errors
        compose.onNodeWithTag("ledgerAmount").performTextReplacement("invalid")
        compose.onNodeWithTag("ledgerSave").performScrollTo().performClick()
        capture("06_quick_add_validation_error")

        // Cancel back to history
        instrumentation.runOnMainSync { ledgerVm.back() }

        // 4. Capture History
        compose.waitUntil(5_000) { compose.onAllNodesWithText(context.getString(R.string.ledger_history_title)).fetchSemanticsNodes().isNotEmpty() }
        capture("07_history_screen")

        // 5. Open Delete Dialog in History
        compose.onAllNodesWithText(context.getString(R.string.ledger_delete))[0].performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(context.getString(R.string.ledger_delete_title)).fetchSemanticsNodes().isNotEmpty() }
        capture("08_history_delete_dialog")
        compose.onNodeWithTag("ledgerCancelDelete").performClick()

        // Back from History to Home
        instrumentation.runOnMainSync { ledgerVm.back() }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("سمند LX").fetchSemanticsNodes().isNotEmpty() }

        // 6. Open Monthly Dashboard
        compose.onNodeWithTag("homeOpenDashboard").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("monthlyDashboard")).fetchSemanticsNodes().isNotEmpty() }
        capture("09_monthly_dashboard")
        compose.onNodeWithText(context.getString(R.string.dashboard_back)).performClick()
        instrumentation.runOnMainSync { ledgerVm.back() }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("سمند LX").fetchSemanticsNodes().isNotEmpty() }

        // 7. Open Reminders
        compose.onNodeWithTag("homeOpenReminders").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(context.getString(R.string.reminders_title)).fetchSemanticsNodes().isNotEmpty() }
        capture("10_reminders_screen")
        compose.onNodeWithTag("remindersBack").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("سمند LX").fetchSemanticsNodes().isNotEmpty() }

        // 8. Open Settings / Backup
        compose.onNodeWithTag("homeOpenSettings").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(context.getString(R.string.backup_settings)).fetchSemanticsNodes().isNotEmpty() }
        capture("11_settings_backup_screen")
        instrumentation.runOnMainSync { backupVm.back() }
    }

    @Test
    fun captureMultiVehicleAndVehicleSetup() {
        // Multi-vehicle home
        setupEnvironment(seedVehicles = 2, seedLedger = true)
        compose.waitUntil(5_000) { compose.onAllNodesWithText("سمند LX").fetchSemanticsNodes().isNotEmpty() }
        capture("04_home_multi_vehicle")

        // Vehicle Setup Form
        compose.onNodeWithTag("homeAddVehicle").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(context.getString(R.string.vehicle_add)).fetchSemanticsNodes().isNotEmpty() }
        capture("02_vehicle_setup_form")
    }

    @Test
    fun captureEmptyVehicleState() {
        setupEnvironment(seedVehicles = 0, seedLedger = false)
        compose.waitUntil(5_000) { compose.onAllNodesWithText(context.getString(R.string.vehicle_empty)).fetchSemanticsNodes().isNotEmpty() }
        capture("01_empty_vehicle_state")
    }

    @Test
    fun captureFontScale150() {
        setupEnvironment(seedVehicles = 1, seedLedger = true, fontScale = 1.5f)
        compose.waitUntil(5_000) { compose.onAllNodesWithText("سمند LX").fetchSemanticsNodes().isNotEmpty() }
        capture("13_font150_home")

        compose.onNodeWithTag("homeQuickAdd").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(context.getString(R.string.ledger_add)).fetchSemanticsNodes().isNotEmpty() }
        capture("12_font150_quick_add")
    }

    @Test
    fun captureDarkScreens() {
        setupEnvironment(seedVehicles = 1, seedLedger = true, themeMode = ThemeMode.DARK)
        compose.waitUntil(5_000) { compose.onAllNodesWithText("سمند LX").fetchSemanticsNodes().isNotEmpty() }
        capture("dark_03_home_single_vehicle")

        // Quick Add Dark
        compose.onNodeWithTag("homeQuickAdd").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(context.getString(R.string.ledger_add)).fetchSemanticsNodes().isNotEmpty() }
        capture("dark_05_quick_add_form")

        // Back from form to History
        instrumentation.runOnMainSync { ledgerVm.back() }
        compose.waitUntil(5_000) { compose.onAllNodesWithText(context.getString(R.string.ledger_history_title)).fetchSemanticsNodes().isNotEmpty() }
        capture("dark_07_history_screen")

        // Back from History to Home
        instrumentation.runOnMainSync { ledgerVm.back() }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("سمند LX").fetchSemanticsNodes().isNotEmpty() }

        // Dashboard Dark
        compose.onNodeWithTag("homeOpenDashboard").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("monthlyDashboard")).fetchSemanticsNodes().isNotEmpty() }
        capture("dark_09_monthly_dashboard")
        compose.onNodeWithText(context.getString(R.string.dashboard_back)).performClick()
        instrumentation.runOnMainSync { ledgerVm.back() }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("سمند LX").fetchSemanticsNodes().isNotEmpty() }

        // Reminders Dark
        compose.onNodeWithTag("homeOpenReminders").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(context.getString(R.string.reminders_title)).fetchSemanticsNodes().isNotEmpty() }
        capture("dark_10_reminders_screen")
        compose.onNodeWithTag("remindersBack").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("سمند LX").fetchSemanticsNodes().isNotEmpty() }

        // Settings Dark
        compose.onNodeWithTag("homeOpenSettings").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(context.getString(R.string.backup_settings)).fetchSemanticsNodes().isNotEmpty() }
        capture("dark_11_settings_backup_screen")
        instrumentation.runOnMainSync { backupVm.back() }
    }
}
