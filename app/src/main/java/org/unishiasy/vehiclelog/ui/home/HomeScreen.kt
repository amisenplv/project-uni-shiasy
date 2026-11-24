package org.unishiasy.vehiclelog.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextOverflow
import java.math.BigInteger
import java.util.Locale
import kotlinx.coroutines.delay
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.local.HistoryEntry
import org.unishiasy.vehiclelog.data.local.ReminderEntity
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.domain.FuelConsumptionCalculator
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.JalaliMonth
import org.unishiasy.vehiclelog.domain.Money
import org.unishiasy.vehiclelog.domain.toPersianGroupedDigits
import org.unishiasy.vehiclelog.domain.withPersianDigits
import org.unishiasy.vehiclelog.ui.components.CircularIconBadge
import org.unishiasy.vehiclelog.ui.components.LedgerFabContentClearance
import org.unishiasy.vehiclelog.ui.components.LedgerTopBar
import org.unishiasy.vehiclelog.ui.components.LedgerSettingsAction
import org.unishiasy.vehiclelog.ui.components.MetricCard
import org.unishiasy.vehiclelog.ui.components.NoticeBanner
import org.unishiasy.vehiclelog.ui.components.NoticeTone
import org.unishiasy.vehiclelog.ui.components.StatusPill
import org.unishiasy.vehiclelog.ui.components.VehicleSwitcherSheet
import org.unishiasy.vehiclelog.ui.ledger.OdometerSheet
import org.unishiasy.vehiclelog.ui.reminders.ReminderThresholdText
import org.unishiasy.vehiclelog.ui.theme.LocalVehicleLogSemanticColors
import org.unishiasy.vehiclelog.ui.theme.VehicleLogShapes

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeScreen(
    viewModel: HomeViewModel,
    state: HomeUiState,
    onOpenHistory: (VehicleEntity) -> Unit,
    onOpenDashboard: (VehicleEntity, JalaliMonth?) -> Unit,
    onAddVehicle: () -> Unit,
    onEditVehicle: (VehicleEntity) -> Unit,
    onOpenReminders: ((VehicleEntity) -> Unit)? = null,
    dueReminders: List<ReminderEntity>? = null,
    remindersFailed: Boolean = false,
    onRetryReminders: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onManageVehicles: (() -> Unit)? = null,
    onOpenEntry: ((VehicleEntity, String) -> Unit)? = null,
    showVehicleSavedBanner: Boolean = false,
    onDismissVehicleSaved: () -> Unit = {},
    showStaleBackupBanner: Boolean = false,
    onDismissStaleBackup: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
) {
    val month by viewModel.currentMonth.collectAsStateWithLifecycle(JalaliDates.monthOf(viewModel.todayDate()))
    val monthlyReport by viewModel.monthlyReport.collectAsStateWithLifecycle()
    val vehicles = state.vehicles.orEmpty()
    val selectedVehicle = state.selectedVehicle
    val semanticColors = LocalVehicleLogSemanticColors.current
    var showVehicleSwitcher by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showVehicleSavedBanner) {
        if (showVehicleSavedBanner) {
            delay(3_500)
            onDismissVehicleSaved()
        }
    }

    LaunchedEffect(state.odometerFeedbackRes) {
        if (state.odometerFeedbackRes != null) {
            delay(3_500)
            viewModel.dismissOdometerFeedback()
        }
    }

    state.odometerDraft?.let { draft ->
        OdometerSheet(
            draft = draft,
            standaloneReadings = state.standaloneReadings,
            currentMileage = state.selectedVehicleMileage,
            todayDate = viewModel.todayDate(),
            onKilometersChange = viewModel::changeOdometerKilometers,
            onDateChange = viewModel::changeOdometerDate,
            onSave = viewModel::saveStandaloneOdometer,
            onEditReading = viewModel::editStandaloneReading,
            onRequestDeleteReading = viewModel::requestDeleteStandaloneReading,
            onConfirmDeleteReading = viewModel::confirmDeleteStandaloneReading,
            onCancelDeleteReading = viewModel::cancelDeleteStandaloneReading,
            onDismiss = viewModel::closeOdometerSheet,
        )
    }

    if (showVehicleSwitcher && selectedVehicle != null) {
        VehicleSwitcherSheet(
            vehicles = vehicles,
            selectedVehicleId = selectedVehicle.id,
            onSelectVehicle = viewModel::selectVehicle,
            onManageVehicles = onManageVehicles ?: onAddVehicle,
            onDismiss = { showVehicleSwitcher = false },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LedgerTopBar(title = stringResource(R.string.app_name)) {
                if (onOpenSettings != null) LedgerSettingsAction(onOpenSettings)
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = if (selectedVehicle != null) floatingActionButton else ({}),
        floatingActionButtonPosition = FabPosition.End,
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
                .padding(bottom = if (selectedVehicle != null) LedgerFabContentClearance + 16.dp else 0.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .testTag("homeScreen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            if (selectedVehicle != null) {
                val mileage = state.selectedVehicleMileage
                val mileageFormatted = if (mileage == null) {
                    stringResource(R.string.ledger_mileage_reference_empty)
                } else {
                    stringResource(R.string.ledger_kilometers, "\u2066${mileage.toPersianGroupedDigits()}\u2069")
                }
                val headerSummary = if (mileage == null) {
                    selectedVehicle.displayLabel
                } else {
                    "${selectedVehicle.displayLabel} • ${mileage.toPersianGroupedDigits()} ${stringResource(R.string.trip_unit_km)}"
                }

                // Top Vehicle Selector Header Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("homeSelectedVehicleCard"),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClickLabel = stringResource(R.string.vehicle_select_title)) {
                                        showVehicleSwitcher = true
                                    }
                                    .testTag("homeOpenVehicleSwitcher"),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularIconBadge(
                                    icon = ImageVector.vectorResource(R.drawable.ic_car),
                                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    iconColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = selectedVehicle.displayLabel,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                textDirection = TextDirection.ContentOrRtl,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.testTag("homeSelectedVehicleName"),
                                        )
                                        Text(
                                            text = "⌄",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                    }
                                    Text(
                                        text = headerSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("homeVehicleHeaderSummary"),
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onEditVehicle(selectedVehicle) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("homeEditVehicle:${selectedVehicle.id}"),
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_edit),
                                    contentDescription = stringResource(R.string.vehicle_edit),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.testTag("homeCurrentMileage")) {
                                if (mileage != null) {
                                    Text(
                                        stringResource(R.string.home_current_mileage),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = mileageFormatted,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("homeMileageValue"),
                                )
                            }
                            TextButton(
                                onClick = viewModel::openOdometerSheet,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("homeUpdateMileage"),
                            ) {
                                Text(
                                    text = stringResource(R.string.home_log_mileage),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                // Fuel & Cost Metric Cards
                val historyEntries = state.allHistory ?: state.recentHistory.orEmpty()
                val avgConsumption = remember(historyEntries, selectedVehicle.id) {
                    FuelConsumptionCalculator.calculateAverageL100Km(
                        entries = historyEntries,
                        targetVehicleId = selectedVehicle.id,
                    )
                }
                val displayedMonth = month
                val report = monthlyReport?.takeIf { it.first == selectedVehicle.id }?.second
                val currentMonthTotal = displayedMonth?.let { m ->
                    report?.months?.firstOrNull { it.month == m }?.totals?.totalRials
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val consumptionValue = if (avgConsumption != null) {
                        "${String.format(Locale.US, "%.1f", avgConsumption).replace('.', '٫').withPersianDigits()} ${stringResource(R.string.trip_unit_liter)}"
                    } else {
                        stringResource(R.string.home_consumption_insufficient)
                    }
                    val consumptionSubtitle = if (avgConsumption != null) {
                        stringResource(R.string.home_consumption_unit)
                    } else {
                        null
                    }
                    MetricCard(
                        title = stringResource(R.string.home_average_consumption),
                        value = consumptionValue,
                        subtitle = consumptionSubtitle,
                        icon = ImageVector.vectorResource(R.drawable.ic_fuel),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("homeConsumptionCard"),
                    )

                    val costValue = if (displayedMonth != null) {
                        if (report == null) {
                            stringResource(R.string.dashboard_loading)
                        } else {
                            stringResource(
                                R.string.dashboard_toman,
                                Money.formatTomanTotal(currentMonthTotal ?: BigInteger.ZERO),
                            )
                        }
                    } else {
                        stringResource(R.string.dashboard_current_unsupported)
                    }
                    MetricCard(
                        title = stringResource(R.string.home_monthly_expense),
                        value = costValue,
                        subtitle = displayedMonth?.let { JalaliDates.formatMonth(it) },
                        icon = ImageVector.vectorResource(R.drawable.ic_chart),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClickLabel = stringResource(R.string.home_full_report)) {
                                onOpenDashboard(selectedVehicle, displayedMonth)
                            }
                            .testTag("homeOpenDashboard"),
                        valueTestTag = "homeMonthTotal",
                    )
                }

                // Quick Action Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("homeQuickActionChips")
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HomeQuickActionChip(
                        label = stringResource(R.string.home_quick_overview),
                        iconRes = R.drawable.ic_chart,
                        onClick = { onOpenDashboard(selectedVehicle, displayedMonth) },
                        testTag = "homeChip:overview",
                    )
                    HomeQuickActionChip(
                        label = stringResource(R.string.home_quick_monthly),
                        iconRes = R.drawable.ic_calendar,
                        onClick = { onOpenDashboard(selectedVehicle, displayedMonth) },
                        testTag = "homeChip:monthly",
                    )
                    HomeQuickActionChip(
                        label = stringResource(R.string.home_all_history),
                        iconRes = R.drawable.ic_book,
                        onClick = { onOpenHistory(selectedVehicle) },
                        testTag = "homeChip:history",
                    )
                    if (onOpenReminders != null) {
                        HomeQuickActionChip(
                            label = stringResource(R.string.reminders_title),
                            iconRes = R.drawable.ic_bell,
                            onClick = { onOpenReminders(selectedVehicle) },
                            testTag = "homeChip:reminders",
                        )
                    }
                }

                // Active Reminders / Alerts Section
                dueReminders?.firstOrNull()?.let { due ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenReminders?.invoke(selectedVehicle) }
                            .testTag("homeDue:${due.id}"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, semanticColors.warningEdge),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularIconBadge(
                                icon = ImageVector.vectorResource(R.drawable.ic_bell),
                                backgroundColor = semanticColors.warningSurface,
                                iconColor = semanticColors.warningEdge,
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
                                        text = due.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    StatusPill(
                                        text = stringResource(R.string.reminder_status_due),
                                        containerColor = semanticColors.warningSurface,
                                        contentColor = semanticColors.onWarning,
                                        modifier = Modifier.testTag("homeReminderStatusPill"),
                                    )
                                }
                                ReminderThresholdText(due)
                                Text(
                                    text = stringResource(R.string.home_view_reminders),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                // Recent Entries Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.ledger_recent),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        TextButton(
                            onClick = { onOpenHistory(selectedVehicle) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("homeOpenHistory"),
                        ) {
                            Text(
                                text = stringResource(R.string.home_all_history),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    val recent = state.recentHistory
                    if (recent == null || recent.isEmpty()) {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        ) {
                            Text(
                                stringResource(R.string.ledger_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                recent.forEachIndexed { index, entry ->
                                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    RecentEntryRow(
                                        entry = entry,
                                        onClick = {
                                            if (onOpenEntry != null) onOpenEntry(selectedVehicle, entry.id)
                                            else onOpenHistory(selectedVehicle)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showVehicleSavedBanner) {
                NoticeBanner(
                    title = stringResource(R.string.vehicle_saved),
                    tone = NoticeTone.Success,
                    modifier = Modifier.testTag("homeVehicleSavedBanner"),
                )
            }

            state.odometerFeedbackRes?.let { feedbackRes ->
                NoticeBanner(
                    title = stringResource(feedbackRes),
                    tone = NoticeTone.Success,
                    modifier = Modifier.testTag("homeOdometerSavedBanner"),
                )
            }

            if (showStaleBackupBanner && onOpenSettings != null) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("homeStaleBackupBanner"),
                    shape = VehicleLogShapes.large,
                    colors = CardDefaults.outlinedCardColors(containerColor = semanticColors.warningSurface),
                    border = BorderStroke(1.dp, semanticColors.warningEdge),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.backup_stale_banner_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = semanticColors.onWarning,
                        )
                        Text(
                            text = stringResource(R.string.backup_stale_banner_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("homeStaleBackupAction"),
                                shape = VehicleLogShapes.medium,
                            ) {
                                Text(stringResource(R.string.backup_stale_action))
                            }
                            TextButton(
                                onClick = onDismissStaleBackup,
                                modifier = Modifier.heightIn(min = 48.dp).testTag("homeStaleBackupDismiss"),
                            ) {
                                Text(stringResource(R.string.backup_stale_dismiss))
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
private fun RecentEntryRow(entry: HistoryEntry, onClick: () -> Unit) {
    val categoryLabelRes = when (entry.category) {
        "fuel" -> R.string.ledger_fuel
        "oil_change" -> R.string.ledger_oil_change
        "repair" -> R.string.ledger_repair
        "insurance" -> R.string.ledger_insurance
        "wash" -> R.string.ledger_wash
        "parts" -> R.string.ledger_parts
        else -> R.string.ledger_other
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("homeRecent:${entry.id}"),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(categoryLabelRes),
                    modifier = Modifier.padding(end = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    entry.amountRials?.let { stringResource(R.string.dashboard_toman, Money.formatTomanInput(it)) }
                        ?: stringResource(R.string.ledger_unpriced),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                JalaliDates.formatGregorian(entry.occurredOn) ?: "\u2066${entry.occurredOn.withPersianDigits()}\u2069",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeQuickActionChip(
    label: String,
    iconRes: Int,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

