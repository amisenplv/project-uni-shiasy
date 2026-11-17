package org.unishiasy.vehiclelog.ui.settings

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.backup.BackupOutcome
import org.unishiasy.vehiclelog.ui.theme.LocalVehicleLogSemanticColors
import org.unishiasy.vehiclelog.ui.theme.VehicleLogShapes

@Composable
internal fun BackupSettingsScreen(
    state: BackupSettingsState,
    viewModel: BackupSettingsViewModel,
    cloudSettings: CloudSettingsViewModel? = null,
    appearancePreferences: AppearancePreferences? = null,
    notificationOptions: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val semanticColors = LocalVehicleLogSemanticColors.current
    val prefs = appearancePreferences ?: remember { AppearancePreferences(context) }
    val currentThemeMode by prefs.themeMode.collectAsState()

    var cloudOpen by rememberSaveable { mutableStateOf(false) }
    if (cloudOpen && cloudSettings != null) {
        CloudSettingsScreen(cloudSettings) { cloudOpen = false }
        return
    }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        viewModel.selected(BackupAction.Export, it)
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        viewModel.selected(BackupAction.Restore, it)
    }
    val idle = state.action == null
    BackHandler(onBack = viewModel::back)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp)
                .testTag("backupSettings"),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = viewModel::back,
                    modifier = Modifier.size(48.dp),
                    enabled = idle,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.ledger_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.backup_settings),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Section 1: Appearance (ظاهر)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.settings_appearance),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
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
                            text = stringResource(R.string.settings_appearance_mode),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val options = listOf(
                                ThemeMode.SYSTEM to R.string.settings_appearance_system,
                                ThemeMode.LIGHT to R.string.settings_appearance_light,
                                ThemeMode.DARK to R.string.settings_appearance_dark,
                            )
                            for ((mode, stringRes) in options) {
                                val isSelected = currentThemeMode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { prefs.setThemeMode(mode) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp),
                                    shape = VehicleLogShapes.medium,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                    ),
                                    label = {
                                        Text(
                                            text = stringResource(stringRes),
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
            }

            // Section 2: Notifications (اعلان‌ها)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.settings_notifications),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                notificationOptions()
            }

            // Section 3: Data & Manual Backup (داده‌ها و پشتیبان‌گیری دستی)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.settings_data),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Plaintext warning card
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = VehicleLogShapes.large,
                    colors = CardDefaults.outlinedCardColors(containerColor = semanticColors.warningSurface),
                    border = BorderStroke(1.dp, semanticColors.warningEdge),
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(start = 0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxSize()
                                    .background(semanticColors.warningEdge)
                            )
                        }
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(semanticColors.warningEdge.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_warning),
                                    contentDescription = null,
                                    tint = semanticColors.onWarning,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = stringResource(R.string.backup_section),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = semanticColors.onWarning,
                                )
                                Text(
                                    text = stringResource(R.string.backup_export_explanation),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                // Export Card
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = VehicleLogShapes.large,
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(semanticColors.wash),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_export),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Text(
                                text = stringResource(R.string.backup_export),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Button(
                            onClick = {
                                launchPicker(viewModel, BackupAction.Export) { export.launch("vehicle-ledger-backup.json") }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("backupExport"),
                            shape = VehicleLogShapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = idle,
                        ) {
                            Text(
                                text = stringResource(R.string.backup_export),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                ),
                            )
                        }
                    }
                }

                // Restore Card
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = VehicleLogShapes.large,
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(semanticColors.wash),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_restore),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Text(
                                text = stringResource(R.string.backup_restore),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = stringResource(R.string.backup_restore_explanation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = {
                                launchPicker(viewModel, BackupAction.Restore) { restore.launch(arrayOf("*/*")) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("backupRestore"),
                            shape = VehicleLogShapes.medium,
                            enabled = idle,
                        ) {
                            Text(
                                text = stringResource(R.string.backup_restore),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }

                // Cloud Button if available
                if (cloudSettings != null) {
                    OutlinedButton(
                        onClick = { cloudOpen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("settingsCloud"),
                        shape = VehicleLogShapes.medium,
                        enabled = idle,
                    ) {
                        Text(stringResource(R.string.cloud_title))
                    }
                }

                // Result feedback card
                val message = when {
                    state.working && state.action == BackupAction.Export -> R.string.backup_exporting
                    state.working -> R.string.backup_restoring
                    state.outcome != null -> outcomeMessage(state.outcome)
                    else -> null
                }
                if (message != null) {
                    val isSuccess = state.outcome == BackupOutcome.Exported || state.outcome == BackupOutcome.Restored
                    val isError = state.outcome != null && !isSuccess && state.outcome != BackupOutcome.Canceled

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = VehicleLogShapes.medium,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = when {
                                isSuccess -> semanticColors.successSurface
                                isError -> semanticColors.dangerSurface
                                else -> semanticColors.wash
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            when {
                                isSuccess -> semanticColors.successEdge
                                isError -> semanticColors.dangerEdge
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                    ) {
                        Text(
                            text = stringResource(message),
                            modifier = Modifier
                                .padding(16.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite }
                                .testTag("backupResult"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = when {
                                isSuccess -> semanticColors.onSuccess
                                isError -> semanticColors.onDanger
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun launchPicker(viewModel: BackupSettingsViewModel, action: BackupAction, launch: () -> Unit) {
    if (!viewModel.startPicker(action)) return
    try {
        launch()
    } catch (_: ActivityNotFoundException) {
        viewModel.pickerUnavailable(action)
    } catch (_: SecurityException) {
        viewModel.pickerUnavailable(action)
    }
}

@StringRes
private fun outcomeMessage(outcome: BackupOutcome): Int = when (outcome) {
    BackupOutcome.Exported -> R.string.backup_exported
    BackupOutcome.Restored -> R.string.backup_restored
    BackupOutcome.RestoredWithNotificationWarning -> R.string.backup_restored_notification_warning
    BackupOutcome.Canceled -> R.string.backup_canceled
    BackupOutcome.LedgerPopulated -> R.string.backup_populated
    BackupOutcome.TooLarge -> R.string.backup_too_large
    BackupOutcome.UnsupportedFormat -> R.string.backup_unsupported
    BackupOutcome.InvalidData -> R.string.backup_invalid
    BackupOutcome.FileUnavailable -> R.string.backup_file_unavailable
    BackupOutcome.StorageFailure -> R.string.backup_storage_failure
}
