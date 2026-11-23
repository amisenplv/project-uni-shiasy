package org.unishiasy.vehiclelog.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleLogThemeTest {

    @Test
    fun lightColorSchemeMatchesApprovedDesign() {
        assertEquals(Color(0xFF075A63), VehicleLogLightColorScheme.primary)
        assertEquals(Color(0xFFA12C30), VehicleLogLightColorScheme.error)
        assertEquals(Color(0xFFF3F6F8), VehicleLogLightColorScheme.background)
        assertEquals(Color(0xFFFFFFFF), VehicleLogLightColorScheme.surface)
        assertEquals(Color(0xFFFFF0D3), VehicleLogLightSemanticColors.warningSurface)
    }

    @Test
    fun darkColorSchemeMatchesApprovedDesign() {
        assertEquals(Color(0xFF9D85F2), VehicleLogDarkColorScheme.primary)
        assertEquals(Color(0xFFFFB4AB), VehicleLogDarkColorScheme.error)
        assertEquals(Color(0xFF0E0E13), VehicleLogDarkColorScheme.background)
        assertEquals(Color(0xFF1A1824), VehicleLogDarkColorScheme.surface)
        assertEquals(Color(0xFF382A16), VehicleLogDarkSemanticColors.warningSurface)
    }

    @Test
    fun cardAndModalCornersUseDistinctMaterialRoles() {
        val size = androidx.compose.ui.geometry.Size(100f, 100f)
        val density = androidx.compose.ui.unit.Density(1f)
        assertEquals(12.dp, (VehicleLogShapes.medium as RoundedCornerShape).topStart.toPx(size, density).dp)
        assertEquals(28.dp, (VehicleLogShapes.extraLarge as RoundedCornerShape).topStart.toPx(size, density).dp)
        assertEquals(28.dp, SheetShape.topStart.toPx(size, density).dp)
        assertEquals(0.dp, SheetShape.bottomStart.toPx(size, density).dp)
    }

    @Test
    fun typographyConfiguredForPersianWithoutLetterSpacing() {
        assertEquals(0.sp, VehicleLogTypography.headlineLarge.letterSpacing)
        assertEquals(0.sp, VehicleLogTypography.headlineMedium.letterSpacing)
        assertEquals(0.sp, VehicleLogTypography.bodyLarge.letterSpacing)
        assertEquals("fa", VehicleLogTypography.bodyLarge.localeList?.get(0)?.language)
    }

    @Test
    fun warningTokensMeetWcagAaContrastInLightAndDarkThemes() {
        assertTrue(contrastRatio(VehicleLogLightSemanticColors.onWarningEdge, VehicleLogLightSemanticColors.warningEdge) >= 4.5)
        assertTrue(contrastRatio(VehicleLogDarkSemanticColors.onWarningEdge, VehicleLogDarkSemanticColors.warningEdge) >= 4.5)
        assertTrue(contrastRatio(VehicleLogLightSemanticColors.onWarning, VehicleLogLightSemanticColors.warningSurface) >= 4.5)
        assertTrue(contrastRatio(VehicleLogDarkSemanticColors.onWarning, VehicleLogDarkSemanticColors.warningSurface) >= 4.5)
    }

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val l1 = foreground.luminance().toDouble()
        val l2 = background.luminance().toDouble()
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
