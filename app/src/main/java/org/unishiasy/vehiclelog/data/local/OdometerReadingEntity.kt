package org.unishiasy.vehiclelog.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "odometer_readings",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["vehicleId", "occurredOn"]),
        Index(value = ["entryId"], unique = true),
    ],
)
data class OdometerReadingEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val occurredOn: String,
    val kilometers: Long,
    val entryId: String?,
)
