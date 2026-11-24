package org.unishiasy.vehiclelog.ui.ledger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.local.HistoryEntry
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.Money
import org.unishiasy.vehiclelog.domain.withPersianDigits
import org.unishiasy.vehiclelog.ui.components.StatusPill
import org.unishiasy.vehiclelog.ui.theme.LocalVehicleLogSemanticColors
import org.unishiasy.vehiclelog.ui.theme.SheetShape
import org.unishiasy.vehiclelog.ui.theme.VehicleLogShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntryDetailSheet(
    entry: HistoryEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val semanticColors = LocalVehicleLogSemanticColors.current
    val fontScale = LocalDensity.current.fontScale

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = SheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.testTag("entryDetailSheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(semanticColors.wash),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = semanticColors.action,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = stringResource(categoryLabelRes),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (entry.isFullTank) {
                        StatusPill(
                            text = stringResource(R.string.ledger_full_tank),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.testTag("entryDetailFullTank"),
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp).testTag("entryDetailClose"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.date_picker_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            HorizontalDivider(color = semanticColors.rule)

            DetailFieldRow(
                label = stringResource(R.string.detail_amount_label),
                value = if (entry.amountRials != null) {
                    stringResource(R.string.dashboard_toman, Money.formatTomanInput(entry.amountRials))
                } else {
                    stringResource(R.string.ledger_unpriced)
                },
                emphasize = entry.amountRials != null,
            )

            val dateStr = JalaliDates.formatGregorian(entry.occurredOn) ?: entry.occurredOn.withPersianDigits()
            DetailFieldRow(
                label = stringResource(R.string.detail_date_label),
                value = dateStr,
            )

            entry.fuelVolumeMl?.let { volumeMl ->
                val liters = BigDecimal.valueOf(volumeMl, 3).stripTrailingZeros().toPlainString()
                    .replace('.', '٫').withPersianDigits()
                DetailFieldRow(
                    label = stringResource(R.string.ledger_volume),
                    value = stringResource(R.string.ledger_liters, "\u2066$liters\u2069"),
                )
            }

            entry.stationName?.takeIf { it.isNotBlank() }?.let { station ->
                DetailFieldRow(
                    label = stringResource(R.string.ledger_station_name),
                    value = station,
                    valueModifier = Modifier.testTag("entryDetailStationName"),
                )
            }

            entry.linkedOdometerKm?.let { km ->
                DetailFieldRow(
                    label = stringResource(R.string.detail_mileage_label),
                    value = stringResource(R.string.ledger_kilometers, "\u2066${km.toString().withPersianDigits()}\u2069"),
                )
            }

            entry.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.detail_notes_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.ContentOrRtl),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            HorizontalDivider(color = semanticColors.rule, modifier = Modifier.padding(top = 4.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stackActions = fontScale >= 1.35f || maxWidth < 360.dp
                val editButton: @Composable (Modifier) -> Unit = { btnMod ->
                    Button(
                        onClick = onEdit,
                        modifier = btnMod.heightIn(min = 48.dp).testTag("entryDetailEdit"),
                        shape = VehicleLogShapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.ledger_edit),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
                val deleteButton: @Composable (Modifier) -> Unit = { btnMod ->
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = btnMod.heightIn(min = 48.dp).testTag("entryDetailDelete"),
                        shape = VehicleLogShapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    ) {
                        Text(
                            text = stringResource(R.string.ledger_delete_action),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (stackActions) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        editButton(Modifier.fillMaxWidth())
                        deleteButton(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        editButton(Modifier.weight(1f))
                        deleteButton(Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailFieldRow(
    label: String,
    value: String,
    emphasize: Boolean = false,
    modifier: Modifier = Modifier,
    valueModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = valueModifier,
            style = if (emphasize) {
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            },
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
