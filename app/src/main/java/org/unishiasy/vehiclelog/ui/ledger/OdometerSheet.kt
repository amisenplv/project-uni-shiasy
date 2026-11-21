package org.unishiasy.vehiclelog.ui.ledger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.local.OdometerReadingEntity
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.withPersianDigits
import org.unishiasy.vehiclelog.ui.components.DismissibleHeading
import org.unishiasy.vehiclelog.ui.components.JalaliDatePickerDialog
import org.unishiasy.vehiclelog.ui.home.OdometerDraft
import org.unishiasy.vehiclelog.ui.theme.LocalVehicleLogSemanticColors
import org.unishiasy.vehiclelog.ui.theme.SheetShape
import org.unishiasy.vehiclelog.ui.theme.VehicleLogShapes

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun OdometerSheet(
    draft: OdometerDraft,
    standaloneReadings: List<OdometerReadingEntity>,
    currentMileage: Long?,
    todayDate: LocalDate,
    onKilometersChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onSave: () -> Unit,
    onEditReading: (OdometerReadingEntity) -> Unit,
    onRequestDeleteReading: (String) -> Unit,
    onConfirmDeleteReading: () -> Unit,
    onCancelDeleteReading: () -> Unit,
    onDismiss: () -> Unit,
) {
    val semanticColors = LocalVehicleLogSemanticColors.current
    val focusManager = LocalFocusManager.current
    var showDatePicker by rememberSaveable(draft.id) { mutableStateOf(false) }

    val todayJalali = JalaliDates.formatJalaliInput(todayDate.toString())
    val yesterdayJalali = JalaliDates.formatJalaliInput(todayDate.minusDays(1).toString())

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialDateText = draft.date,
            todayDate = todayDate,
            onSelectDate = { selected ->
                onDateChange(selected)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (draft.pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { if (!draft.saving) onCancelDeleteReading() },
            title = {
                DismissibleHeading(
                    title = stringResource(R.string.odometer_delete_confirm_title),
                    onClose = onCancelDeleteReading,
                    closeModifier = Modifier.testTag("odometerCancelDelete"),
                    closeEnabled = !draft.saving,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Text(
                    stringResource(R.string.odometer_delete_confirm_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmDeleteReading,
                    enabled = !draft.saving,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("odometerConfirmDelete"),
                    shape = VehicleLogShapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(stringResource(R.string.ledger_delete))
                }
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = { if (!draft.saving) onDismiss() },
        shape = SheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.testTag("odometerSheet"),
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
                Text(
                    text = stringResource(if (draft.editing) R.string.odometer_edit_title else R.string.odometer_sheet_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = onDismiss,
                    enabled = !draft.saving,
                    modifier = Modifier.size(48.dp).testTag("odometerClose"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.app_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (currentMileage != null) {
                Text(
                    text = stringResource(
                        R.string.ledger_mileage_reference,
                        "\u2066${currentMileage.toString().withPersianDigits()}\u2069",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = semanticColors.rule)

            OutlinedTextField(
                value = draft.kilometers,
                onValueChange = onKilometersChange,
                modifier = Modifier.fillMaxWidth().testTag("odometerKilometersInput"),
                label = { Text(stringResource(R.string.odometer_km_label)) },
                singleLine = true,
                enabled = !draft.saving,
                isError = draft.kmError != null,
                shape = VehicleLogShapes.medium,
                textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                supportingText = {
                    val support = draft.kmError ?: R.string.odometer_km_hint
                    Text(stringResource(support))
                },
            )

            OutlinedTextField(
                value = draft.date,
                onValueChange = onDateChange,
                modifier = Modifier.fillMaxWidth().testTag("odometerDateInput"),
                label = { Text(stringResource(R.string.odometer_date_label)) },
                singleLine = true,
                enabled = !draft.saving,
                isError = draft.dateError != null,
                shape = VehicleLogShapes.medium,
                textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                trailingIcon = {
                    IconButton(
                        onClick = { showDatePicker = true },
                        enabled = !draft.saving,
                        modifier = Modifier.size(48.dp).testTag("odometerDatePickerOpen"),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar),
                            contentDescription = stringResource(R.string.date_picker_open),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                supportingText = {
                    val support = draft.dateError ?: R.string.ledger_date_example
                    Text(stringResource(support))
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
                            onClick = { onDateChange(todayJalali) },
                            enabled = !draft.saving,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("odometerDateToday"),
                            shape = VehicleLogShapes.medium,
                            label = { Text(stringResource(R.string.date_chip_today)) },
                        )
                    }
                    if (yesterdayJalali != null) {
                        FilterChip(
                            selected = draft.date.trim() == yesterdayJalali,
                            onClick = { onDateChange(yesterdayJalali) },
                            enabled = !draft.saving,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("odometerDateYesterday"),
                            shape = VehicleLogShapes.medium,
                            label = { Text(stringResource(R.string.date_chip_yesterday)) },
                        )
                    }
                }
            }

            draft.error?.let { errRes ->
                Text(
                    text = stringResource(errRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag("odometerError"),
                )
            }

            Button(
                onClick = onSave,
                enabled = !draft.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("odometerSave"),
                shape = VehicleLogShapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(if (draft.saving) R.string.odometer_saving else R.string.odometer_save),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }

            HorizontalDivider(color = semanticColors.rule, modifier = Modifier.padding(top = 4.dp))

            Text(
                text = stringResource(R.string.odometer_standalone_history),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (standaloneReadings.isEmpty()) {
                Text(
                    text = stringResource(R.string.odometer_standalone_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (reading in standaloneReadings) {
                        val dateText = JalaliDates.formatGregorian(reading.occurredOn) ?: reading.occurredOn.withPersianDigits()
                        val kmText = stringResource(
                            R.string.ledger_kilometers,
                            "\u2066${reading.kilometers.toString().withPersianDigits()}\u2069",
                        )
                        val editLabel = "${stringResource(R.string.odometer_edit_action)} $kmText"
                        val deleteLabel = "${stringResource(R.string.ledger_delete)} $kmText"
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth().testTag("odometerRow:${reading.id}"),
                            shape = VehicleLogShapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = kmText,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = dateText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onEditReading(reading) },
                                        enabled = !draft.saving,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("odometerEdit:${reading.id}")
                                            .semantics { contentDescription = editLabel },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_edit),
                                            contentDescription = editLabel,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRequestDeleteReading(reading.id) },
                                        enabled = !draft.saving,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("odometerDelete:${reading.id}")
                                            .semantics { contentDescription = deleteLabel },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_close),
                                            contentDescription = deleteLabel,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
