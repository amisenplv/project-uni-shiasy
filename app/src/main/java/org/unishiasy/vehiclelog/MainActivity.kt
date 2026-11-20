package org.unishiasy.vehiclelog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.unishiasy.vehiclelog.ui.home.HomeViewModel
import org.unishiasy.vehiclelog.ui.reminders.RemindersViewModel
import org.unishiasy.vehiclelog.ui.reminders.ReminderNotificationOptions
import org.unishiasy.vehiclelog.ui.settings.BackupSettingsViewModel
import org.unishiasy.vehiclelog.ui.settings.CloudSettingsViewModel
import org.unishiasy.vehiclelog.ui.theme.VehicleLogTheme
import org.unishiasy.vehiclelog.ui.settings.ThemeMode
import org.unishiasy.vehiclelog.ui.ledger.LedgerHost
import org.unishiasy.vehiclelog.ui.ledger.LedgerViewModel
import org.unishiasy.vehiclelog.ui.vehicle.VehicleViewModel

class MainActivity : ComponentActivity() {
    private val vehicleViewModel: VehicleViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = application as VehicleLedgerApplication
                VehicleViewModel(
                    repository = app.container.vehicleRepository,
                    savedStateHandle = createSavedStateHandle(),
                    loadCatalog = app.container::loadVehicleCatalog,
                )
            }
        }
    }
    private val ledgerViewModel: LedgerViewModel by viewModels {
        viewModelFactory {
            initializer {
                LedgerViewModel((application as VehicleLedgerApplication).container.ledgerRepository, savedStateHandle = createSavedStateHandle())
            }
        }
    }
    private val homeViewModel: HomeViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = application as VehicleLedgerApplication
                HomeViewModel(
                    vehicleRepository = app.container.vehicleRepository,
                    ledgerRepository = app.container.ledgerRepository,
                    initialSelectedVehicleId = app.container.selectedVehiclePreferences.getSelectedVehicleId(),
                    onPersistSelectedVehicleId = app.container.selectedVehiclePreferences::setSelectedVehicleId,
                )
            }
        }
    }

    private val remindersViewModel: RemindersViewModel by viewModels {
        viewModelFactory {
            initializer {
                val container = (application as VehicleLedgerApplication).container
                RemindersViewModel(container.reminderRepository, container.ledgerRepository, savedStateHandle = createSavedStateHandle())
            }
        }
    }

    private val backupSettingsViewModel: BackupSettingsViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = application as VehicleLedgerApplication
                BackupSettingsViewModel(
                    repository = app.container.backupRepository,
                    resolver = app.contentResolver,
                    savedStateHandle = createSavedStateHandle(),
                    onExportRecorded = { app.container.backupPreferences.recordExport() },
                )
            }
        }
    }

    private val cloudSettingsViewModel: CloudSettingsViewModel by viewModels {
        viewModelFactory {
            initializer {
                val container = (application as VehicleLedgerApplication).container
                CloudSettingsViewModel(checkNotNull(container.cloudRepository))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as VehicleLedgerApplication).container.reconcileReminderSchedule()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val vehicles = vehicleViewModel
        val ledger = ledgerViewModel
        val home = homeViewModel
        val reminders = remindersViewModel
        val backupSettings = backupSettingsViewModel
        val container = (application as VehicleLedgerApplication).container
        val cloudSettings = if (BuildConfig.LOCAL_CLOUD_ENABLED) cloudSettingsViewModel else null
        setContent {
            val themeMode by container.appearancePreferences.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                VehicleLogTheme(darkTheme = darkTheme) {
                    LedgerHost(
                        vehicles = vehicles,
                        ledger = ledger,
                        home = home,
                        reminders = reminders,
                        backupSettings = backupSettings,
                        cloudSettings = cloudSettings,
                        appearancePreferences = container.appearancePreferences,
                        backupPreferences = container.backupPreferences,
                    ) {
                        ReminderNotificationOptions(container.reminderNotifications, container::reconcileReminderSchedule)
                    }
                }
            }
        }
    }
}
