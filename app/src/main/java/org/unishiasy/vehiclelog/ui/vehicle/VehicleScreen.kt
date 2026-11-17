package org.unishiasy.vehiclelog.ui.vehicle

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.VehicleInputError
import org.unishiasy.vehiclelog.data.VehicleSaveResult
import org.unishiasy.vehiclelog.data.catalog.VehicleCatalog
import org.unishiasy.vehiclelog.data.catalog.VehicleCatalogEntry
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.domain.withPersianDigits
import org.unishiasy.vehiclelog.ui.theme.LocalVehicleLogSemanticColors
import org.unishiasy.vehiclelog.ui.theme.VehicleLogShapes

@Composable
internal fun VehicleScreen(
    viewModel: VehicleViewModel,
    openLedger: ((VehicleEntity) -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft = state.draft
    val semanticColors = LocalVehicleLogSemanticColors.current
    BackHandler(enabled = draft != null, onBack = viewModel::cancel)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        key(draft != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (draft == null) {
                    // Top App Bar for First Launch / Empty State
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

                    VehicleList(state, viewModel, openLedger, onOpenSettings)
                } else {
                    // Vehicle Add / Edit Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconButton(
                            onClick = viewModel::cancel,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.vehicle_cancel),
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Text(
                            stringResource(if (draft.id == null) R.string.vehicle_add else R.string.vehicle_edit),
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                    VehicleForm(draft, state, viewModel)
                }
            }
        }
    }
}

@Composable
private fun VehicleList(
    state: VehicleUiState,
    viewModel: VehicleViewModel,
    openLedger: ((VehicleEntity) -> Unit)?,
    onOpenSettings: (() -> Unit)?,
) {
    val semanticColors = LocalVehicleLogSemanticColors.current
    when (state.saveResult) {
        is VehicleSaveResult.Created, is VehicleSaveResult.Updated -> Text(
            stringResource(R.string.vehicle_saved),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        else -> Unit
    }
    val vehicles = state.vehicles
    when {
        state.listFailed -> {
            ErrorText(R.string.vehicle_list_failed)
            OutlinedButton(onClick = viewModel::retryList, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.vehicle_retry))
            }
        }
        vehicles == null -> Text(stringResource(R.string.vehicle_loading))
        vehicles.isEmpty() -> {
            // First Launch Empty Experience (Matches previews/empty.png)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = VehicleLogShapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(semanticColors.wash),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_car),
                            contentDescription = null,
                            tint = semanticColors.action,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.home_empty_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            stringResource(R.string.vehicle_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            stringResource(R.string.home_empty_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = viewModel::addVehicle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("vehicleAddButton"),
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
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.vehicle_add),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            // Restore from File Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = VehicleLogShapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        stringResource(R.string.home_empty_backup_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.home_empty_backup_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (onOpenSettings != null) {
                        OutlinedButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("homeRestoreBackup"),
                            shape = VehicleLogShapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_restore),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.backup_restore),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
        else -> {
            SectionHeading(R.string.vehicle_list_title)
            for (vehicle in vehicles) {
                OutlinedButton(
                    onClick = { viewModel.editVehicle(vehicle) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            vehicle.displayLabel,
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDirection = TextDirection.ContentOrRtl,
                            ),
                        )
                        val calendar = when (vehicle.productionYearCalendar) {
                            "solar_hijri" -> R.string.vehicle_solar_hijri
                            "gregorian" -> R.string.vehicle_gregorian
                            else -> null
                        }
                        if (vehicle.productionYear != null && calendar != null) {
                            Text(
                                stringResource(
                                    R.string.vehicle_year_summary,
                                    vehicle.productionYear.toPersianYear(),
                                    stringResource(calendar),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (openLedger != null) {
                    TextButton(
                        onClick = { openLedger(vehicle) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(
                            stringResource(R.string.ledger_open, "\u2068${vehicle.displayLabel}\u2069"),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            Button(
                onClick = viewModel::addVehicle,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(stringResource(R.string.vehicle_add))
            }
        }
    }
}

@Composable
private fun VehicleForm(draft: VehicleDraft, state: VehicleUiState, viewModel: VehicleViewModel) {
    val input = draft.input
    val year = input.productionYear.orEmpty()
    val invalid = (state.saveResult as? VehicleSaveResult.InvalidInput)?.reason
    val nameError = invalid == VehicleInputError.BlankDisplayLabel
    val yearError = when {
        invalid == VehicleInputError.MalformedProductionYear -> R.string.vehicle_year_invalid
        invalid == VehicleInputError.YearAndCalendarMustBePaired && year.isBlank() ->
            R.string.vehicle_year_required
        else -> null
    }
    val calendarError = invalid == VehicleInputError.UnsupportedYearCalendar ||
        (invalid == VehicleInputError.YearAndCalendarMustBePaired && year.isNotBlank())
    val enabled = !state.saving
    val focusManager = LocalFocusManager.current
    val catalog = state.catalog
    if (catalog != null) {
        var pickerOpen by rememberSaveable { mutableStateOf(false) }
        OutlinedButton(
            onClick = { pickerOpen = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = VehicleLogShapes.medium,
        ) {
            Text(stringResource(R.string.vehicle_catalog_select))
        }
        if (pickerOpen && enabled) {
            VehicleCatalogPicker(catalog, onClose = { pickerOpen = false }) { entry ->
                viewModel.selectCatalogEntry(entry)
                pickerOpen = false
            }
        }
    }
    OutlinedTextField(
        value = input.displayLabel,
        onValueChange = { label -> viewModel.changeInput { it.copy(displayLabel = label) } },
        label = { Text(stringResource(R.string.vehicle_name)) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.ContentOrRtl),
        modifier = Modifier.fillMaxWidth().testTag("vehicleName"),
        shape = VehicleLogShapes.medium,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        isError = nameError,
        supportingText = if (nameError) ({ ErrorText(R.string.vehicle_name_required) }) else null,
    )
    OutlinedTextField(
        value = year,
        onValueChange = { newYear ->
            viewModel.changeInput {
                it.copy(
                    productionYear = newYear,
                    productionYearCalendar = it.productionYearCalendar.takeIf { newYear.isNotBlank() },
                )
            }
        },
        label = { Text(stringResource(R.string.vehicle_year)) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
        modifier = Modifier.fillMaxWidth().testTag("vehicleYear"),
        shape = VehicleLogShapes.medium,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        isError = yearError != null,
        supportingText = if (yearError != null) ({ ErrorText(yearError) }) else null,
    )
    if (year.isNotBlank()) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.vehicle_year_calendar), style = MaterialTheme.typography.titleMedium)
            CalendarOption(
                R.string.vehicle_solar_hijri,
                selected = input.productionYearCalendar == "solar_hijri",
                enabled = enabled,
                onClick = { viewModel.changeInput { it.copy(productionYearCalendar = "solar_hijri") } },
            )
            CalendarOption(
                R.string.vehicle_gregorian,
                selected = input.productionYearCalendar == "gregorian",
                enabled = enabled,
                onClick = { viewModel.changeInput { it.copy(productionYearCalendar = "gregorian") } },
            )
            if (calendarError) ErrorText(R.string.vehicle_calendar_required)
        }
    }
    when (state.saveResult) {
        VehicleSaveResult.NotFound -> ErrorText(R.string.vehicle_not_found)
        VehicleSaveResult.StorageFailure -> ErrorText(R.string.vehicle_save_failed)
        else -> Unit
    }

    Spacer(Modifier.height(8.dp))

    // Sticky submit / cancel buttons
    Button(
        onClick = viewModel::save,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("vehicleSave"),
        shape = VehicleLogShapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(
            stringResource(if (enabled) R.string.vehicle_save else R.string.vehicle_saving),
            style = MaterialTheme.typography.labelLarge,
        )
    }
    OutlinedButton(
        onClick = viewModel::cancel,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("vehicleCancel"),
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
private fun VehicleCatalogPicker(
    catalog: VehicleCatalog,
    onClose: () -> Unit,
    onSelect: (VehicleCatalogEntry) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(catalog, query) { catalog.search(query) }
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.vehicle_catalog_close))
            }
        },
        title = { Text(stringResource(R.string.vehicle_catalog_select)) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.vehicle_catalog_search)) },
                    modifier = Modifier.fillMaxWidth().testTag("vehicleCatalogQuery"),
                    shape = VehicleLogShapes.medium,
                    singleLine = true,
                )
                if (results.isEmpty()) {
                    Text(
                        stringResource(R.string.vehicle_catalog_no_matches),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(results, key = { it.key }) { entry ->
                            TextButton(
                                onClick = { onSelect(entry) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(entry.displayLabel, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun CalendarOption(
    @StringRes labelRes: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SectionHeading(@StringRes textRes: Int) {
    Text(
        stringResource(textRes),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun ErrorText(@StringRes textRes: Int) {
    Text(
        stringResource(textRes),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

fun Int?.toPersianYear(): String = this?.toString().orEmpty().withPersianDigits()
