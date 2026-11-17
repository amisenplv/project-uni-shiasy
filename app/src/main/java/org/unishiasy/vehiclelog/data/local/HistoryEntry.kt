package org.unishiasy.vehiclelog.data.local

data class HistoryEntry(
    val id: String,
    val vehicleId: String,
    val category: String,
    val occurredOn: String,
    val createdAtEpochMillis: Long,
    val notes: String?,
    val amountRials: Long?,
    val fuelVolumeMl: Long?,
    val linkedOdometerKm: Long?,
)
