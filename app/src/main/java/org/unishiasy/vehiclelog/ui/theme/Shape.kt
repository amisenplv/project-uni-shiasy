package org.unishiasy.vehiclelog.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Cohesive shape scale for Vehicle Ledger Concept C v2.
 *
 * Controls / inputs: 8dp
 * Buttons: 10dp
 * Cards / groups: 12dp
 * Notices / status badges: 6dp
 * Modal bottom sheets: 16dp
 */
val VehicleLogShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

val ButtonShape = RoundedCornerShape(10.dp)
val SheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
val NoticeShape = RoundedCornerShape(6.dp)
val ControlShape = RoundedCornerShape(8.dp)
val CardShape = RoundedCornerShape(12.dp)
