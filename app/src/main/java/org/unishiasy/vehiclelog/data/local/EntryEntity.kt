package org.unishiasy.vehiclelog.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["vehicleId", "occurredOn"])],
)
data class EntryEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val category: String,
    val occurredOn: String,
    val createdAtEpochMillis: Long,
    val notes: String?,
    val amountRials: Long?,
    val fuelVolumeMl: Long?,
    @ColumnInfo(name = "station_name") val stationName: String? = null,
    @ColumnInfo(name = "is_full_tank", defaultValue = "0") val isFullTank: Boolean = false,
)
