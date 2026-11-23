package org.unishiasy.vehiclelog.data.backup

import org.unishiasy.vehiclelog.data.ReminderInput
import org.unishiasy.vehiclelog.data.ReminderState
import org.unishiasy.vehiclelog.data.allowsUnpricedEntry
import org.unishiasy.vehiclelog.data.isKnownEntryCategory
import org.unishiasy.vehiclelog.data.isValid
import org.unishiasy.vehiclelog.data.isValidOccurrenceDate
import org.unishiasy.vehiclelog.data.local.OdometerReadingEntity

private val uuid = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

internal fun validateSnapshot(snapshot: BackupSnapshot) {
    checkSnapshot(
        snapshot.vehicles.size <= BackupCodec.MAX_VEHICLES && snapshot.entries.size <= BackupCodec.MAX_ENTRIES &&
            snapshot.readings.size <= BackupCodec.MAX_READINGS && snapshot.reminders.size <= BackupCodec.MAX_REMINDERS,
        BackupError.CollectionLimit,
    )
    val vehicles = rowIds(snapshot.vehicles) { it.id }
    rowIds(snapshot.entries) { it.id }
    rowIds(snapshot.readings) { it.id }
    rowIds(snapshot.reminders) { it.id }

    for (vehicle in snapshot.vehicles) {
        val year = vehicle.productionYear
        checkSnapshot(
            vehicle.kind == "car" && vehicle.displayLabel.isNotBlank() &&
                (vehicle.catalogKey == null || vehicle.catalogKey.isNotBlank()) &&
                if (year == null) vehicle.productionYearCalendar == null
                else year in 1..9999 && vehicle.productionYearCalendar in setOf("solar_hijri", "gregorian"),
            BackupError.InvalidVehicle,
        )
        validateText(vehicle.displayLabel, vehicle.catalogKey)
    }
    for (entry in snapshot.entries) {
        checkReference(entry.vehicleId, vehicles)
        val amount = entry.amountRials
        checkSnapshot(
            isValidOccurrenceDate(entry.occurredOn) && isKnownEntryCategory(entry.category) &&
                (amount?.let { it > 0 } ?: allowsUnpricedEntry(entry.category)) &&
                if (entry.category == "fuel") entry.fuelVolumeMl?.let { it > 0 } == true
                else entry.fuelVolumeMl == null,
            BackupError.InvalidEntry,
        )
        validateText(entry.notes, entry.stationName)
    }
    val entries = snapshot.entries.associateBy { it.id }
    val linkedEntries = HashSet<String>()
    for (reading in snapshot.readings) {
        checkReference(reading.vehicleId, vehicles)
        checkSnapshot(isValidOccurrenceDate(reading.occurredOn) && reading.kilometers >= 0, BackupError.InvalidReading)
        val entryId = reading.entryId ?: continue
        checkReference(entryId, entries.keys)
        val entry = entries.getValue(entryId)
        checkSnapshot(
            entry.vehicleId == reading.vehicleId && entry.occurredOn == reading.occurredOn,
            BackupError.BrokenReference,
        )
        checkSnapshot(linkedEntries.add(entryId), BackupError.InvalidReading)
    }
    for (reminder in snapshot.reminders) {
        checkReference(reminder.vehicleId, vehicles)
        checkSnapshot(
            ReminderState.entries.any { it.key == reminder.state } &&
                ReminderInput(reminder.title, reminder.dueOn, reminder.dueKilometers).isValid(),
            BackupError.InvalidReminder,
        )
        validateText(reminder.title)
    }
    validateMileageOrder(snapshot.readings)
}

private inline fun <T> rowIds(rows: List<T>, idOf: (T) -> String): Set<String> {
    val ids = HashSet<String>()
    for (row in rows) {
        val id = idOf(row)
        checkSnapshot(uuid.matches(id), BackupError.InvalidId)
        checkSnapshot(ids.add(id), BackupError.DuplicateId)
    }
    return ids
}

private fun checkReference(id: String, ids: Set<String>) {
    checkSnapshot(uuid.matches(id), BackupError.InvalidId)
    checkSnapshot(id in ids, BackupError.BrokenReference)
}

private fun validateText(vararg values: String?) {
    for (value in values.filterNotNull()) {
        checkSnapshot(value.length <= BackupCodec.MAX_BYTES, BackupError.SizeLimit)
        var index = 0
        while (index < value.length) {
            val character = value[index++]
            if (character.isHighSurrogate()) {
                checkSnapshot(value.getOrNull(index)?.isLowSurrogate() == true, BackupError.InvalidField)
                index++
            } else {
                checkSnapshot(!character.isLowSurrogate(), BackupError.InvalidField)
            }
        }
    }
}

private fun validateMileageOrder(readings: List<OdometerReadingEntity>) {
    var vehicleId: String? = null
    var day: String? = null
    var previousDaysMaximum = 0L
    var dayMaximum = 0L
    for (reading in readings.sortedWith(compareBy({ it.vehicleId }, { it.occurredOn }))) {
        if (reading.vehicleId != vehicleId) {
            vehicleId = reading.vehicleId
            day = null
            previousDaysMaximum = 0
            dayMaximum = 0
        }
        if (reading.occurredOn != day) {
            day = reading.occurredOn
            previousDaysMaximum = dayMaximum
            dayMaximum = 0
        }
        checkSnapshot(reading.kilometers >= previousDaysMaximum, BackupError.MileageOrder)
        dayMaximum = maxOf(dayMaximum, reading.kilometers)
    }
}
