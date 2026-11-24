package org.unishiasy.vehiclelog.ui.ledger

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.EntrySaveResult
import org.unishiasy.vehiclelog.data.VehicleSaveResult
import org.unishiasy.vehiclelog.data.allowsUnpricedEntry
import org.unishiasy.vehiclelog.data.local.HistoryEntry
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.Money
import org.unishiasy.vehiclelog.domain.normalizeInputDigits
import org.unishiasy.vehiclelog.domain.toPersianGroupedDigits
import org.unishiasy.vehiclelog.domain.withPersianDigits
import org.unishiasy.vehiclelog.ui.components.CircularIconBadge
import org.unishiasy.vehiclelog.ui.components.DismissibleHeading
import org.unishiasy.vehiclelog.ui.components.HeaderCloseButton
import org.unishiasy.vehiclelog.ui.components.LedgerTopBar
import org.unishiasy.vehiclelog.ui.components.AppBottomDock
import org.unishiasy.vehiclelog.ui.components.StatusPill
import org.unishiasy.vehiclelog.ui.components.StickyFormFooter
import org.unishiasy.vehiclelog.ui.components.LedgerFab
import org.unishiasy.vehiclelog.ui.components.LedgerFabContentClearance
import org.unishiasy.vehiclelog.ui.components.EntrySelectorSheet
import org.unishiasy.vehiclelog.ui.components.JalaliDatePickerDialog
import org.unishiasy.vehiclelog.ui.components.MainTab
import org.unishiasy.vehiclelog.ui.components.PersianNumericTransformation
import org.unishiasy.vehiclelog.ui.components.TomanVisualTransformation
import org.unishiasy.vehiclelog.ui.home.HomeScreen
import org.unishiasy.vehiclelog.ui.home.HomeViewModel
import org.unishiasy.vehiclelog.ui.reminders.RemindersScreen
import org.unishiasy.vehiclelog.ui.reminders.RemindersViewModel
import org.unishiasy.vehiclelog.ui.settings.AppearancePreferences
import org.unishiasy.vehiclelog.ui.settings.BackupPreferences
import org.unishiasy.vehiclelog.ui.settings.BackupSettingsScreen
import org.unishiasy.vehiclelog.ui.settings.BackupSettingsViewModel
import org.unishiasy.vehiclelog.ui.settings.CloudSettingsViewModel
import org.unishiasy.vehiclelog.ui.theme.LocalVehicleLogSemanticColors
import org.unishiasy.vehiclelog.ui.theme.VehicleLogShapes
import org.unishiasy.vehiclelog.ui.tools.ToolsHubScreen
import org.unishiasy.vehiclelog.ui.tools.TripCalculatorScreen
import org.unishiasy.vehiclelog.ui.vehicle.VehicleScreen
import org.unishiasy.vehiclelog.ui.vehicle.VehicleViewModel

@Composable
internal fun LedgerHost(
    vehicles: VehicleViewModel,
    ledger: LedgerViewModel,
    home: HomeViewModel? = null,
    reminders: RemindersViewModel? = null,
    backupSettings: BackupSettingsViewModel? = null,
    cloudSettings: CloudSettingsViewModel? = null,
    appearancePreferences: AppearancePreferences? = null,
    backupPreferences: BackupPreferences? = null,
    reminderNotificationOptions: @Composable () -> Unit = {},
) {
    if (backupSettings != null) {
        val settings by backupSettings.state.collectAsStateWithLifecycle()
        if (settings.open) {
            BackupSettingsScreen(
                state = settings,
                viewModel = backupSettings,
                cloudSettings = cloudSettings,
                appearancePreferences = appearancePreferences,
                notificationOptions = reminderNotificationOptions,
                backupPreferences = backupPreferences,
            )
            return
        }
    }

    val state by ledger.state.collectAsStateWithLifecycle()
    val vehicle = state.vehicle
    val vehicleState by vehicles.state.collectAsStateWithLifecycle()
    val homeState = home?.state?.collectAsStateWithLifecycle()?.value
    val reminderState = reminders?.state?.collectAsStateWithLifecycle()?.value
    val reminderVehicle = reminderState?.vehicle

    val currentVehicle = vehicle ?: reminderVehicle ?: homeState?.selectedVehicle
    val entryFabFocus = remember { FocusRequester() }
    var restoreEntryFabFocus by remember { mutableStateOf(false) }
    LaunchedEffect(restoreEntryFabFocus) {
        if (restoreEntryFabFocus) {
            entryFabFocus.requestFocus()
            restoreEntryFabFocus = false
        }
    }

    var showEntrySelector by rememberSaveable { mutableStateOf(false) }
    val entryFab: @Composable () -> Unit = {
        LedgerFab(
            labelRes = R.string.dock_new_entry,
            onClick = { showEntrySelector = true },
            modifier = Modifier.focusRequester(entryFabFocus).testTag("entryFab"),
        )
    }
    var reminderAddTrigger by rememberSaveable { mutableIntStateOf(0) }

    if (showEntrySelector && currentVehicle != null) {
        EntrySelectorSheet(
            vehicleLabel = currentVehicle.displayLabel,
            onDismiss = {
                showEntrySelector = false
                restoreEntryFabFocus = true
            },
            onSelectFuel = {
                showEntrySelector = false
                ledger.openQuickAdd(currentVehicle, EntryCategory.Fuel)
            },
            onSelectExpense = {
                showEntrySelector = false
                ledger.openQuickAdd(currentVehicle, EntryCategory.Other)
            },
        )
    }

    val activeTab = when {
        state.toolsOpen -> MainTab.TOOLS
        vehicle != null && state.draft == null && state.dashboard == null -> MainTab.HISTORY
        reminderVehicle != null -> MainTab.REMINDERS
        else -> MainTab.HOME
    }

    val vehicleSaved = !vehicleState.managingList &&
        vehicleState.draft == null &&
        (vehicleState.saveResult is VehicleSaveResult.Created || vehicleState.saveResult is VehicleSaveResult.Updated)

    LaunchedEffect(vehicleState.saveResult) {
        val result = vehicleState.saveResult
        if (result is VehicleSaveResult.Created) {
            home?.selectVehicle(result.id)
        }
    }

    val showHome = home != null &&
        vehicleState.draft == null &&
        !vehicleState.managingList &&
        vehicleState.vehicles?.isNotEmpty() == true

    val dockComposable: @Composable () -> Unit = {
        if (showHome) {
            AppBottomDock(
                activeTab = activeTab,
                onSelectTab = { tab ->
                    when (tab) {
                        MainTab.HOME -> {
                            if (state.toolsOpen) ledger.closeTools()
                            if (vehicle != null) ledger.back()
                            if (reminderVehicle != null) reminders.back()
                        }
                        MainTab.HISTORY -> {
                            if (state.toolsOpen) ledger.closeTools()
                            if (reminderVehicle != null) reminders.back()
                            if (vehicle == null && currentVehicle != null) {
                                ledger.openVehicle(currentVehicle)
                            }
                        }
                        MainTab.REMINDERS -> {
                            if (state.toolsOpen) ledger.closeTools()
                            if (vehicle != null) ledger.back()
                            if (reminderVehicle == null && currentVehicle != null) {
                                reminders?.open(currentVehicle)
                            }
                        }
                        MainTab.TOOLS -> {
                            if (vehicle != null) ledger.back()
                            if (reminderVehicle != null) reminders.back()
                            ledger.openTools()
                        }
                    }
                },
            )
        }
    }

    if (vehicle != null) {
        val draft = state.draft
        val dashboard = state.dashboard
        val historyListState = rememberSaveable(vehicle.id, saver = LazyListState.Saver) { LazyListState() }
        val historyStateHolder = rememberSaveableStateHolder()
        BackHandler(enabled = draft == null, onBack = ledger::back)
        Surface(Modifier.fillMaxSize()) {
            if (dashboard != null) {
                MonthlyDashboardScreen(
                    vehicle.displayLabel,
                    dashboard,
                    ledger::selectDashboardMonth,
                    ledger::retryDashboard,
                    ledger::back,
                )
            } else if (draft == null) {
                historyStateHolder.SaveableStateProvider(vehicle.id) {
                    HistoryView(
                        vehicle, state, ledger, historyListState,
                        bottomBar = dockComposable,
                        floatingActionButton = entryFab,
                    )
                }
            } else {
                key(draft.id) {
                    EntryFormScreen(vehicle, draft, state, ledger)
                }
            }
        }

        state.inspectingEntryId?.let { inspectedId ->
            val inspected = state.entries?.firstOrNull { it.id == inspectedId }
            if (inspected != null) {
                EntryDetailSheet(
                    entry = inspected,
                    onEdit = { ledger.editFromDetail(inspected.id) },
                    onDelete = { ledger.deleteFromDetail(inspected.id) },
                    onDismiss = ledger::dismissEntryDetail,
                )
            }
        }

        state.entryPendingDeletion?.let {
            DeleteConfirmationDialog(
                deleting = state.deleting,
                onConfirm = ledger::confirmDelete,
                onDismiss = ledger::cancelDelete,
            )
        }
        return
    }

    if (showHome) {
        if (state.tripCalculatorOpen) {
            BackHandler(onBack = ledger::closeTripCalculator)
            TripCalculatorScreen(
                onBack = ledger::closeTripCalculator,
            )
            return
        }

        if (state.toolsOpen) {
            BackHandler(onBack = ledger::closeTools)
            ToolsHubScreen(
                currentVehicleLabel = currentVehicle?.displayLabel,
                onOpenTripCalculator = ledger::openTripCalculator,
                onOpenVehicleManagement = vehicles::openManagement,
                onOpenReports = {
                    if (currentVehicle != null) {
                        ledger.openDashboardForVehicle(currentVehicle)
                    }
                },
                onOpenSettings = {
                    backupSettings?.open()
                },
                bottomBar = dockComposable,
            )
            return
        }

        val observedVehicleId = reminderVehicle?.id ?: homeState?.selectedVehicle?.id
        if (reminders != null && observedVehicleId != null) {
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            LaunchedEffect(lifecycle, observedVehicleId, reminderState?.loadAttempt) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    reminders.observe(observedVehicleId)
                }
            }
            if (reminderVehicle != null) {
                BackHandler(onBack = reminders::back)
                RemindersScreen(
                    vehicle = reminderVehicle,
                    state = checkNotNull(reminderState),
                    viewModel = reminders,
                    notificationOptions = reminderNotificationOptions,
                    bottomBar = dockComposable,
                    newReminderRequestCount = reminderAddTrigger,
                    onNewReminder = { reminderAddTrigger++ },
                )
                return
            }
        }
        val homeReminders = reminderState?.takeIf { it.observedVehicleId == homeState?.selectedVehicle?.id }
        var staleBackupDismissed by rememberSaveable { mutableStateOf(false) }
        val lastBackupEpoch by (backupPreferences?.lastExportEpochMillis ?: remember {
            kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
        }).collectAsStateWithLifecycle()
        val hasHomeEntries = homeState?.recentHistory?.isNotEmpty() == true
        val showStaleBackupBanner = backupPreferences != null &&
            !staleBackupDismissed &&
            hasHomeEntries &&
            (lastBackupEpoch == null || backupPreferences.isBackupStale())
        HomeScreen(
            viewModel = home,
            state = checkNotNull(homeState),
            onOpenHistory = ledger::openVehicle,
            onOpenDashboard = ledger::openDashboardForVehicle,
            onAddVehicle = vehicles::addVehicle,
            onEditVehicle = vehicles::editVehicle,
            onOpenReminders = reminders?.let { it::open },
            dueReminders = homeReminders?.list?.due,
            remindersFailed = homeReminders?.loadFailed == true,
            onRetryReminders = reminders?.let { it::retry },
            onOpenSettings = backupSettings?.let { it::open },
            onManageVehicles = vehicles::openManagement,
            onOpenEntry = ledger::openEntryFromHome,
            showVehicleSavedBanner = vehicleSaved,
            onDismissVehicleSaved = vehicles::clearSaveResult,
            showStaleBackupBanner = showStaleBackupBanner,
            onDismissStaleBackup = { staleBackupDismissed = true },
            bottomBar = dockComposable,
            floatingActionButton = entryFab,
        )
    } else {
        VehicleScreen(vehicles, ledger::openVehicle, backupSettings?.let { it::open })
    }
}

private data class HistoryDisplayItem(
    val entry: HistoryEntry,
    val monthKey: String? = null,
    val monthHeader: String? = null,
    val monthSummary: String? = null,
)

@Composable
private fun HistoryView(
    vehicle: VehicleEntity,
    state: LedgerUiState,
    ledger: LedgerViewModel,
    listState: LazyListState = rememberLazyListState(),
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LedgerTopBar(
                title = stringResource(R.string.dock_history),
                subtitle = vehicle.displayLabel,
                onBack = ledger::back,
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = FabPosition.End,
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
        ) {


            TextButton(
                onClick = ledger::openDashboard,
                enabled = !state.openingEntry && !state.deleting,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 16.dp)
                    .heightIn(min = 48.dp)
                    .testTag("ledgerOpenDashboard"),
            ) {
                Text(stringResource(R.string.dashboard_title))
            }

            var searchQuery by rememberSaveable(vehicle.id) { mutableStateOf("") }
            var selectedCategoryKey by rememberSaveable(vehicle.id) { mutableStateOf<String?>(null) }
            val filterState = HistoryFilterState(query = searchQuery, categoryKey = selectedCategoryKey)
            val categoryLabels = EntryCategory.entries.associate { it.key to stringResource(it.label) }
            val fallbackGroupLabel = stringResource(R.string.history_month_other)
            val monthGroups = remember(state.entries, searchQuery, selectedCategoryKey, categoryLabels, fallbackGroupLabel) {
                state.entries?.let { entries ->
                    filterAndGroupHistory(entries, filterState, categoryLabels, fallbackGroupLabel)
                }.orEmpty()
            }
            val displayItems = remember(monthGroups) {
                monthGroups.flatMap { group ->
                    val summary = if (group.totalRials > 0) {
                        Money.formatTomanInput(group.totalRials)
                    } else null
                    group.entries.mapIndexed { idx, entry ->
                        HistoryDisplayItem(
                            entry = entry,
                            monthKey = if (idx == 0) group.monthKey else null,
                            monthHeader = if (idx == 0) group.headerLabel else null,
                            monthSummary = if (idx == 0) summary else null,
                        )
                    }
                }
            }

            if (state.entries != null && state.entries.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().testTag("historySearchInput"),
                        placeholder = { Text(stringResource(R.string.history_search_hint)) },
                        singleLine = true,
                        shape = VehicleLogShapes.medium,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.ContentOrRtl),
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(48.dp).testTag("historySearchClear"),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = stringResource(R.string.history_search_clear),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else null,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = selectedCategoryKey == null,
                            onClick = { selectedCategoryKey = null },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("historyFilterAll"),
                            shape = VehicleLogShapes.medium,
                            label = { Text(stringResource(R.string.history_filter_all)) },
                        )
                        for (cat in EntryCategory.entries) {
                            FilterChip(
                                selected = selectedCategoryKey == cat.key,
                                onClick = {
                                    selectedCategoryKey = if (selectedCategoryKey == cat.key) null else cat.key
                                },
                                modifier = Modifier.heightIn(min = 48.dp).testTag("historyFilter:${cat.key}"),
                                shape = VehicleLogShapes.medium,
                                label = { Text(stringResource(cat.label)) },
                            )
                        }
                    }
                }
            }

            // Recent records grouped by Jalali month.
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, bottom = LedgerFabContentClearance)
                    .testTag("ledgerHistoryList"),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { ResultMessage(state.result, isDelete = state.lastActionWasDelete) }

                when {
                    state.listFailed -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ResultMessage(EntrySaveResult.StorageFailure)
                                OutlinedButton(ledger::retryList, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                    Text(stringResource(R.string.vehicle_retry))
                                }
                            }
                        }
                    }
                    state.entries == null -> {
                        item {
                            Text(
                                stringResource(if (state.openingEntry) R.string.ledger_loading_entry else R.string.ledger_loading_recent),
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }
                    }
                    state.entries.isEmpty() -> {
                        item {
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = VehicleLogShapes.medium,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Text(
                                    stringResource(R.string.ledger_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }
                    displayItems.isEmpty() -> {
                        item {
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth().testTag("historyFilterEmpty"),
                                shape = VehicleLogShapes.medium,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        stringResource(R.string.history_filter_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            searchQuery = ""
                                            selectedCategoryKey = null
                                        },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("historyClearFilters"),
                                        shape = VehicleLogShapes.medium,
                                    ) {
                                        Text(stringResource(R.string.history_filter_reset))
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        if (state.openingEntry) {
                            item {
                                Text(
                                    stringResource(R.string.ledger_loading_entry),
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                )
                            }
                        }
                        items(
                            items = displayItems,
                            key = { it.entry.id },
                        ) { item ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (item.monthHeader != null && item.monthKey != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp, bottom = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = item.monthHeader,
                                            modifier = Modifier
                                                .semantics { heading() }
                                                .testTag("historyMonthHeader:${item.monthKey}"),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (item.monthSummary != null) {
                                            Text(
                                                text = stringResource(R.string.dashboard_toman, item.monthSummary),
                                                modifier = Modifier.testTag("historyMonthSummary:${item.monthKey}"),
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                                HistoryRecordRow(item.entry) { ledger.inspectEntry(item.entry.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryRecordRow(entry: HistoryEntry, onClick: () -> Unit) {
    val category = EntryCategory.entries.firstOrNull { it.key == entry.category }
    val categoryLabel = stringResource((category ?: EntryCategory.Other).label)
    val date = JalaliDates.formatGregorian(entry.occurredOn)
        ?: "\u2066${entry.occurredOn.withPersianDigits()}\u2069"
    val mileage = entry.linkedOdometerKm?.let {
        stringResource(R.string.ledger_kilometers, "\u2066${it.toPersianGroupedDigits()}\u2069")
    } ?: if (category?.let { allowsUnpricedEntry(it.key) } == true) {
        stringResource(R.string.ledger_no_recorded_mileage)
    } else null
    val amount = entry.amountRials?.let {
        stringResource(R.string.dashboard_toman, Money.formatTomanInput(it))
    } ?: stringResource(R.string.ledger_unpriced)

    val iconRes = when (entry.category) {
        "fuel" -> R.drawable.ic_fuel
        "oil_change" -> R.drawable.ic_drop
        "repair", "parts" -> R.drawable.ic_tool
        else -> R.drawable.ic_file
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("ledgerRecent:${entry.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header Row: Circular Icon Badge, Category Title, Date, Linked Odometer, and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularIconBadge(
                    painter = painterResource(iconRes),
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    iconColor = MaterialTheme.colorScheme.primary,
                    contentDescription = categoryLabel,
                    modifier = Modifier.size(40.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = amount,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (mileage != null) {
                            Text(
                                text = mileage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Fuel volume & unit rate breakdown
            if (entry.fuelVolumeMl != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                val liters = java.math.BigDecimal.valueOf(entry.fuelVolumeMl, 3).stripTrailingZeros().toPlainString()
                    .replace('.', '٫').withPersianDigits()
                val ratePerLiterToman = if (entry.amountRials != null && entry.fuelVolumeMl > 0) {
                    kotlin.math.round((entry.amountRials / 10.0) / (entry.fuelVolumeMl.toDouble() / 1000.0)).toLong()
                } else 0L
                val breakdownText = if (entry.amountRials != null && entry.fuelVolumeMl > 0 && ratePerLiterToman > 0L) {
                    val rateFormatted = ratePerLiterToman.toPersianGroupedDigits()
                    stringResource(R.string.ledger_fuel_rate_breakdown, "\u2066$liters\u2069", "\u2066$rateFormatted\u2069")
                } else {
                    stringResource(R.string.ledger_liters, "\u2066$liters\u2069")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_drop),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = breakdownText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Station Name
            if (!entry.stationName.isNullOrBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pin),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = entry.stationName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Status Pill (Full Tank)
            if (entry.isFullTank) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusPill(
                        text = stringResource(R.string.ledger_full_tank),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("entryFullTank:${entry.id}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryFormScreen(
    vehicle: VehicleEntity,
    draft: EntryDraft,
    state: LedgerUiState,
    ledger: LedgerViewModel,
) {
    val screenTitleRes = when {
        draft.editing -> R.string.ledger_edit
        draft.category == EntryCategory.Fuel -> R.string.home_log_fuel
        else -> R.string.home_log_expense
    }
    var confirmExit by rememberSaveable(draft.id) { mutableStateOf(false) }
    val requestExit = {
        if (!state.saving) {
            if (state.draftChanged) confirmExit = true else ledger.back()
        }
    }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    BackHandler(enabled = !imeVisible, onBack = requestExit)
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(stringResource(R.string.ledger_discard_draft_title)) },
            confirmButton = {
                TextButton(onClick = ledger::back, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.ledger_discard_draft_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.ledger_keep_draft))
                }
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LedgerTopBar(
                title = stringResource(screenTitleRes),
                subtitle = vehicle.displayLabel,
                actions = {
                    HeaderCloseButton(
                        onClose = requestExit,
                        modifier = Modifier.testTag("ledgerCloseEntry"),
                        enabled = !state.saving,
                        label = stringResource(if (draft.editing) R.string.app_close else R.string.ledger_close_new_entry),
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            StickyFormFooter(
                actionLabel = stringResource(if (draft.editing) R.string.vehicle_save else R.string.ledger_save),
                onAction = ledger::save,
                modifier = Modifier.navigationBarsPadding(),
                isActionBusy = state.saving,
                actionTestTag = "ledgerSave",
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = if (imeVisible) 8.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {


            EntryForm(draft, state, ledger)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryForm(
    draft: EntryDraft,
    state: LedgerUiState,
    ledger: LedgerViewModel,
) {
    val enabled = !state.saving
    val focusManager = LocalFocusManager.current
    val fontScale = LocalDensity.current.fontScale
    val amountVisualTransformation = remember { TomanVisualTransformation() }
    var showDatePicker by rememberSaveable(draft.id) { mutableStateOf(false) }

    val hasOptionalData = draft.editing && (!draft.mileage.isBlank() || !draft.notes.isBlank() || draft.omitMileage)
    val hasDetailsError = state.fieldErrors.containsKey(EntryField.Mileage) ||
        state.fieldErrors.containsKey(EntryField.Notes) ||
        state.result == EntrySaveResult.MileageConflict
    var detailsExpanded by rememberSaveable(draft.id) { mutableStateOf(hasOptionalData || hasDetailsError) }
    LaunchedEffect(hasDetailsError) {
        if (hasDetailsError) detailsExpanded = true
    }

    val todayLocalDate = ledger.todayDate()
    val todayJalali = JalaliDates.formatJalaliInput(todayLocalDate.toString())
    val yesterdayJalali = JalaliDates.formatJalaliInput(todayLocalDate.minusDays(1).toString())

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialDateText = draft.date,
            todayDate = todayLocalDate,
            onSelectDate = { selected ->
                ledger.changeField(EntryField.Date, selected)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    val isFuel = draft.category == EntryCategory.Fuel
    val amountLabel = when {
        isFuel -> R.string.ledger_fuel_total_paid
        allowsUnpricedEntry(draft.category.key) -> R.string.ledger_optional_amount
        else -> R.string.ledger_amount
    }
    val amountField: @Composable () -> Unit = {
        EntryTextField(
            draft.amount, EntryField.Amount, amountLabel, state, ledger,
            keyboard = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
            visualTransformation = amountVisualTransformation,
        )
    }
    if (isFuel) {
        val volumeField: @Composable () -> Unit = {
            EntryTextField(
                draft.volume, EntryField.Volume, R.string.ledger_volume, state, ledger,
                keyboard = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (fontScale >= 1.35f || maxWidth < 360.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    amountField()
                    volumeField()
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) { amountField() }
                    Box(Modifier.weight(1f)) { volumeField() }
                }
            }
        }

        // Full Tank toggle
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = VehicleLogShapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.ledger_full_tank),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.ledger_full_tank_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = draft.isFullTank,
                    onCheckedChange = ledger::changeFullTank,
                    modifier = Modifier.testTag("ledgerFullTankSwitch"),
                    enabled = enabled,
                )
            }
        }

        // Station Name for Fuel
        EntryTextField(
            draft.stationName, EntryField.StationName, R.string.ledger_station_name, state, ledger,
            keyboard = KeyboardType.Text,
            hint = R.string.ledger_station_name_hint,
            imeAction = ImeAction.Next,
        )
    } else {
        amountField()
    }
    if (draft.editing || !isFuel) {
        CategoryField(draft.category, enabled, allowFuel = draft.editing, onSelect = ledger::changeCategory)
    }

    // Gregorian Date warning if present
    draft.originalGregorianDate?.let { original ->
        Text(
            stringResource(R.string.ledger_original_date, "\u2068${JalaliDates.formatGregorian(original).orEmpty()}\u2069"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val dateError = state.fieldErrors[EntryField.Date]
    val dateSection = remember { BringIntoViewRequester() }
    val firstErrorIsDate = state.fieldErrors.keys.minByOrNull { it.ordinal } == EntryField.Date
    LaunchedEffect(firstErrorIsDate) {
        if (firstErrorIsDate) dateSection.bringIntoView()
    }
    Column(
        modifier = Modifier.bringIntoViewRequester(dateSection),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedCard(
            onClick = {
                focusManager.clearFocus()
                showDatePicker = true
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("ledgerDatePickerOpen"),
            shape = VehicleLogShapes.medium,
            border = BorderStroke(
                1.dp,
                if (dateError == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.ledger_date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (draft.date.isBlank()) stringResource(R.string.date_picker_open)
                        else "\u2066${draft.date.normalizeInputDigits().withPersianDigits()}\u2069",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Icon(
                    painterResource(R.drawable.ic_calendar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        if (dateError != null) StatusText(dateError, error = true)
        if (todayJalali != null || yesterdayJalali != null) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (todayJalali != null) {
                    FilterChip(
                        selected = draft.date.trim() == todayJalali,
                        onClick = { ledger.changeField(EntryField.Date, todayJalali) },
                        enabled = enabled,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("ledgerDateToday"),
                        shape = VehicleLogShapes.medium,
                        label = { Text(stringResource(R.string.date_chip_today)) },
                    )
                }
                if (yesterdayJalali != null) {
                    FilterChip(
                        selected = draft.date.trim() == yesterdayJalali,
                        onClick = { ledger.changeField(EntryField.Date, yesterdayJalali) },
                        enabled = enabled,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("ledgerDateYesterday"),
                        shape = VehicleLogShapes.medium,
                        label = { Text(stringResource(R.string.date_chip_yesterday)) },
                    )
                }
            }
        }
    }

    // Accordion Disclosure: Mileage & Notes (Optional)
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = VehicleLogShapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val disclosureStateDesc = stringResource(
                if (detailsExpanded) R.string.state_expanded else R.string.state_collapsed
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics(mergeDescendants = true) {
                        stateDescription = disclosureStateDesc
                    }
                    .clickable(
                        role = Role.Button,
                        onClick = { detailsExpanded = !detailsExpanded },
                    )
                    .testTag("ledgerDetailsToggle"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.entry_mileage_and_notes),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (detailsExpanded) "▲" else "▼",
                    modifier = Modifier.clearAndSetSemantics { },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            AnimatedVisibility(visible = detailsExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val latest = state.latestMileage?.toString()?.withPersianDigits()
                    Text(
                        if (latest == null) stringResource(R.string.ledger_mileage_reference_empty)
                        else stringResource(R.string.ledger_mileage_reference, "\u2066$latest\u2069"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    EntryTextField(
                        draft.mileage, EntryField.Mileage, R.string.ledger_mileage, state, ledger,
                        keyboard = KeyboardType.Number,
                        enabled = enabled && !draft.omitMileage,
                        hint = if (draft.originalMileage != null && !draft.omitMileage) R.string.ledger_keep_mileage else null,
                        imeAction = ImeAction.Next,
                    )

                    if (draft.originalMileage != null || draft.omitMileage) {
                        if (draft.omitMileage) Text(stringResource(R.string.ledger_omit_mileage_hint))
                        OutlinedButton(
                            onClick = ledger::toggleMileageRemoval,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            enabled = enabled,
                            shape = VehicleLogShapes.small,
                        ) {
                            Text(stringResource(if (draft.omitMileage) R.string.ledger_restore_mileage else R.string.ledger_remove_mileage))
                        }
                    }

                    if (!isFuel) {
                        EntryTextField(
                            draft.stationName, EntryField.StationName, R.string.ledger_station_name, state, ledger,
                            keyboard = KeyboardType.Text,
                            hint = R.string.ledger_station_name_hint,
                            imeAction = ImeAction.Next,
                        )
                    }

                    EntryTextField(
                        draft.notes, EntryField.Notes, R.string.ledger_notes, state, ledger,
                        singleLine = false,
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    )
                }
            }
        }
    }

    if (state.fieldErrors.isNotEmpty() && state.result != EntrySaveResult.MileageConflict) {
        StatusText(R.string.ledger_check_fields, error = true)
    }
    ResultMessage(state.result)
    if (state.result == EntrySaveResult.MileageConflict) {
        if (draft.originalMileage != null) Text(stringResource(R.string.ledger_conflict_remove_explanation))
        OutlinedButton(
            ledger::saveWithoutMileage,
            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("ledgerSaveWithoutMileage"),
            enabled = enabled,
            shape = VehicleLogShapes.medium,
        ) {
            Text(stringResource(R.string.ledger_save_without_mileage))
        }
    }

    Spacer(Modifier.height(8.dp))

    // Destructive Link in Edit Mode
    if (draft.editing) {
        TextButton(
            onClick = { ledger.requestDelete(draft.id.toString()) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("ledgerDeleteEntry"),
            enabled = enabled && !state.deleting,
        ) {
            Text(
                stringResource(R.string.ledger_delete_action),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun CategoryField(
    category: EntryCategory,
    enabled: Boolean,
    allowFuel: Boolean,
    onSelect: (EntryCategory) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.ledger_category), style = MaterialTheme.typography.labelMedium)

        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("ledgerCategory"),
                enabled = enabled,
                shape = VehicleLogShapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(category.label), style = MaterialTheme.typography.bodyLarge)
                    Text("▼", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            DropdownMenu(expanded && enabled, onDismissRequest = { expanded = false }) {
                for (option in EntryCategory.entries.filter { allowFuel || it != EntryCategory.Fuel }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(option.label)) },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("ledgerCategory:${option.key}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryTextField(
    value: String,
    field: EntryField,
    @StringRes label: Int,
    state: LedgerUiState,
    ledger: LedgerViewModel,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    enabled: Boolean = !state.saving,
    @StringRes hint: Int? = null,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = if (keyboard == KeyboardType.Text) {
        VisualTransformation.None
    } else {
        PersianNumericTransformation
    },
) {
    val error = state.fieldErrors[field]
    val supportingText = error ?: hint
    val bringIntoView = remember { BringIntoViewRequester() }
    val firstInvalidField = state.fieldErrors.keys.minByOrNull { it.ordinal }
    LaunchedEffect(firstInvalidField == field) {
        if (firstInvalidField == field) bringIntoView.bringIntoView()
    }
    OutlinedTextField(
        value = value,
        onValueChange = { ledger.changeField(field, it) },
        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(bringIntoView).testTag("ledger${field.name}"),
        label = { Text(stringResource(label)) },
        enabled = enabled,
        isError = error != null,
        shape = VehicleLogShapes.medium,
        supportingText = supportingText?.let { { Text(stringResource(it)) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textDirection = if (keyboard == KeyboardType.Text) TextDirection.ContentOrRtl else TextDirection.Ltr,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = imeAction),
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
    )
}

@Composable
private fun ResultMessage(result: EntrySaveResult?, isDelete: Boolean = false) {
    val message = when (result) {
        EntrySaveResult.Saved -> if (isDelete) R.string.ledger_deleted else R.string.ledger_saved
        EntrySaveResult.NotFound -> R.string.ledger_not_found
        EntrySaveResult.MileageConflict -> R.string.ledger_mileage_conflict_action
        EntrySaveResult.ConstraintViolation -> R.string.ledger_constraint
        EntrySaveResult.StorageFailure -> R.string.ledger_storage_failure
        EntrySaveResult.InvalidCategory -> R.string.ledger_category_invalid
        EntrySaveResult.ReadingRequiresVehicleConfirmation -> R.string.ledger_vehicle_conflict
        else -> return
    }
    StatusText(message, error = result != EntrySaveResult.Saved)
}

@Composable
private fun StatusText(@StringRes message: Int, error: Boolean) {
    Text(
        stringResource(message),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun DeleteConfirmationDialog(
    deleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        title = {
            DismissibleHeading(
                title = stringResource(R.string.delete_confirm_sheet_title),
                onClose = onDismiss,
                closeModifier = Modifier.testTag("ledgerCancelDelete"),
                closeEnabled = !deleting,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Text(
                stringResource(R.string.ledger_delete_confirm),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !deleting,
                modifier = Modifier.testTag("ledgerConfirmDelete").heightIn(min = 48.dp),
                shape = VehicleLogShapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(if (deleting) R.string.ledger_deleting else R.string.delete_confirm_sheet_action))
            }
        },
    )
}
