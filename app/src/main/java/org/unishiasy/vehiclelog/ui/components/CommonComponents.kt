package org.unishiasy.vehiclelog.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.ui.theme.ButtonShape
import org.unishiasy.vehiclelog.ui.theme.LocalVehicleLogSemanticColors
import org.unishiasy.vehiclelog.ui.theme.SheetShape
import org.unishiasy.vehiclelog.ui.vehicle.toPersianYear

enum class MainTab {
    HOME,
    HISTORY,
    REMINDERS;
}

/** Main destinations and the current destination's create action. */
@Composable
fun AppBottomDock(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    showPrimaryAction: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth().testTag("appBottomDock"),
        color = colors.surfaceContainer,
    ) {
        Column {
            if (showPrimaryAction) {
                val actionTextRes = when (activeTab) {
                    MainTab.REMINDERS -> R.string.dock_new_reminder
                    else -> R.string.dock_new_entry
                }
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .heightIn(min = 48.dp)
                        .testTag("dockPrimaryAction"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_plus),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(actionTextRes))
                }
            }

            val navLabel = stringResource(R.string.dock_nav_label)
            NavigationBar(
                modifier = Modifier.semantics { contentDescription = navLabel },
                containerColor = colors.surfaceContainer,
            ) {
                for (tab in MainTab.entries) {
                    val (labelRes, iconRes) = when (tab) {
                        MainTab.HOME -> R.string.dock_home to R.drawable.ic_home
                        MainTab.HISTORY -> R.string.dock_history to R.drawable.ic_book
                        MainTab.REMINDERS -> R.string.dock_reminders to R.drawable.ic_bell
                    }
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { onSelectTab(tab) },
                        modifier = Modifier.testTag("dockTab:${tab.name.lowercase()}"),
                        icon = { Icon(painterResource(iconRes), contentDescription = null) },
                        label = { Text(stringResource(labelRes), textAlign = TextAlign.Center) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = colors.secondaryContainer,
                            selectedIconColor = colors.onSecondaryContainer,
                            selectedTextColor = colors.onSurface,
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LedgerTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    backLabel: String = stringResource(R.string.ledger_back),
    backEnabled: Boolean = true,
    backModifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleModifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val fontScale = LocalDensity.current.fontScale
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        modifier = subtitleModifier,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack, enabled = backEnabled, modifier = backModifier) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = backLabel)
                }
            }
        },
        actions = actions,
        expandedHeight = 64.dp + 40.dp * (fontScale - 1f).coerceAtLeast(0f),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
internal fun LedgerSettingsAction(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.testTag("homeOpenSettings")) {
        Icon(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = stringResource(R.string.backup_settings),
        )
    }
}

enum class NoticeTone {
    Info,
    Warning,
    Danger,
    Success;
}

/** Inline feedback with a semantic container and a live announcement. */
@Composable
fun NoticeBanner(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    tone: NoticeTone = NoticeTone.Info,
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalVehicleLogSemanticColors.current
    val (container, content) = when (tone) {
        NoticeTone.Info -> colors.secondaryContainer to colors.onSecondaryContainer
        NoticeTone.Warning -> colors.tertiaryContainer to colors.onTertiaryContainer
        NoticeTone.Danger -> colors.errorContainer to colors.onErrorContainer
        NoticeTone.Success -> semantic.successSurface to semantic.onSuccess
    }

    Surface(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (!description.isNullOrBlank()) {
                Text(text = description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * Sticky Form Footer with 1dp divider and IME padding.
 */
@Composable
fun StickyFormFooter(
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    cancelLabel: String? = null,
    onCancel: (() -> Unit)? = null,
    isActionBusy: Boolean = false,
    isActionEnabled: Boolean = true,
    actionTestTag: String = "formActionSave",
    cancelTestTag: String = "formActionCancel",
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onAction,
                enabled = isActionEnabled && !isActionBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(actionTestTag),
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = if (isActionBusy) stringResource(R.string.vehicle_saving) else actionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }

            if (cancelLabel != null && onCancel != null) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isActionBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag(cancelTestTag),
                    shape = ButtonShape,
                ) {
                    Text(
                        text = cancelLabel,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/**
 * Concept C Disclosure Row with expanded semantics and animation.
 */
@Composable
fun DisclosureRow(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    val semanticColors = LocalVehicleLogSemanticColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, semanticColors.rule), RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = if (isExpanded) "▲" else "▼",
                style = MaterialTheme.typography.labelSmall,
                color = semanticColors.mutedInk,
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                HorizontalDivider(color = semanticColors.rule, modifier = Modifier.padding(bottom = 8.dp))
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySelectorSheet(
    onDismiss: () -> Unit,
    onSelectFuel: () -> Unit,
    onSelectExpense: () -> Unit,
    onSelectService: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semanticColors = LocalVehicleLogSemanticColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = SheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.testTag("entrySelectorSheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.entry_type_selector_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.vehicle_cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            HorizontalDivider(color = semanticColors.rule)

            SelectorOptionRow(
                title = stringResource(R.string.entry_type_fuel),
                subtitle = stringResource(R.string.entry_type_fuel_desc),
                iconRes = R.drawable.ic_fuel,
                onClick = onSelectFuel,
                testTag = "selectorOption:fuel",
            )

            SelectorOptionRow(
                title = stringResource(R.string.entry_type_expense),
                subtitle = stringResource(R.string.entry_type_expense_desc),
                iconRes = R.drawable.ic_wallet,
                onClick = onSelectExpense,
                testTag = "selectorOption:expense",
            )

            SelectorOptionRow(
                title = stringResource(R.string.entry_type_service),
                subtitle = stringResource(R.string.entry_type_service_desc),
                iconRes = R.drawable.ic_tool,
                onClick = onSelectService,
                testTag = "selectorOption:service",
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SelectorOptionRow(
    title: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit,
    testTag: String,
) {
    val semanticColors = LocalVehicleLogSemanticColors.current

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semanticColors.rule),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
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
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = semanticColors.action,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = null,
                tint = semanticColors.mutedInk,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSwitcherSheet(
    vehicles: List<VehicleEntity>,
    selectedVehicleId: String?,
    onSelectVehicle: (String) -> Unit,
    onManageVehicles: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semanticColors = LocalVehicleLogSemanticColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = SheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.testTag("vehicleSwitcherSheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.vehicle_select_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.vehicle_cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            HorizontalDivider(color = semanticColors.rule)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (car in vehicles) {
                    val isSelected = car.id == selectedVehicleId
                    Surface(
                        onClick = {
                            onSelectVehicle(car.id)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("switchVehicle:${car.id}"),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) semanticColors.wash else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, semanticColors.action) else null,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = car.displayLabel,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                car.productionYear?.let { year ->
                                    Text(
                                        text = stringResource(R.string.vehicle_model_year, year.toPersianYear()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = semanticColors.action,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = semanticColors.rule, modifier = Modifier.padding(vertical = 4.dp))

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onManageVehicles()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("manageVehiclesButton"),
                shape = ButtonShape,
            ) {
                Text(
                    text = stringResource(R.string.manage_vehicles),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

