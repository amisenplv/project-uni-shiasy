package org.unishiasy.vehiclelog.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val displayLabel: String,
    val catalogKey: String? = null,
    val productionYear: Int? = null,
    val productionYearCalendar: String? = null,
)
