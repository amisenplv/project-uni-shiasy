package org.unishiasy.vehiclelog.ui.ledger

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.EntrySaveResult
import org.unishiasy.vehiclelog.data.VehicleSaveResult
import org.unishiasy.vehiclelog.data.local.HistoryEntry
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.Money
import org.unishiasy.vehiclelog.domain.withPersianDigits
import org.unishiasy.vehiclelog.ui.components.LedgerTopBar
import org.unishiasy.vehiclelog.ui.components.AppBottomDock
import org.unishiasy.vehiclelog.ui.components.StickyFormFooter
import org.unishiasy.vehiclelog.ui.components.EntrySelectorSheet
import org.unishiasy.vehiclelog.ui.components.JalaliDatePickerDialog
import org.unishiasy.vehiclelog.ui.components.MainTab
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
    var showEntrySelector by rememberSaveable { mutableStateOf(false) }
    var reminderAddTrigger by rememberSaveable { mutableIntStateOf(0) }

    if (showEntrySelector && currentVehicle != null) {
        EntrySelectorSheet(
            onDismiss = { showEntrySelector = false },
            onSelectFuel = {
                showEntrySelector = false
                ledger.openQuickAdd(currentVehicle, EntryCategory.Fuel)
            },
            onSelectExpense = {
                showEntrySelector = false
                ledger.openQuickAdd(currentVehicle, EntryCategory.Other)
            },
            onSelectService = {
                showEntrySelector = false
                ledger.openQuickAdd(currentVehicle, EntryCategory.OilChange)
            },
        )
    }

    val activeTab = when {
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
                            if (vehicle != null) ledger.back()
                            if (reminderVehicle != null) reminders.back()
                        }
                        MainTab.HISTORY -> {
                            if (reminderVehicle != null) reminders.back()
                            if (vehicle == null && currentVehicle != null) {
                                ledger.openVehicle(currentVehicle)
                            }
                        }
                        MainTab.REMINDERS -> {
                            if (vehicle != null) ledger.back()
                            if (reminderVehicle == null && currentVehicle != null) {
                                reminders?.open(currentVehicle)
                            }
                        }
                    }
                },
                onPrimaryAction = {
                    if (activeTab == MainTab.REMINDERS) {
                        reminderAddTrigger++
                    } else if (currentVehicle != null) {
                        showEntrySelector = true
                    }
                },
            )
        }
    }

    if (vehicle != null) {
        val draft = state.draft
        val dashboard = state.dashboard
        val historyListState = rememberSaveable(vehicle.id, saver = LazyListState.Saver) { LazyListState() }
        BackHandler(onBack = ledger::back)
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
                HistoryView(vehicle, state, ledger, historyListState, bottomBar = dockComposable)
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
            onQuickAdd = ledger::openQuickAdd,
            onOpenHistory = ledger::openVehicle,
            onOpenDashboard = ledger::openDashboardForVehicle,
            onAddVehicle = vehicles::addVehicle,
            onEditVehicle = vehicles::editVehicle,
            onOpenReminders = reminders?.let { it::open },
            dueReminders = homeReminders?.list?.due,
            remindersFailed = homeReminders?.loadFailed == true,
            onRetryReminders = reminders?.let { it::retry },
            onOpenSettings = backupSettings?.let { it::open },
            onOpenGeneralExpense = { selected -> ledger.openQuickAdd(selected, EntryCategory.Other) },
            onManageVehicles = vehicles::openManagement,
            onOpenEntry = ledger::openEntryFromHome,
            showVehicleSavedBanner = vehicleSaved,
            onDismissVehicleSaved = vehicles::clearSaveResult,
            showStaleBackupBanner = showStaleBackupBanner,
            onDismissStaleBackup = { staleBackupDismissed = true },
            bottomBar = dockComposable,
        )
    } else {
        VehicleScreen(vehicles, ledger::openVehicle, backupSettings?.let { it::open })
    }
}

@Composable
private fun HistoryView(
    vehicle: VehicleEntity,
    state: LedgerUiState,
    ledger: LedgerViewModel,
    listState: LazyListState = rememberLazyListState(),
    bottomBar: @Composable () -> Unit = {},
) {
    val semanticColors = LocalVehicleLogSemanticColors.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LedgerTopBar(
                title = stringResource(R.string.ledger_title),
                subtitle = vehicle.displayLabel,
                onBack = ledger::back,
            )
        },
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
        ) {


            // Notebook Segmented Tabs (History & Report)
            PrimaryTabRow(
                selectedTabIndex = 0,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(0, matchContentSize = false),
                        width = androidx.compose.ui.unit.Dp.Unspecified,
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) },
            ) {
                Tab(
                    selected = true,
                    onClick = { /* already on history */ },
                    text = {
                        Text(
                            stringResource(R.string.notebook_history_tab),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
                Tab(
                    selected = false,
                    onClick = ledger::openDashboard,
                    text = {
                        Text(
                            stringResource(R.string.notebook_report_tab),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("ledgerOpenDashboard"),
                )
            }

            // Search & Category Filter Controls (above LazyColumn so list item indices remain stable)
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
                    group.entries.mapIndexed { idx, entry ->
                        Triple(
                            entry,
                            if (idx == 0) group.monthKey else null,
                            if (idx == 0) group.headerLabel else null,
                        )
                    }
                }
            }

            if (state.entries != null && state.entries.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
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

            // History Records Content with Day Rail
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .testTag("ledgerHistoryList"),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = ledger::newEntry,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                enabled = !state.openingEntry && !state.deleting,
                                shape = VehicleLogShapes.medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_plus),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.ledger_add))
                            }
                        }
                        ResultMessage(state.result, isDelete = state.lastActionWasDelete)
                        Text(
                            stringResource(R.string.ledger_history_title),
                            Modifier.semantics { heading() },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }

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
                            key = { it.first.id },
                        ) { (entry, monthKey, monthHeader) ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (monthHeader != null && monthKey != null) {
                                    Text(
                                        text = monthHeader,
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .semantics { heading() }
                                            .testTag("historyMonthHeader:$monthKey"),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                HistoryDayRailRow(entry, state, ledger)
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
private fun HistoryDayRailRow(entry: HistoryEntry, state: LedgerUiState, ledger: LedgerViewModel) {
    val semanticColors = LocalVehicleLogSemanticColors.current
    val categoryLabelRes = when (entry.category) {
        "fuel" -> R.string.ledger_fuel
        "oil_change" -> R.string.ledger_oil_change
        "repair" -> R.string.ledger_repair
        "insurance" -> R.string.ledger_insurance
        "wash" -> R.string.ledger_wash
        "parts" -> R.string.ledger_parts
        else -> R.string.ledger_other
    }
    val iconRes = when (entry.category) {
        "fuel" -> R.drawable.ic_fuel
        "oil_change" -> R.drawable.ic_drop
        "repair", "parts" -> R.drawable.ic_tool
        else -> R.drawable.ic_file
    }

    // Extract Day Number for the Day Rail
    val jalaliDateStr = JalaliDates.formatGregorian(entry.occurredOn) ?: entry.occurredOn
    val dayNumber = jalaliDateStr.split(" ").firstOrNull()?.withPersianDigits() ?: "۱"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Day Rail Column (40dp Day Pill + Vertical Connecting Rail)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(semanticColors.wash),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = dayNumber,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = semanticColors.action,
                    ),
                )
            }
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }

        // Record Card
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { ledger.inspectEntry(entry.id) }
                .testTag("ledgerRecent:${entry.id}"),
            shape = VehicleLogShapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(semanticColors.wash),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                tint = semanticColors.action,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                stringResource(categoryLabelRes),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (entry.linkedOdometerKm == null) {
                                Text(
                                    jalaliDateStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        if (entry.amountRials != null) {
                            Text(
                                stringResource(
                                    R.string.dashboard_toman,
                                    Money.formatTomanInput(entry.amountRials),
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End,
                            )
                        } else {
                            Text(
                                stringResource(R.string.ledger_unpriced),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Volume, Mileage & Notes Details
                val hasDetails = entry.fuelVolumeMl != null || entry.linkedOdometerKm != null || !entry.notes.isNullOrBlank()
                if (hasDetails) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        entry.fuelVolumeMl?.let { volumeMl ->
                            val liters = java.math.BigDecimal.valueOf(volumeMl, 3).stripTrailingZeros().toPlainString()
                                .replace('.', '٫').withPersianDigits()
                            Text(
                                stringResource(R.string.ledger_liters, "\u2066$liters\u2069"),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        entry.linkedOdometerKm?.let { km ->
                            Text(
                                jalaliDateStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val kmString = km.toString().withPersianDigits()
                            Text(
                                stringResource(R.string.ledger_kilometers, "\u2066$kmString\u2069"),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    entry.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        Text(
                            notes,
                            style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.ContentOrRtl),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Actions row on Card (Edit + Delete)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { ledger.requestDelete(entry.id) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("ledgerDelete:${entry.id}"),
                        enabled = !state.openingEntry && !state.deleting,
                    ) {
                        Text(
                            stringResource(R.string.ledger_delete),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
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
        else -> R.string.ledger_add
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LedgerTopBar(
                title = stringResource(screenTitleRes),
                subtitle = vehicle.displayLabel,
                onBack = ledger::back,
                backLabel = stringResource(R.string.vehicle_cancel),
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {


            EntryForm(draft, state, ledger)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryForm(draft: EntryDraft, state: LedgerUiState, ledger: LedgerViewModel) {
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

    if (draft.category != EntryCategory.Fuel) {
        CategoryField(draft.category, enabled, ledger::changeCategory)
    }

    // Amount & Volume Inputs
    if (draft.category == EntryCategory.Fuel) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stackInputs = fontScale >= 1.35f || maxWidth < 360.dp
            if (stackInputs) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    EntryTextField(
                        draft.amount, EntryField.Amount, R.string.ledger_amount, state, ledger,
                        keyboard = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                        visualTransformation = amountVisualTransformation,
                    )
                    EntryTextField(
                        draft.volume, EntryField.Volume, R.string.ledger_volume, state, ledger,
                        keyboard = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.weight(1f)) {
                        EntryTextField(
                            draft.amount, EntryField.Amount, R.string.ledger_amount, state, ledger,
                            keyboard = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                            visualTransformation = amountVisualTransformation,
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        EntryTextField(
                            draft.volume, EntryField.Volume, R.string.ledger_volume, state, ledger,
                            keyboard = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        )
                    }
                }
            }
        }
    } else {
        EntryTextField(
            draft.amount, EntryField.Amount, R.string.ledger_amount, state, ledger,
            keyboard = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
            visualTransformation = amountVisualTransformation,
        )
    }

    // Gregorian Date warning if present
    draft.originalGregorianDate?.let { original ->
        Text(
            stringResource(R.string.ledger_original_date, "\u2068${JalaliDates.formatGregorian(original).orEmpty()}\u2069"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    // Date Field with Calendar Trigger & Quick Today/Yesterday Chips
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EntryTextField(
            draft.date, EntryField.Date, R.string.ledger_date, state, ledger,
            numeric = true,
            hint = if (draft.originalGregorianDate == null) R.string.ledger_date_example else R.string.ledger_replace_date,
            imeAction = ImeAction.Next,
            trailingIcon = {
                IconButton(
                    onClick = { showDatePicker = true },
                    enabled = enabled,
                    modifier = Modifier.size(48.dp).testTag("ledgerDatePickerOpen"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = stringResource(R.string.date_picker_open),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )
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

    if (draft.category == EntryCategory.Fuel) {
        CategoryField(draft.category, enabled, ledger::changeCategory)
    }

    // Accordion Disclosure: Mileage & Notes (Optional)
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = VehicleLogShapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { detailsExpanded = !detailsExpanded }
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

    OutlinedButton(
        onClick = ledger::back,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        enabled = enabled,
        shape = VehicleLogShapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            stringResource(R.string.vehicle_cancel),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CategoryField(category: EntryCategory, enabled: Boolean, onSelect: (EntryCategory) -> Unit) {
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
                for (option in EntryCategory.entries) {
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
    numeric: Boolean = keyboard != KeyboardType.Text,
    singleLine: Boolean = true,
    enabled: Boolean = !state.saving,
    @StringRes hint: Int? = null,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val error = state.fieldErrors[field]
    val supportingText = error ?: hint
    OutlinedTextField(
        value = value,
        onValueChange = { ledger.changeField(field, it) },
        modifier = Modifier.fillMaxWidth().testTag("ledger${field.name}"),
        label = { Text(stringResource(label)) },
        enabled = enabled,
        isError = error != null,
        shape = VehicleLogShapes.medium,
        supportingText = supportingText?.let { { Text(stringResource(it)) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textDirection = if (numeric) TextDirection.Ltr else TextDirection.ContentOrRtl,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = imeAction),
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
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
            Text(
                stringResource(R.string.delete_confirm_sheet_title),
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
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !deleting,
                modifier = Modifier.testTag("ledgerCancelDelete").heightIn(min = 48.dp),
                shape = VehicleLogShapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(stringResource(R.string.vehicle_cancel))
            }
        },
    )
}
