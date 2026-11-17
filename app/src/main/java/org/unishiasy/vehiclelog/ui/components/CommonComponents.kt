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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.ui.theme.ButtonShape
import org.unishiasy.vehiclelog.ui.theme.LocalVehicleLogSemanticColors
import org.unishiasy.vehiclelog.ui.theme.NoticeShape
import org.unishiasy.vehiclelog.ui.theme.SheetShape
import org.unishiasy.vehiclelog.ui.vehicle.toPersianYear

enum class MainTab {
    HOME,
    HISTORY,
    REMINDERS;
}

/**
 * Concept C persistent bottom dock.
 *
 * Sits at the bottom of the screen with:
 * - A primary action button ("ثبت جدید" on Home/History, "ثبت یادآور" on Reminders).
 * - A 3-item navigation row (خانه, سوابق, یادآورها) with equal 48dp+ targets.
 */
@Composable
fun AppBottomDock(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    showPrimaryAction: Boolean = true,
) {
    val semanticColors = LocalVehicleLogSemanticColors.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("appBottomDock"),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semanticColors.rule),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showPrimaryAction) {
                val actionTextRes = when (activeTab) {
                    MainTab.REMINDERS -> R.string.dock_new_reminder
                    else -> R.string.dock_new_entry
                }
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("dockPrimaryAction"),
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_plus),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(actionTextRes),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            ),
                        )
                    }
                }
            }

            // 3-Destination Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DockNavItem(
                    tab = MainTab.HOME,
                    label = stringResource(R.string.dock_home),
                    iconRes = R.drawable.ic_home,
                    isSelected = activeTab == MainTab.HOME,
                    onClick = { onSelectTab(MainTab.HOME) },
                    modifier = Modifier.weight(1f),
                )
                DockNavItem(
                    tab = MainTab.HISTORY,
                    label = stringResource(R.string.dock_history),
                    iconRes = R.drawable.ic_book,
                    isSelected = activeTab == MainTab.HISTORY,
                    onClick = { onSelectTab(MainTab.HISTORY) },
                    modifier = Modifier.weight(1f),
                )
                DockNavItem(
                    tab = MainTab.REMINDERS,
                    label = stringResource(R.string.dock_reminders),
                    iconRes = R.drawable.ic_bell,
                    isSelected = activeTab == MainTab.REMINDERS,
                    onClick = { onSelectTab(MainTab.REMINDERS) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DockNavItem(
    tab: MainTab,
    label: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semanticColors = LocalVehicleLogSemanticColors.current
    val containerColor = if (isSelected) semanticColors.wash else Color.Transparent
    val contentColor = if (isSelected) semanticColors.action else semanticColors.ink

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .selectable(
                selected = isSelected,
                role = Role.Tab,
                onClick = onClick,
            )
            .testTag("dockTab:${tab.name.lowercase()}")
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                ),
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

enum class NoticeTone {
    Info,
    Warning,
    Danger,
    Success;
}

/**
 * Concept C Notice View.
 *
 * 3dp start edge, 6dp corner radius, 12dp padding.
 */
@Composable
fun NoticeBanner(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    tone: NoticeTone = NoticeTone.Info,
) {
    val semanticColors = LocalVehicleLogSemanticColors.current

    val (surfaceColor, edgeColor, textColor) = when (tone) {
        NoticeTone.Info -> Triple(semanticColors.wash, semanticColors.action, semanticColors.ink)
        NoticeTone.Warning -> Triple(semanticColors.warningSurface, semanticColors.warningEdge, semanticColors.onWarning)
        NoticeTone.Danger -> Triple(semanticColors.dangerSurface, semanticColors.dangerEdge, semanticColors.onDanger)
        NoticeTone.Success -> Triple(semanticColors.successSurface, semanticColors.successEdge, semanticColors.onSuccess)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(NoticeShape)
            .background(surfaceColor)
            .border(BorderStroke(1.dp, edgeColor.copy(alpha = 0.5f)), NoticeShape)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            // 3dp logical start edge
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(edgeColor),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = textColor,
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.9f),
                    )
                }
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
    val semanticColors = LocalVehicleLogSemanticColors.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semanticColors.rule),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
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
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
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
                    modifier = Modifier.size(36.dp),
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
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
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
                    modifier = Modifier.size(36.dp),
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

