package org.unishiasy.vehiclelog.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextOverflow
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.delay
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.local.HistoryEntry
import org.unishiasy.vehiclelog.data.local.ReminderEntity
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.JalaliMonth
import org.unishiasy.vehiclelog.domain.Money
import org.unishiasy.vehiclelog.domain.withPersianDigits
import org.unishiasy.vehiclelog.ui.components.LedgerTopBar
import org.unishiasy.vehiclelog.ui.components.LedgerSettingsAction
import org.unishiasy.vehiclelog.ui.components.NoticeBanner
import org.unishiasy.vehiclelog.ui.components.NoticeTone
import org.unishiasy.vehiclelog.ui.components.VehicleSwitcherSheet
import org.unishiasy.vehiclelog.ui.ledger.OdometerSheet
import org.unishiasy.vehiclelog.ui.reminders.ReminderThresholdText
import org.unishiasy.vehiclelog.ui.theme.LocalVehicleLogSemanticColors
import org.unishiasy.vehiclelog.ui.theme.VehicleLogShapes

@Composable
internal fun HomeScreen(
    viewModel: HomeViewModel,
    state: HomeUiState,
    onQuickAdd: (VehicleEntity) -> Unit,
    onOpenHistory: (VehicleEntity) -> Unit,
    onOpenDashboard: (VehicleEntity, JalaliMonth?) -> Unit,
    onAddVehicle: () -> Unit,
    onEditVehicle: (VehicleEntity) -> Unit,
    onOpenReminders: ((VehicleEntity) -> Unit)? = null,
    dueReminders: List<ReminderEntity>? = null,
    remindersFailed: Boolean = false,
    onRetryReminders: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenGeneralExpense: ((VehicleEntity) -> Unit)? = null,
    onManageVehicles: (() -> Unit)? = null,
    onOpenEntry: ((VehicleEntity, String) -> Unit)? = null,
    showVehicleSavedBanner: Boolean = false,
    onDismissVehicleSaved: () -> Unit = {},
    showStaleBackupBanner: Boolean = false,
    onDismissStaleBackup: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
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
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("homeScreen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {


            if (selectedVehicle != null) {
                val mileage = state.selectedVehicleMileage
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().testTag("homeSelectedVehicleCard"),
                    shape = VehicleLogShapes.medium,
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                                .heightIn(min = 48.dp)
                                .clickable(onClickLabel = stringResource(R.string.vehicle_select_title)) {
                                    showVehicleSwitcher = true
                                }
                                .testTag("homeOpenVehicleSwitcher")
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                selectedVehicle.displayLabel,
                                modifier = Modifier.testTag("homeSelectedVehicleName"),
                                style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.ContentOrRtl),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Box(Modifier.testTag("homeCurrentMileage")) {
                                Text(
                                    if (mileage == null) stringResource(R.string.ledger_mileage_reference_empty)
                                    else stringResource(R.string.ledger_kilometers, "\u2066${mileage.toString().withPersianDigits()}\u2069"),
                                    modifier = Modifier.testTag("homeMileageValue"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = viewModel::openOdometerSheet, modifier = Modifier.testTag("homeUpdateMileage")) {
                            Icon(
                                painterResource(R.drawable.ic_car),
                                contentDescription = stringResource(R.string.home_update_mileage),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(
                            onClick = { onEditVehicle(selectedVehicle) },
                            modifier = Modifier.testTag("homeEditVehicle:${selectedVehicle.id}"),
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_edit),
                                contentDescription = stringResource(R.string.vehicle_edit),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Paired Action Buttons (responsive stack at high fontScale or narrow widths)
                val fontScale = LocalDensity.current.fontScale
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val stackActions = fontScale >= 1.35f || maxWidth < 360.dp
                    val fuelButton: @Composable (Modifier) -> Unit = { btnModifier ->
                        Button(
                            onClick = { onQuickAdd(selectedVehicle) },
                            modifier = btnModifier
                                .heightIn(min = 48.dp)
                                .testTag("homeQuickAdd"),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_fuel),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    stringResource(R.string.home_log_fuel),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                    val expenseButton: @Composable (Modifier) -> Unit = { btnModifier ->
                        FilledTonalButton(
                            onClick = {
                                if (onOpenGeneralExpense != null) {
                                    onOpenGeneralExpense(selectedVehicle)
                                } else {
                                    onQuickAdd(selectedVehicle)
                                }
                            },
                            modifier = btnModifier
                                .heightIn(min = 48.dp)
                                .testTag("homeAddExpense"),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_plus),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    stringResource(R.string.home_log_expense),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                    if (stackActions) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            fuelButton(Modifier.fillMaxWidth())
                            expenseButton(Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            fuelButton(Modifier.weight(1f))
                            expenseButton(Modifier.weight(1f))
                        }
                    }
                }

                dueReminders?.firstOrNull()?.let { due ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenReminders?.invoke(selectedVehicle) }
                            .testTag("homeDue:${due.id}"),
                        shape = VehicleLogShapes.medium,
                        colors = CardDefaults.cardColors(containerColor = semanticColors.warningSurface),
                        border = BorderStroke(1.5.dp, semanticColors.warningEdge),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_bell),
                                contentDescription = null,
                                tint = semanticColors.onWarning,
                                modifier = Modifier.size(24.dp),
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        due.title,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            textDirection = TextDirection.ContentOrRtl,
                                        ),
                                        color = semanticColors.onWarning,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(semanticColors.warningEdge)
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            stringResource(R.string.reminders_status_due_badge),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = semanticColors.onWarningEdge,
                                        )
                                    }
                                }
                                ReminderThresholdText(due)
                            }
                        }
                    }
                }

                val displayedMonth = month
                val report = monthlyReport?.takeIf { it.first == selectedVehicle.id }?.second
                OutlinedCard(
                    onClick = { onOpenDashboard(selectedVehicle, displayedMonth) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("homeOpenDashboard"),
                    shape = VehicleLogShapes.medium,
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.home_notebook_report),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (displayedMonth != null) {
                            Text(JalaliDates.formatMonth(displayedMonth), style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (report == null) stringResource(R.string.dashboard_loading)
                                else stringResource(
                                    R.string.dashboard_toman,
                                    Money.formatTomanTotal(report.months.firstOrNull { it.month == displayedMonth }?.totals?.totalRials ?: BigInteger.ZERO),
                                ),
                                modifier = Modifier.testTag("homeMonthTotal"),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        } else {
                            Text(stringResource(R.string.dashboard_current_unsupported))
                        }
                    }
                }

                // Recent Entries Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.ledger_recent),
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
                                stringResource(R.string.home_all_history),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    val recent = state.recentHistory
                    if (recent == null || recent.isEmpty()) {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = VehicleLogShapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Text(
                                stringResource(R.string.ledger_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (entry in recent) {
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

                if (onOpenReminders != null) {
                    TextButton(
                        onClick = { onOpenReminders(selectedVehicle) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("homeOpenReminders"),
                    ) { Text(stringResource(R.string.home_notebook_reminders)) }
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

            // Bottom Navigation Footer
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onAddVehicle,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("homeAddVehicle"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_plus),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.vehicle_add))
                }

                if (onOpenSettings != null) {
                    TextButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("homeOpenSettingsBottom"),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.backup_settings))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentEntryRow(entry: HistoryEntry, onClick: () -> Unit) {
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("homeRecent:${entry.id}"),
        shape = VehicleLogShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(semanticColors.wash),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = semanticColors.action,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(categoryLabelRes),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        JalaliDates.formatGregorian(entry.occurredOn) ?: entry.occurredOn.withPersianDigits(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
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
                if (entry.fuelVolumeMl != null && entry.fuelVolumeMl > 0) {
                    val liters = BigDecimal.valueOf(entry.fuelVolumeMl, 3)
                        .stripTrailingZeros()
                        .toPlainString()
                        .replace('.', '٫')
                        .withPersianDigits()
                    Text(
                        stringResource(R.string.ledger_liters, "\u2066$liters\u2069"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
