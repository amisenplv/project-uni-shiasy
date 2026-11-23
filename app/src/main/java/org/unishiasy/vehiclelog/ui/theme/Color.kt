package org.unishiasy.vehiclelog.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Design system color palette and semantic tokens for Vehicle Ledger.
 *
 * Dark Slate & Lavender palette for modern in-cab readability,
 * paired with high-contrast light mode tokens.
 */
object VehicleLogColors {
    // Design System Tokens (Dark Slate & Lavender)
    val DarkSlateBackground = Color(0xFF0E0E13)
    val DarkSlateSurface = Color(0xFF1A1824)
    val DarkSlateSurfaceElevated = Color(0xFF262335)
    val LavenderPrimary = Color(0xFF9D85F2)
    val LavenderSecondary = Color(0xFFB8A7FF)
    val TextPrimary = Color(0xFFF3F3F6)
    val TextSecondary = Color(0xFFA09DB1)
    val AccentGreen = Color(0xFF4EBA87)
    val AccentAmber = Color(0xFFF5A623)
    val AccentRed = Color(0xFFE55656)

    // Light Palette
    val CanvasLight = Color(0xFFF3F6F8)
    val SurfaceLight = Color(0xFFFFFFFF)
    val InkLight = Color(0xFF112D34)
    val MutedInkLight = Color(0xFF4D6269)
    val ActionLight = Color(0xFF075A63)
    val OnActionLight = Color(0xFFFFFFFF)
    val WashLight = Color(0xFFDBECEF)
    val RuleLight = Color(0xFFD2E0E3)
    val ControlOutlineLight = Color(0xFF71858B)
    val FocusLight = Color(0xFF125D9A)
    val WarningSurfaceLight = Color(0xFFFFF0D3)
    val OnWarningLight = Color(0xFF694414)
    val WarningEdgeLight = Color(0xFFDDA133)
    val ErrorBackgroundLight = Color(0xFFFAE9E7)
    val DangerLight = Color(0xFFA12C30)
    val OnDangerLight = Color(0xFFFFFFFF)
    val SuccessSurfaceLight = Color(0xFFE4F1E9)
    val SuccessInkLight = Color(0xFF23533B)
    val SuccessEdgeLight = Color(0xFF2E7D32)
    val ScrimLight = Color(0xA3142E35)

    // Dark Palette
    val CanvasDark = DarkSlateBackground
    val SurfaceDark = DarkSlateSurface
    val SurfaceElevatedDark = DarkSlateSurfaceElevated
    val InkDark = TextPrimary
    val MutedInkDark = TextSecondary
    val ActionDark = LavenderPrimary
    val OnActionDark = Color(0xFF160D35)
    val WashDark = DarkSlateSurfaceElevated
    val RuleDark = DarkSlateSurfaceElevated
    val ControlOutlineDark = DarkSlateSurfaceElevated
    val FocusDark = LavenderSecondary
    val WarningSurfaceDark = Color(0xFF382A16)
    val OnWarningDark = Color(0xFFF4D497)
    val WarningEdgeDark = Color(0xFFDDA133)
    val ErrorBackgroundDark = Color(0xFF3A1C22)
    val DangerDark = Color(0xFFFFB4AB)
    val OnDangerDark = Color(0xFF4B1418)
    val SuccessSurfaceDark = Color(0xFF152E22)
    val SuccessInkDark = AccentGreen
    val SuccessEdgeDark = Color(0xFF2E7D32)
    val ScrimDark = Color(0xBD060A0D)

    // Category Indicators
    val FuelGreen = AccentGreen
    val ServiceAmber = AccentAmber
    val RepairOrange = Color(0xFFC2410C)
}

@Immutable
data class VehicleLogSemanticColors(
    val canvas: Color,
    val surface: Color,
    val ink: Color,
    val mutedInk: Color,
    val action: Color,
    val onAction: Color,
    val wash: Color,
    val rule: Color,
    val controlOutline: Color,
    val focus: Color,
    val warningSurface: Color,
    val onWarning: Color,
    val warningEdge: Color,
    val onWarningEdge: Color,
    val dangerSurface: Color,
    val onDanger: Color,
    val dangerEdge: Color,
    val destructiveButtonFill: Color,
    val onDestructiveButton: Color,
    val successSurface: Color,
    val onSuccess: Color,
    val successEdge: Color,
    val modalScrim: Color,
    // Backwards-compatible aliases
    val quietWash: Color = wash,
    val blueInk: Color = action,
    val odometerContainer: Color = surface,
    val onOdometer: Color = ink,
    val odometerMuted: Color = mutedInk,
    val odometerDivider: Color = rule,
    val reviewMat: Color = canvas,
)

val LocalVehicleLogSemanticColors = staticCompositionLocalOf {
    VehicleLogLightSemanticColors
}

internal val VehicleLogLightSemanticColors = VehicleLogSemanticColors(
    canvas = VehicleLogColors.CanvasLight,
    surface = VehicleLogColors.SurfaceLight,
    ink = VehicleLogColors.InkLight,
    mutedInk = VehicleLogColors.MutedInkLight,
    action = VehicleLogColors.ActionLight,
    onAction = VehicleLogColors.OnActionLight,
    wash = VehicleLogColors.WashLight,
    rule = VehicleLogColors.RuleLight,
    controlOutline = VehicleLogColors.ControlOutlineLight,
    focus = VehicleLogColors.FocusLight,
    warningSurface = VehicleLogColors.WarningSurfaceLight,
    onWarning = VehicleLogColors.OnWarningLight,
    warningEdge = VehicleLogColors.WarningEdgeLight,
    onWarningEdge = VehicleLogColors.InkLight,
    dangerSurface = VehicleLogColors.ErrorBackgroundLight,
    onDanger = VehicleLogColors.DangerLight,
    dangerEdge = VehicleLogColors.DangerLight,
    destructiveButtonFill = VehicleLogColors.DangerLight,
    onDestructiveButton = VehicleLogColors.OnDangerLight,
    successSurface = VehicleLogColors.SuccessSurfaceLight,
    onSuccess = VehicleLogColors.SuccessInkLight,
    successEdge = VehicleLogColors.SuccessEdgeLight,
    modalScrim = VehicleLogColors.ScrimLight,
    quietWash = VehicleLogColors.WashLight,
    blueInk = VehicleLogColors.ActionLight,
    odometerContainer = VehicleLogColors.SurfaceLight,
    onOdometer = VehicleLogColors.InkLight,
    odometerMuted = VehicleLogColors.MutedInkLight,
    odometerDivider = VehicleLogColors.RuleLight,
    reviewMat = VehicleLogColors.CanvasLight,
)

internal val VehicleLogDarkSemanticColors = VehicleLogSemanticColors(
    canvas = VehicleLogColors.CanvasDark,
    surface = VehicleLogColors.SurfaceDark,
    ink = VehicleLogColors.InkDark,
    mutedInk = VehicleLogColors.MutedInkDark,
    action = VehicleLogColors.ActionDark,
    onAction = VehicleLogColors.OnActionDark,
    wash = VehicleLogColors.WashDark,
    rule = VehicleLogColors.RuleDark,
    controlOutline = VehicleLogColors.ControlOutlineDark,
    focus = VehicleLogColors.FocusDark,
    warningSurface = VehicleLogColors.WarningSurfaceDark,
    onWarning = VehicleLogColors.OnWarningDark,
    warningEdge = VehicleLogColors.WarningEdgeDark,
    onWarningEdge = VehicleLogColors.InkLight,
    dangerSurface = VehicleLogColors.ErrorBackgroundDark,
    onDanger = VehicleLogColors.DangerDark,
    dangerEdge = VehicleLogColors.DangerDark,
    destructiveButtonFill = VehicleLogColors.OnDangerDark,
    onDestructiveButton = VehicleLogColors.DangerDark,
    successSurface = VehicleLogColors.SuccessSurfaceDark,
    onSuccess = VehicleLogColors.SuccessInkDark,
    successEdge = VehicleLogColors.SuccessEdgeDark,
    modalScrim = VehicleLogColors.ScrimDark,
    quietWash = VehicleLogColors.WashDark,
    blueInk = VehicleLogColors.ActionDark,
    odometerContainer = VehicleLogColors.SurfaceDark,
    onOdometer = VehicleLogColors.InkDark,
    odometerMuted = VehicleLogColors.MutedInkDark,
    odometerDivider = VehicleLogColors.RuleDark,
    reviewMat = VehicleLogColors.CanvasDark,
)

internal val VehicleLogLightColorScheme = lightColorScheme(
    primary = VehicleLogColors.ActionLight,
    onPrimary = VehicleLogColors.OnActionLight,
    primaryContainer = VehicleLogColors.WashLight,
    onPrimaryContainer = VehicleLogColors.InkLight,
    secondary = VehicleLogColors.MutedInkLight,
    onSecondary = VehicleLogColors.OnActionLight,
    secondaryContainer = VehicleLogColors.WashLight,
    onSecondaryContainer = VehicleLogColors.InkLight,
    tertiary = VehicleLogColors.OnWarningLight,
    onTertiary = VehicleLogColors.WarningSurfaceLight,
    tertiaryContainer = VehicleLogColors.WarningSurfaceLight,
    onTertiaryContainer = VehicleLogColors.OnWarningLight,
    error = VehicleLogColors.DangerLight,
    onError = VehicleLogColors.OnDangerLight,
    errorContainer = VehicleLogColors.ErrorBackgroundLight,
    onErrorContainer = VehicleLogColors.DangerLight,
    background = VehicleLogColors.CanvasLight,
    onBackground = VehicleLogColors.InkLight,
    surface = VehicleLogColors.SurfaceLight,
    onSurface = VehicleLogColors.InkLight,
    surfaceVariant = VehicleLogColors.WashLight,
    onSurfaceVariant = VehicleLogColors.MutedInkLight,
    outline = VehicleLogColors.ControlOutlineLight,
    outlineVariant = VehicleLogColors.RuleLight,
    surfaceTint = VehicleLogColors.ActionLight,
    inverseSurface = VehicleLogColors.InkLight,
    inverseOnSurface = VehicleLogColors.CanvasLight,
    inversePrimary = VehicleLogColors.ActionDark,
    scrim = Color.Black,
    surfaceBright = VehicleLogColors.SurfaceLight,
    surfaceDim = Color(0xFFD6E2E5),
    surfaceContainerLowest = VehicleLogColors.SurfaceLight,
    surfaceContainerLow = Color(0xFFF6FAFB),
    surfaceContainer = Color(0xFFEAF1F3),
    surfaceContainerHigh = Color(0xFFE3ECEF),
    surfaceContainerHighest = Color(0xFFDCE8EB),
)

internal val VehicleLogDarkColorScheme = darkColorScheme(
    primary = VehicleLogColors.ActionDark,
    onPrimary = VehicleLogColors.OnActionDark,
    primaryContainer = VehicleLogColors.SurfaceElevatedDark,
    onPrimaryContainer = VehicleLogColors.InkDark,
    secondary = VehicleLogColors.LavenderSecondary,
    onSecondary = Color(0xFF1F1545),
    secondaryContainer = VehicleLogColors.SurfaceElevatedDark,
    onSecondaryContainer = VehicleLogColors.InkDark,
    tertiary = VehicleLogColors.OnWarningDark,
    onTertiary = VehicleLogColors.WarningSurfaceDark,
    tertiaryContainer = VehicleLogColors.WarningSurfaceDark,
    onTertiaryContainer = VehicleLogColors.OnWarningDark,
    error = VehicleLogColors.DangerDark,
    onError = VehicleLogColors.OnDangerDark,
    errorContainer = VehicleLogColors.ErrorBackgroundDark,
    onErrorContainer = VehicleLogColors.DangerDark,
    background = VehicleLogColors.CanvasDark,
    onBackground = VehicleLogColors.InkDark,
    surface = VehicleLogColors.SurfaceDark,
    onSurface = VehicleLogColors.InkDark,
    surfaceVariant = VehicleLogColors.SurfaceElevatedDark,
    onSurfaceVariant = VehicleLogColors.MutedInkDark,
    outline = VehicleLogColors.SurfaceElevatedDark,
    outlineVariant = VehicleLogColors.RuleDark,
    surfaceTint = VehicleLogColors.ActionDark,
    inverseSurface = VehicleLogColors.InkDark,
    inverseOnSurface = VehicleLogColors.CanvasDark,
    inversePrimary = VehicleLogColors.ActionLight,
    scrim = Color.Black,
    surfaceBright = VehicleLogColors.SurfaceElevatedDark,
    surfaceDim = VehicleLogColors.CanvasDark,
    surfaceContainerLowest = Color(0xFF09090D),
    surfaceContainerLow = Color(0xFF14131C),
    surfaceContainer = VehicleLogColors.SurfaceDark,
    surfaceContainerHigh = VehicleLogColors.SurfaceElevatedDark,
    surfaceContainerHighest = Color(0xFF322E45),
)
