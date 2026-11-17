package org.unishiasy.vehiclelog.data.backup

import java.time.Instant
import org.unishiasy.vehiclelog.data.local.EntryEntity
import org.unishiasy.vehiclelog.data.local.OdometerReadingEntity
import org.unishiasy.vehiclelog.data.local.ReminderEntity
import org.unishiasy.vehiclelog.data.local.VehicleEntity

internal data class BackupSnapshot(
    val exportedAt: Instant,
    val vehicles: List<VehicleEntity> = emptyList(),
    val entries: List<EntryEntity> = emptyList(),
    val readings: List<OdometerReadingEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
)

internal enum class BackupError {
    SizeLimit, CollectionLimit, MalformedJson, DuplicateField, UnknownField, MissingField, InvalidField,
    UnsupportedVersion, UnsupportedCurrency, InvalidId, DuplicateId, BrokenReference,
    InvalidVehicle, InvalidEntry, InvalidReading, InvalidReminder, MileageOrder,
}

internal sealed interface BackupResult<out T> {
    data class Success<T>(val value: T) : BackupResult<T>
    data class Failure(val reason: BackupError) : BackupResult<Nothing>
}

internal class InvalidSnapshot(val reason: BackupError) : RuntimeException()

internal fun checkSnapshot(condition: Boolean, error: BackupError) {
    if (!condition) throw InvalidSnapshot(error)
}
