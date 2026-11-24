package org.unishiasy.vehiclelog.ui.tools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.ui.components.CircularIconBadge
import org.unishiasy.vehiclelog.ui.components.LedgerFabContentClearance
import org.unishiasy.vehiclelog.ui.components.LedgerTopBar

/**
 * Tools Hub screen displaying navigation cards for trip calculations, vehicle management,
 * reports, and settings.
 */
@Composable
fun ToolsHubScreen(
    onOpenTripCalculator: () -> Unit,
    onOpenVehicleManagement: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    currentVehicleLabel: String? = null,
    bottomBar: @Composable () -> Unit = {},
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .testTag("toolsHubScreen"),
            topBar = {
                LedgerTopBar(
                    title = stringResource(R.string.tools_title),
                    subtitle = currentVehicleLabel ?: stringResource(R.string.tools_hub_subtitle),
                )
            },
            bottomBar = bottomBar,
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = LedgerFabContentClearance + 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Trip Cost Calculator Card
                ToolsNavigationCard(
                    title = stringResource(R.string.tools_trip_calculator_title),
                    subtitle = stringResource(R.string.tools_trip_calculator_desc),
                    icon = painterResource(R.drawable.ic_fuel),
                    onClick = onOpenTripCalculator,
                    testTag = "toolsCard:tripCalculator",
                )

                // Vehicle Management Card
                ToolsNavigationCard(
                    title = stringResource(R.string.tools_vehicle_management_title),
                    subtitle = stringResource(R.string.tools_vehicle_management_desc),
                    icon = painterResource(R.drawable.ic_car),
                    onClick = onOpenVehicleManagement,
                    testTag = "toolsCard:vehicleManagement",
                )

                // Reports & Monthly Dashboard Card
                ToolsNavigationCard(
                    title = stringResource(R.string.tools_reports_dashboard_title),
                    subtitle = stringResource(R.string.tools_reports_dashboard_desc),
                    icon = painterResource(R.drawable.ic_chart),
                    onClick = onOpenReports,
                    testTag = "toolsCard:reportsDashboard",
                )

                // Settings & Backup Card
                ToolsNavigationCard(
                    title = stringResource(R.string.tools_settings_backup_title),
                    subtitle = stringResource(R.string.tools_settings_backup_desc),
                    icon = painterResource(R.drawable.ic_settings),
                    onClick = onOpenSettings,
                    testTag = "toolsCard:settingsBackup",
                )
            }
        }
    }
}

@Composable
fun ToolsNavigationCard(
    title: String,
    subtitle: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(
                onClick = onClick,
                role = Role.Button,
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceContainer,
        ),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularIconBadge(
                    painter = icon,
                    backgroundColor = colors.surfaceContainerHigh,
                    iconColor = colors.primary,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = colors.onSurface,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_end),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
