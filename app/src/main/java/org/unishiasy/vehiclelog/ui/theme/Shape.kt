package org.unishiasy.vehiclelog.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Material 3 shape scale, shared by controls, cards, and modal surfaces. */
val VehicleLogShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val ButtonShape = androidx.compose.foundation.shape.CircleShape
val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val NoticeShape = VehicleLogShapes.medium
val ControlShape = VehicleLogShapes.small
val CardShape = VehicleLogShapes.medium
