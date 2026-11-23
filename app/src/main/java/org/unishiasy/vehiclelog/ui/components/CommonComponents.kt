package org.unishiasy.vehiclelog.ui.components

import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.ui.theme.ButtonShape
import org.unishiasy.vehiclelog.ui.theme.LocalVehicleLogSemanticColors
import org.unishiasy.vehiclelog.ui.theme.SheetShape
import org.unishiasy.vehiclelog.ui.vehicle.toPersianYear

enum class MainTab {
    HOME,
    HISTORY,
    REMINDERS,
    TOOLS;
}

/** Main destinations. Screen actions use the Scaffold FAB slot. */
@Composable
fun AppBottomDock(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val navLabel = stringResource(R.string.dock_nav_label)
    NavigationBar(
        modifier = modifier.testTag("appBottomDock").semantics { contentDescription = navLabel },
        containerColor = colors.surfaceContainer,
    ) {
        for (tab in MainTab.entries) {
            val (labelRes, iconRes) = when (tab) {
                MainTab.HOME -> R.string.dock_home to R.drawable.ic_home
                MainTab.HISTORY -> R.string.dock_history to R.drawable.ic_book
                MainTab.REMINDERS -> R.string.dock_reminders to R.drawable.ic_bell
                MainTab.TOOLS -> R.string.dock_tools to R.drawable.ic_tool
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

/**
 * Floating rounded pill navigation dock (32dp rounded corner, elevated surface,
 * lavender active pill indicator, Persian labels and Material icons).
 */
@Composable
fun FloatingBottomDock(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navLabel = stringResource(R.string.dock_nav_label)
    Surface(
        modifier = modifier
            .testTag("floatingBottomDock")
            .semantics { contentDescription = navLabel },
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (tab in MainTab.entries) {
                val isSelected = selectedTab == tab
                val (labelRes, iconRes) = when (tab) {
                    MainTab.HOME -> R.string.dock_home to R.drawable.ic_home
                    MainTab.HISTORY -> R.string.dock_history to R.drawable.ic_book
                    MainTab.REMINDERS -> R.string.dock_reminders to R.drawable.ic_bell
                    MainTab.TOOLS -> R.string.dock_tools to R.drawable.ic_tool
                }
                val label = stringResource(labelRes)

                Surface(
                    onClick = { onTabSelected(tab) },
                    selected = isSelected,
                    shape = RoundedCornerShape(24.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .testTag("dockTab:${tab.name.lowercase()}")
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = label },
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = if (isSelected) 14.dp else 12.dp,
                            vertical = 10.dp,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        if (isSelected) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Metric card with slate surface, title, large Persian value, optional subtitle, and icon.
 */
@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                CircularIconBadge(
                    icon = icon,
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
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
    }
}

/**
 * Circular badge container for icons.
 */
@Composable
fun CircularIconBadge(
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Status badge pill.
 */
@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
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
    compact: Boolean = false,
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
        expandedHeight = if (compact) 56.dp else 64.dp + 40.dp * (fontScale - 1f).coerceAtLeast(0f),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
internal fun HeaderCloseButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = stringResource(R.string.app_close),
) {
    IconButton(onClick = onClose, modifier = modifier.size(48.dp), enabled = enabled) {
        Icon(painterResource(R.drawable.ic_close), contentDescription = label)
    }
}

@Composable
internal fun DismissibleHeading(
    title: String,
    onClose: () -> Unit,
    closeModifier: Modifier = Modifier,
    closeEnabled: Boolean = true,
    style: TextStyle = LocalTextStyle.current,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f).semantics { heading() }, style = style)
        HeaderCloseButton(onClose, modifier = closeModifier, enabled = closeEnabled)
    }
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
 * Sticky form actions above the IME or system navigation.
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
    compact: Boolean = false,
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
                .padding(horizontal = 16.dp, vertical = if (compact) 4.dp else 12.dp),
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
                contentPadding = if (compact) PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                else ButtonDefaults.ContentPadding,
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

// Default extended FAB height plus Scaffold’s bottom gap.
internal val LedgerFabContentClearance = 72.dp

@Composable
internal fun LedgerFab(
    @StringRes labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(labelRes)
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = label },
        icon = { Icon(painterResource(R.drawable.ic_plus), contentDescription = null) },
        text = { Text(label) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySelectorSheet(
    vehicleLabel: String,
    onDismiss: () -> Unit,
    onSelectFuel: () -> Unit,
    onSelectExpense: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selecting by remember { mutableStateOf(false) }

    fun select(openForm: () -> Unit) {
        if (selecting) return
        selecting = true
        scope.launch {
            try {
                sheetState.hide()
                if (!sheetState.isVisible) openForm()
            } finally {
                selecting = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!selecting) onDismiss() },
        sheetState = sheetState,
        sheetGesturesEnabled = !selecting,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = !selecting,
            shouldDismissOnClickOutside = !selecting,
        ),
        shape = SheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.testTag("entrySelectorSheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.dock_new_entry),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = vehicleLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SelectorOptionRow(
                title = stringResource(R.string.home_log_fuel),
                iconRes = R.drawable.ic_fuel,
                detail = stringResource(R.string.entry_type_fuel_desc),
                enabled = !selecting,
                onClick = { select(onSelectFuel) },
                testTag = "selectorOption:fuel",
            )

            SelectorOptionRow(
                title = stringResource(R.string.home_log_expense),
                iconRes = R.drawable.ic_wallet,
                detail = stringResource(R.string.entry_type_expense_desc),
                enabled = !selecting,
                onClick = { select(onSelectExpense) },
                testTag = "selectorOption:expense",
            )

        }
    }
}

@Composable
private fun SelectorOptionRow(
    title: String,
    iconRes: Int,
    detail: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    val semanticColors = LocalVehicleLogSemanticColors.current

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
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
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                        contentDescription = stringResource(R.string.app_close),
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

