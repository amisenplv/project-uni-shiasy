package org.unishiasy.vehiclelog.ui.ledger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import java.math.BigInteger
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.ReportDateIssue
import org.unishiasy.vehiclelog.data.UnassignedReportEntry
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.JalaliMonth
import org.unishiasy.vehiclelog.domain.Money
import org.unishiasy.vehiclelog.domain.withPersianDigits
import org.unishiasy.vehiclelog.ui.components.LedgerTopBar
import org.unishiasy.vehiclelog.ui.theme.LocalVehicleLogSemanticColors
import org.unishiasy.vehiclelog.ui.theme.VehicleLogShapes

@Composable
internal fun MonthlyDashboardScreen(
    vehicleLabel: String,
    state: MonthlyDashboardState,
    onSelect: (JalaliMonth) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val report = state.report
    val monthReport = state.selectedReport
    val categories = monthReport?.categories.orEmpty()
    val semanticColors = LocalVehicleLogSemanticColors.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LedgerTopBar(
                title = stringResource(R.string.dashboard_title),
                subtitle = vehicleLabel,
                subtitleModifier = Modifier.testTag("dashboardVehicle"),
                onBack = onBack,
                backLabel = stringResource(R.string.dashboard_back),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .padding(horizontal = 20.dp)
                .testTag("monthlyDashboard"),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {


            when {
                state.failed -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.dashboard_failed),
                            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            color = MaterialTheme.colorScheme.error,
                        )
                        OutlinedButton(onRetry, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.vehicle_retry))
                        }
                    }
                }
                report == null -> item {
                    Text(
                        stringResource(R.string.dashboard_loading),
                        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                else -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.currentMonth == null) Text(stringResource(R.string.dashboard_current_unsupported))
                            if (state.availableMonths.isNotEmpty()) MonthSelector(state, onSelect)
                        }
                    }
                    if (state.selectedMonth != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        stringResource(R.string.dashboard_total_label),
                                        Modifier.semantics { heading() },
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        tomanLabel(monthReport?.totals?.totalRials ?: BigInteger.ZERO),
                                        Modifier.testTag("dashboardTotal"),
                                        style = MaterialTheme.typography.displayLarge,
                                    )
                                    HorizontalDivider()
                                    Text(
                                        stringResource(R.string.dashboard_entry_count, (monthReport?.totals?.entryCount ?: 0).persian()),
                                        Modifier.testTag("dashboardEntryCount"),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        stringResource(R.string.dashboard_unpriced_count, (monthReport?.totals?.unpricedEntryCount ?: 0).persian()),
                                        Modifier.testTag("dashboardUnpricedCount"),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                        if (monthReport == null) item {
                            Text(stringResource(R.string.dashboard_empty))
                        }
                    }
                    if (categories.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.dashboard_categories),
                                Modifier.semantics { heading() },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        val totalMonthRials = monthReport?.totals?.totalRials ?: BigInteger.ZERO
                        items(
                            items = orderedReportCategories(categories.keys),
                            key = { "category:$it" },
                        ) { key ->
                            val totals = categories.getValue(key)
                            val fraction = if (totalMonthRials > BigInteger.ZERO) {
                                (totals.totalRials.toFloat() / totalMonthRials.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            val iconRes = when (key) {
                                "fuel" -> R.drawable.ic_fuel
                                "oil_change" -> R.drawable.ic_drop
                                "repair", "parts" -> R.drawable.ic_tool
                                else -> R.drawable.ic_file
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = VehicleLogShapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, semanticColors.rule),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                                            Text(
                                                categoryLabel(key),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                        Text(
                                            tomanLabel(totals.totalRials),
                                            Modifier.testTag("dashboardCategory:$key"),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                            ),
                                        )
                                    }
                                    if (fraction > 0f) {
                                        LinearProgressIndicator(
                                            progress = { fraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = semanticColors.wash,
                                            strokeCap = StrokeCap.Round,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (report.unassignedEntries.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    stringResource(R.string.dashboard_unassigned),
                                    Modifier.semantics { heading() },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                )
                                Text(
                                    stringResource(R.string.dashboard_unassigned_explanation),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(items = report.unassignedEntries, key = { "unassigned:${it.entry.id}" }) { unassigned ->
                            UnassignedRow(unassigned)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(state: MonthlyDashboardState, onSelect: (JalaliMonth) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val label = state.selectedMonth?.let(JalaliDates::formatMonth) ?: stringResource(R.string.dashboard_choose_month)
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("dashboardMonth"),
            shape = VehicleLogShapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
                Text("▼", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            for (month in state.availableMonths) {
                val isCurrent = month == state.selectedMonth
                DropdownMenuItem(
                    text = {
                        Text(
                            JalaliDates.formatMonth(month),
                            Modifier.semantics { selected = isCurrent },
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(month)
                    },
                    modifier = Modifier.testTag("dashboardMonth:${month.year}-${month.month}"),
                )
            }
        }
    }
}

@Composable
private fun UnassignedRow(unassigned: UnassignedReportEntry) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().testTag("dashboardUnassigned:${unassigned.entry.id}"),
        shape = VehicleLogShapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(categoryLabel(unassigned.entry.category), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.dashboard_original_gregorian, "\u2066${unassigned.entry.occurredOn}\u2069"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val issueText = when (unassigned.reason) {
                ReportDateIssue.OutsideSupportedJalaliRange -> R.string.dashboard_date_outside_range
                ReportDateIssue.InvalidGregorianDate -> R.string.dashboard_date_malformed
            }
            Text(stringResource(issueText), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun categoryLabel(key: String): String = when (key) {
    "fuel" -> stringResource(R.string.ledger_fuel)
    "oil_change" -> stringResource(R.string.ledger_oil_change)
    "repair" -> stringResource(R.string.ledger_repair)
    "insurance" -> stringResource(R.string.ledger_insurance)
    "wash" -> stringResource(R.string.ledger_wash)
    "parts" -> stringResource(R.string.ledger_parts)
    "other" -> stringResource(R.string.ledger_other)
    else -> stringResource(R.string.dashboard_unknown_category, key)
}

@Composable
private fun tomanLabel(rials: BigInteger): String =
    stringResource(R.string.dashboard_toman, Money.formatTomanTotal(rials))

private fun Int.persian(): String = toString().withPersianDigits()
