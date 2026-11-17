package org.unishiasy.vehiclelog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.unishiasy.vehiclelog.ui.settings.ThemeMode

@Composable
fun VehicleLogTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    },
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) VehicleLogDarkColorScheme else VehicleLogLightColorScheme
    val semanticColors = if (darkTheme) VehicleLogDarkSemanticColors else VehicleLogLightSemanticColors

    CompositionLocalProvider(
        LocalVehicleLogSemanticColors provides semanticColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VehicleLogTypography,
            shapes = VehicleLogShapes,
            content = content,
        )
    }
}
