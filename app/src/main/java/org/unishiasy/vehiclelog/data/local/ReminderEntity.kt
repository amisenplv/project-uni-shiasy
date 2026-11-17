package org.unishiasy.vehiclelog.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("vehicleId")],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val title: String,
    val dueOn: String?,
    val dueKilometers: Long?,
    val state: String,
)
