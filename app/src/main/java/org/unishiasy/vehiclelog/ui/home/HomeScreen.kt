package org.unishiasy.vehiclelog.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.local.HistoryEntry
import org.unishiasy.vehiclelog.data.local.ReminderEntity
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.Money
import org.unishiasy.vehiclelog.domain.withPersianDigits
import org.unishiasy.vehiclelog.ui.components.VehicleSwitcherSheet
import org.unishiasy.vehiclelog.ui.reminders.ReminderThresholdText
import org.unishiasy.vehiclelog.ui.theme.LocalVehicleLogSemanticColors
import org.unishiasy.vehiclelog.ui.theme.VehicleLogShapes
import org.unishiasy.vehiclelog.ui.vehicle.toPersianYear

@Composable
internal fun HomeScreen(
    viewModel: HomeViewModel,
    onQuickAdd: (VehicleEntity) -> Unit,
    onOpenHistory: (VehicleEntity) -> Unit,
    onOpenDashboard: (VehicleEntity) -> Unit,
    onAddVehicle: () -> Unit,
    onEditVehicle: (VehicleEntity) -> Unit,
    onOpenReminders: ((VehicleEntity) -> Unit)? = null,
    dueReminders: List<ReminderEntity>? = null,
    remindersFailed: Boolean = false,
    onRetryReminders: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenGeneralExpense: ((VehicleEntity) -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val vehicles = state.vehicles.orEmpty()
    val selectedVehicle = state.selectedVehicle
    val semanticColors = LocalVehicleLogSemanticColors.current
    var showVehicleSwitcher by rememberSaveable { mutableStateOf(false) }

    if (showVehicleSwitcher && selectedVehicle != null) {
        VehicleSwitcherSheet(
            vehicles = vehicles,
            selectedVehicleId = selectedVehicle.id,
            onSelectVehicle = viewModel::selectVehicle,
            onManageVehicles = onAddVehicle,
            onDismiss = { showVehicleSwitcher = false },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("homeScreen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Top App Bar
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(semanticColors.wash),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_book),
                            contentDescription = null,
                            tint = semanticColors.action,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        stringResource(R.string.app_name),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                if (onOpenSettings != null) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("homeOpenSettings"),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.backup_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            // Multi-Vehicle Selector Cards (Visible when multiple vehicles are registered)
            if (vehicles.size > 1) {
                Text(
                    stringResource(R.string.home_switch_vehicle),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Column(
                    modifier = Modifier.fillMaxWidth().testTag("homeVehicleSelector"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (vehicle in vehicles) {
                        val isSelected = vehicle.id == selectedVehicle?.id
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = { viewModel.selectVehicle(vehicle.id) },
                                )
                                .testTag("homeVehicle:${vehicle.id}"),
                            shape = VehicleLogShapes.medium,
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = if (isSelected) CardDefaults.outlinedCardColors(
                                containerColor = semanticColors.wash.copy(alpha = 0.5f)
                            ) else CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
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
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            vehicle.displayLabel,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                textDirection = TextDirection.ContentOrRtl,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            ),
                                        )
                                        val calendarRes = when (vehicle.productionYearCalendar) {
                                            "solar_hijri" -> R.string.vehicle_solar_hijri
                                            "gregorian" -> R.string.vehicle_gregorian
                                            else -> null
                                        }
                                        if (vehicle.productionYear != null && calendarRes != null) {
                                            Text(
                                                stringResource(
                                                    R.string.vehicle_year_summary,
                                                    vehicle.productionYear.toPersianYear(),
                                                    stringResource(calendarRes),
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                val editDescription = "${stringResource(R.string.vehicle_edit)} ${vehicle.displayLabel}"
                                IconButton(
                                    onClick = { onEditVehicle(vehicle) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("homeEditVehicle:${vehicle.id}")
                                        .semantics { contentDescription = editDescription },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_edit),
                                        contentDescription = editDescription,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Primary Vehicle Readout Card
            if (selectedVehicle != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("homeSelectedVehicleCard"),
                    shape = VehicleLogShapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Vehicle Identity Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(semanticColors.wash),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_car),
                                        contentDescription = null,
                                        tint = semanticColors.action,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        selectedVehicle.displayLabel,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            textDirection = TextDirection.ContentOrRtl,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("homeSelectedVehicleName"),
                                    )
                                    val calendarRes = when (selectedVehicle.productionYearCalendar) {
                                        "solar_hijri" -> R.string.vehicle_solar_hijri
                                        "gregorian" -> R.string.vehicle_gregorian
                                        else -> null
                                    }
                                    if (selectedVehicle.productionYear != null && calendarRes != null) {
                                        Text(
                                            stringResource(
                                                R.string.vehicle_year_summary,
                                                selectedVehicle.productionYear.toPersianYear(),
                                                stringResource(calendarRes),
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            val editDesc = "${stringResource(R.string.vehicle_edit)} ${selectedVehicle.displayLabel}"
                            IconButton(
                                onClick = { onEditVehicle(selectedVehicle) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("homeEditVehicle:${selectedVehicle.id}")
                                    .semantics { contentDescription = editDesc },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_edit),
                                    contentDescription = editDesc,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        // Odometer Inset Panel
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(semanticColors.odometerContainer)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("homeCurrentMileage"),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    stringResource(R.string.home_current_mileage),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = semanticColors.odometerMuted,
                                )
                                val mileage = state.selectedVehicleMileage
                                if (mileage != null) {
                                    Text(
                                        stringResource(
                                            R.string.ledger_kilometers,
                                            "\u2066${mileage.toString().withPersianDigits()}\u2069",
                                        ),
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = semanticColors.onOdometer,
                                        ),
                                        modifier = Modifier.testTag("homeMileageValue"),
                                    )
                                } else {
                                    Text(
                                        stringResource(R.string.ledger_mileage_reference_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = semanticColors.odometerMuted,
                                        modifier = Modifier.testTag("homeMileageValue"),
                                    )
                                }
                            }
                        }
                    }
                }

                // Paired Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Primary Action: Fuel Entry (High Contrast Cobalt)
                    Button(
                        onClick = { onQuickAdd(selectedVehicle) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("homeQuickAdd"),
                        shape = VehicleLogShapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
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

                    // Secondary Action: Other Expense / Service Entry
                    OutlinedButton(
                        onClick = {
                            if (onOpenGeneralExpense != null) {
                                onOpenGeneralExpense(selectedVehicle)
                            } else {
                                onQuickAdd(selectedVehicle)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("homeAddExpense"),
                        shape = VehicleLogShapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                // Due Service Banner (Warning Card with 3dp Amber Edge)
                if (dueReminders != null && dueReminders.isNotEmpty()) {
                    for (due in dueReminders) {
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
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        }
                                    }
                                    ReminderThresholdText(due)
                                }
                            }
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
                                .heightIn(min = 40.dp)
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
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (entry in recent) {
                                RecentEntryRow(entry, onClick = { onOpenHistory(selectedVehicle) })
                            }
                        }
                    }
                }

                // Bottom Cards: Monthly Report & Reminders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .clickable { onOpenDashboard(selectedVehicle) }
                            .testTag("homeOpenDashboard"),
                        shape = VehicleLogShapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(semanticColors.wash),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_chart),
                                    contentDescription = null,
                                    tint = semanticColors.action,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Text(
                                stringResource(R.string.home_notebook_report),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    if (onOpenReminders != null) {
                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .clickable { onOpenReminders(selectedVehicle) }
                                .testTag("homeOpenReminders"),
                            shape = VehicleLogShapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(semanticColors.wash),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_bell),
                                        contentDescription = null,
                                        tint = semanticColors.action,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Text(
                                    stringResource(R.string.home_notebook_reminders),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Footer
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

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
            .clickable(onClick = onClick),
        shape = VehicleLogShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                        "بدون مبلغ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (entry.fuelVolumeMl != null && entry.fuelVolumeMl > 0) {
                    val liters = String.format(java.util.Locale.US, "%.1f", entry.fuelVolumeMl / 1000.0).withPersianDigits()
                    Text(
                        stringResource(R.string.ledger_liters, liters),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
