package org.unishiasy.vehiclelog.ui.reminders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.ReminderState
import org.unishiasy.vehiclelog.data.local.ReminderEntity
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.withPersianDigits
import org.unishiasy.vehiclelog.ui.components.DismissibleHeading
import org.unishiasy.vehiclelog.ui.components.HeaderCloseButton
import org.unishiasy.vehiclelog.ui.components.LedgerTopBar
import org.unishiasy.vehiclelog.ui.components.JalaliDatePickerDialog
import org.unishiasy.vehiclelog.ui.theme.LocalVehicleLogSemanticColors
import org.unishiasy.vehiclelog.ui.theme.VehicleLogShapes

@Composable
internal fun RemindersScreen(
    vehicle: VehicleEntity,
    state: RemindersUiState,
    viewModel: RemindersViewModel,
    notificationOptions: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    newReminderRequestCount: Int = 0,
) {
    val semanticColors = LocalVehicleLogSemanticColors.current
    val list = state.list.takeIf { state.observedVehicleId == vehicle.id }
    val dueReminders = list?.due.orEmpty()
    val dueIds = dueReminders.mapTo(hashSetOf()) { it.id }
    val otherReminders = list?.rows.orEmpty().filterNot { it.id in dueIds }
    // Header and notification options always occupy one item each.
    val formIndex = 1 + dueReminders.size + if (dueReminders.isEmpty()) 0 else 1
    val listState = rememberLazyListState()
    val titleFocusRequester = remember { FocusRequester() }
    var pendingCancelId by rememberSaveable { mutableStateOf<String?>(null) }
    var handledCreateRequest by remember { mutableIntStateOf(newReminderRequestCount) }
    var formScrollRequest by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(newReminderRequestCount) {
        if (newReminderRequestCount != handledCreateRequest) {
            handledCreateRequest = newReminderRequestCount
            formScrollRequest = true
        }
    }

    LaunchedEffect(state.lastCompletedForRecurring?.id) {
        if (state.lastCompletedForRecurring != null && formScrollRequest != true) {
            formScrollRequest = false
        }
    }

    LaunchedEffect(formScrollRequest, formIndex) {
        val requestFocus = formScrollRequest ?: return@LaunchedEffect
        listState.animateScrollToItem(formIndex)
        if (requestFocus) titleFocusRequester.requestFocus()
        formScrollRequest = null
    }

    val reminderContent: @Composable (ReminderEntity, Boolean) -> Unit = { reminder, due ->
        ReminderCard(
            reminder = reminder,
            due = due,
            mileageMissing = list?.mileage == null,
            currentMileage = list?.mileage,
            state = state,
            viewModel = viewModel,
            onEdit = {
                viewModel.startEditing(reminder)
                formScrollRequest = true
            },
            onRequestCancel = { pendingCancelId = reminder.id },
        )
    }

    pendingCancelId?.let { cancelId ->
        val cancelTarget = list?.rows?.firstOrNull { it.id == cancelId }
        if (cancelTarget != null) {
            AlertDialog(
                onDismissRequest = { if (!state.writing) pendingCancelId = null },
                title = {
                    DismissibleHeading(
                        title = stringResource(R.string.reminder_cancel_confirm_title),
                        onClose = { pendingCancelId = null },
                        closeModifier = Modifier.testTag("reminderDismissCancel"),
                        closeEnabled = !state.writing,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                text = {
                    Text(
                        stringResource(R.string.reminder_cancel_confirm_body, "\u2068${cancelTarget.title}\u2069"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.finish(UUID.fromString(cancelId), complete = false)
                            pendingCancelId = null
                        },
                        enabled = !state.writing,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("reminderConfirmCancel"),
                        shape = VehicleLogShapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text(stringResource(R.string.reminder_cancel_confirm_action))
                    }
                },
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LedgerTopBar(
                title = stringResource(R.string.reminders_title),
                subtitle = vehicle.displayLabel,
                onBack = viewModel::back,
                backEnabled = !state.writing,
                backModifier = Modifier.testTag("remindersBack"),
                actions = {
                    if (state.draft.editing) {
                        HeaderCloseButton(
                            onClose = viewModel::cancelEditing,
                            modifier = Modifier.testTag("reminderCancelEdit"),
                            enabled = !state.writing,
                        )
                    }
                },
            )
        },
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
                .testTag("remindersScreen"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {


            if (dueReminders.isNotEmpty()) {
                item(key = "due-heading") {
                    Text(
                        text = stringResource(R.string.reminders_due_section),
                        modifier = Modifier.testTag("reminderDueSection").semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                        color = semanticColors.onWarning,
                    )
                }
                items(dueReminders, key = { it.id }) { reminder ->
                    reminderContent(reminder, true)
                }
            }

            // Notification options slot
            item {
                notificationOptions()
            }

            // New / Edit Reminder Form + Post-Complete Recurring Prompt
            item(key = "reminder-form") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.lastCompletedForRecurring?.let { completed ->
                        RecurringNextBanner(
                            completedTitle = completed.title,
                            onScheduleNext = {
                                viewModel.prepareRecurringNext(completed)
                                formScrollRequest = true
                            },
                            onDismiss = viewModel::dismissRecurringPrompt,
                        )
                    }
                    ReminderForm(
                        state = state,
                        viewModel = viewModel,
                        currentMileage = list?.mileage,
                        mileageKnownMissing = list != null && list.mileage == null,
                        titleFocusRequester = titleFocusRequester,
                    )
                }
            }

            // Reminders list status
            when {
                state.observedVehicleId == vehicle.id && state.loadFailed -> {
                    item {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = VehicleLogShapes.large,
                            colors = CardDefaults.outlinedCardColors(containerColor = semanticColors.dangerSurface),
                            border = BorderStroke(1.dp, semanticColors.dangerEdge),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.reminders_load_failed),
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = semanticColors.onDanger,
                                )
                                OutlinedButton(
                                    onClick = viewModel::retry,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    shape = VehicleLogShapes.medium,
                                ) {
                                    Text(stringResource(R.string.vehicle_retry))
                                }
                            }
                        }
                    }
                }
                list == null -> {
                    item {
                        Text(
                            text = stringResource(R.string.reminders_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                }
                list.rows.isEmpty() -> {
                    item {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = VehicleLogShapes.large,
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.reminders_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
                else -> items(otherReminders, key = { it.id }) { reminder ->
                    reminderContent(reminder, false)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderForm(
    state: RemindersUiState,
    viewModel: RemindersViewModel,
    currentMileage: Long? = null,
    mileageKnownMissing: Boolean = false,
    titleFocusRequester: FocusRequester = remember { FocusRequester() },
) {
    val semanticColors = LocalVehicleLogSemanticColors.current
    val draft = state.draft
    val titleError = state.showValidation && draft.title.isBlank()
    val dateBased = draft.threshold == ReminderThreshold.Date
    val thresholdError = state.showValidation && if (dateBased) {
        JalaliDates.parseJalali(draft.date) == null
    } else {
        parseReminderMileage(draft.mileage) == null
    }
    val enabled = !state.writing
    val focusManager = LocalFocusManager.current
    val fontScale = LocalDensity.current.fontScale
    var showDatePicker by rememberSaveable(draft.id) { mutableStateOf(false) }
    val todayLocalDate = viewModel.todayDate()

    if (showDatePicker && dateBased) {
        JalaliDatePickerDialog(
            initialDateText = draft.date,
            todayDate = todayLocalDate,
            onSelectDate = { selected ->
                viewModel.edit { it.copy(date = selected) }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = VehicleLogShapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(if (draft.editing) R.string.reminders_edit else R.string.reminders_new),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
                value = draft.title,
                onValueChange = { value -> viewModel.edit { it.copy(title = value) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocusRequester)
                    .testTag("reminderTitle"),
                label = { Text(stringResource(R.string.reminder_title_label)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.ContentOrRtl),
                singleLine = true,
                shape = VehicleLogShapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                enabled = enabled,
                isError = titleError,
                supportingText = if (titleError) ({ Text(stringResource(R.string.reminder_title_required)) }) else null,
            )

            Text(
                text = stringResource(R.string.reminder_threshold_type),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stackChips = fontScale >= 1.35f || maxWidth < 360.dp
                if (stackChips) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (type in ReminderThreshold.entries) {
                            val isDate = type == ReminderThreshold.Date
                            val isSelected = draft.threshold == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.edit { it.copy(threshold = type) } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag(if (isDate) "reminderDateType" else "reminderMileageType"),
                                enabled = enabled,
                                shape = VehicleLogShapes.medium,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                label = {
                                    Text(
                                        text = stringResource(if (isDate) R.string.reminder_date_type else R.string.reminder_mileage_type),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (type in ReminderThreshold.entries) {
                            val isDate = type == ReminderThreshold.Date
                            val isSelected = draft.threshold == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.edit { it.copy(threshold = type) } },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag(if (isDate) "reminderDateType" else "reminderMileageType"),
                                enabled = enabled,
                                shape = VehicleLogShapes.medium,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                label = {
                                    Text(
                                        text = stringResource(if (isDate) R.string.reminder_date_type else R.string.reminder_mileage_type),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = if (dateBased) draft.date else draft.mileage,
                onValueChange = { value ->
                    viewModel.edit { if (dateBased) it.copy(date = value) else it.copy(mileage = value) }
                },
                modifier = Modifier.fillMaxWidth().testTag(if (dateBased) "reminderDate" else "reminderMileage"),
                label = { Text(stringResource(if (dateBased) R.string.reminder_date_label else R.string.reminder_mileage_label)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                singleLine = true,
                shape = VehicleLogShapes.medium,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (dateBased) KeyboardType.Text else KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                enabled = enabled,
                isError = thresholdError,
                trailingIcon = if (dateBased) {
                    {
                        IconButton(
                            onClick = { showDatePicker = true },
                            enabled = enabled,
                            modifier = Modifier.size(48.dp).testTag("reminderDatePickerOpen"),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_calendar),
                                contentDescription = stringResource(R.string.date_picker_open),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                } else null,
                supportingText = {
                    Text(
                        stringResource(
                            when {
                                dateBased && thresholdError -> R.string.reminder_date_invalid
                                thresholdError -> R.string.reminder_mileage_invalid
                                dateBased -> R.string.reminder_date_hint
                                else -> R.string.reminder_mileage_hint
                            }
                        )
                    )
                },
            )

            if (dateBased) {
                val baseDateStr = todayLocalDate.toString()
                val presets = listOf(
                    Triple(1, R.string.date_chip_plus_1m, "reminderQuick1m"),
                    Triple(3, R.string.date_chip_plus_3m, "reminderQuick3m"),
                    Triple(6, R.string.date_chip_plus_6m, "reminderQuick6m"),
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for ((months, labelRes, tag) in presets) {
                        val targetGregorian = JalaliDates.addMonths(baseDateStr, months)
                        val targetJalali = targetGregorian?.let(JalaliDates::formatJalaliInput)
                        if (targetJalali != null) {
                            FilterChip(
                                selected = draft.date.trim() == targetJalali,
                                onClick = { viewModel.edit { it.copy(date = targetJalali) } },
                                enabled = enabled,
                                modifier = Modifier.heightIn(min = 48.dp).testTag(tag),
                                shape = VehicleLogShapes.medium,
                                label = { Text(stringResource(labelRes)) },
                            )
                        }
                    }
                }
            }

            if (!dateBased) {
                if (mileageKnownMissing) {
                    Text(
                        text = stringResource(R.string.reminder_missing_mileage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (currentMileage != null) {
                    Text(
                        text = stringResource(
                            R.string.ledger_mileage_reference,
                            "\u2066${currentMileage.toString().withPersianDigits()}\u2069",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("reminderSave"),
                enabled = enabled,
                shape = VehicleLogShapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(
                        when {
                            state.writing -> R.string.reminder_saving
                            draft.editing -> R.string.reminder_save_edit
                            else -> R.string.reminder_save
                        }
                    ),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }

            if (state.actionId == null) {
                state.message?.let { ReminderFeedback(it) }
            }
        }
    }
}

@Composable
private fun RecurringNextBanner(
    completedTitle: String,
    onScheduleNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    val semanticColors = LocalVehicleLogSemanticColors.current
    val fontScale = LocalDensity.current.fontScale
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminderRecurringBanner"),
        shape = VehicleLogShapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = semanticColors.successSurface),
        border = BorderStroke(1.dp, semanticColors.successEdge),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.reminder_recurring_prompt, "\u2068$completedTitle\u2069"),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = semanticColors.onSuccess,
            )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stack = fontScale >= 1.35f || maxWidth < 340.dp
                if (stack) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onScheduleNext,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("reminderRecurringNext"),
                            shape = VehicleLogShapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text(stringResource(R.string.reminder_recurring_next))
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("reminderRecurringDismiss"),
                            shape = VehicleLogShapes.medium,
                        ) {
                            Text(stringResource(R.string.reminder_recurring_dismiss))
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onScheduleNext,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("reminderRecurringNext"),
                            shape = VehicleLogShapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text(stringResource(R.string.reminder_recurring_next))
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("reminderRecurringDismiss"),
                            shape = VehicleLogShapes.medium,
                        ) {
                            Text(stringResource(R.string.reminder_recurring_dismiss))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderFeedback(message: ReminderMessage) {
    val label = when (message) {
        ReminderMessage.Created -> R.string.reminder_created
        ReminderMessage.Updated -> R.string.reminder_updated
        ReminderMessage.Completed -> R.string.reminder_completed
        ReminderMessage.Canceled -> R.string.reminder_canceled
        ReminderMessage.Failed -> R.string.reminder_write_failed
        ReminderMessage.NoActiveReminder -> R.string.reminder_no_longer_active
    }
    Text(
        text = stringResource(label),
        modifier = Modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("reminderMessage"),
        color = if (message == ReminderMessage.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
    )
}

@Composable
internal fun ReminderThresholdText(reminder: ReminderEntity) {
    val date = reminder.dueOn
    val label = if (date != null) {
        stringResource(R.string.reminder_date_summary, JalaliDates.formatGregorian(date).orEmpty())
    } else {
        stringResource(R.string.reminder_mileage_summary, "\u2066${reminder.dueKilometers.toString().withPersianDigits()}\u2069")
    }
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ReminderCard(
    reminder: ReminderEntity,
    due: Boolean,
    mileageMissing: Boolean,
    currentMileage: Long?,
    state: RemindersUiState,
    viewModel: RemindersViewModel,
    onEdit: () -> Unit,
    onRequestCancel: () -> Unit,
) {
    val semanticColors = LocalVehicleLogSemanticColors.current
    val active = reminder.state == ReminderState.Active.key
    val statusText = when {
        due -> R.string.reminder_status_due
        active -> R.string.reminder_status_pending
        reminder.state == ReminderState.Completed.key -> R.string.reminder_status_completed
        else -> R.string.reminder_status_canceled
    }

    val containerColor = when {
        due -> semanticColors.warningSurface
        active -> MaterialTheme.colorScheme.surface
        else -> semanticColors.wash
    }
    val borderColor = when {
        due -> semanticColors.warningEdge
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder:${reminder.id}"),
        shape = VehicleLogShapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 3dp amber accent line on the logical start edge for due reminders
            if (due) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(start = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .heightIn(min = 60.dp)
                            .background(semanticColors.warningEdge)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
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
                        if (due) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(semanticColors.warningEdge.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_bell),
                                    contentDescription = null,
                                    tint = semanticColors.onWarning,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDirection = TextDirection.ContentOrRtl,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            due -> semanticColors.warningEdge.copy(alpha = 0.2f)
                            active -> semanticColors.wash
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ) {
                        Text(
                            text = stringResource(statusText),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = when {
                                due -> semanticColors.onWarning
                                active -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                ReminderThresholdText(reminder)

                // Remaining distance badge for upcoming mileage reminders
                if (active && !due && reminder.dueKilometers != null && currentMileage != null) {
                    val remainingKm = reminder.dueKilometers - currentMileage
                    if (remainingKm >= 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = semanticColors.wash,
                        ) {
                            Text(
                                text = stringResource(R.string.reminders_remaining_km, remainingKm.toString().withPersianDigits()),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                if (active && mileageMissing && reminder.dueKilometers != null) {
                    Text(
                        text = stringResource(R.string.reminder_missing_mileage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (state.actionId?.toString() == reminder.id) {
                    state.message?.let { ReminderFeedback(it) }
                }

                if (active) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    val completeDescription = "${stringResource(R.string.reminder_complete_action)} ${reminder.title}"
                    val editDescription = "${stringResource(R.string.reminder_edit_action)} ${reminder.title}"
                    val cancelDescription = "${stringResource(R.string.reminder_cancel_action)} ${reminder.title}"
                    val fontScale = LocalDensity.current.fontScale
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { viewModel.finish(UUID.fromString(reminder.id), complete = true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("reminderComplete:${reminder.id}")
                                .semantics { contentDescription = completeDescription },
                            shape = VehicleLogShapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            enabled = !state.writing,
                        ) {
                            Text(
                                text = stringResource(R.string.reminder_complete_action),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        }
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val stackSecondary = fontScale >= 1.35f || maxWidth < 340.dp
                            val editBtn: @Composable (Modifier) -> Unit = { btnMod ->
                                OutlinedButton(
                                    onClick = onEdit,
                                    modifier = btnMod
                                        .heightIn(min = 48.dp)
                                        .testTag("reminderEdit:${reminder.id}")
                                        .semantics { contentDescription = editDescription },
                                    shape = VehicleLogShapes.medium,
                                    enabled = !state.writing,
                                ) {
                                    Text(
                                        text = stringResource(R.string.reminder_edit_action),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                        ),
                                    )
                                }
                            }
                            val cancelBtn: @Composable (Modifier) -> Unit = { btnMod ->
                                OutlinedButton(
                                    onClick = onRequestCancel,
                                    modifier = btnMod
                                        .heightIn(min = 48.dp)
                                        .testTag("reminderCancel:${reminder.id}")
                                        .semantics { contentDescription = cancelDescription },
                                    shape = VehicleLogShapes.medium,
                                    enabled = !state.writing,
                                ) {
                                    Text(
                                        text = stringResource(R.string.reminder_cancel_action),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = semanticColors.onDanger,
                                        ),
                                    )
                                }
                            }
                            if (stackSecondary) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    editBtn(Modifier.fillMaxWidth())
                                    cancelBtn(Modifier.fillMaxWidth())
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    editBtn(Modifier.weight(1f))
                                    cancelBtn(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
