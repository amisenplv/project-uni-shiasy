package org.unishiasy.vehiclelog.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
        assertEquals(Color(0xFFA6DEDF), VehicleLogDarkColorScheme.primary)
        assertEquals(Color(0xFFFFB4AB), VehicleLogDarkColorScheme.error)
        assertEquals(Color(0xFF101D22), VehicleLogDarkColorScheme.background)
        assertEquals(Color(0xFF192B31), VehicleLogDarkColorScheme.surface)
        assertEquals(Color(0xFF382A16), VehicleLogDarkSemanticColors.warningSurface)
    }

    @Test
    fun shapesEliminateOversizedPillButtons() {
        val mediumCorner = (VehicleLogShapes.medium as RoundedCornerShape).topStart
        val largeCorner = (VehicleLogShapes.large as RoundedCornerShape).topStart
        assertEquals(8.dp, mediumCorner.toPx(androidx.compose.ui.geometry.Size(100f, 100f), androidx.compose.ui.unit.Density(1f)).dp)
        assertEquals(12.dp, largeCorner.toPx(androidx.compose.ui.geometry.Size(100f, 100f), androidx.compose.ui.unit.Density(1f)).dp)
        assertNotEquals(28.dp, mediumCorner.toPx(androidx.compose.ui.geometry.Size(100f, 100f), androidx.compose.ui.unit.Density(1f)).dp)
    }

    @Test
    fun typographyConfiguredForPersianWithoutLetterSpacing() {
        assertEquals(0.sp, VehicleLogTypography.headlineLarge.letterSpacing)
        assertEquals(0.sp, VehicleLogTypography.headlineMedium.letterSpacing)
        assertEquals(0.sp, VehicleLogTypography.bodyLarge.letterSpacing)
        assertEquals("fa", VehicleLogTypography.bodyLarge.localeList?.get(0)?.language)
    }
}
