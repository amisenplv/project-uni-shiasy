package org.unishiasy.vehiclelog.data.backup

import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.unishiasy.vehiclelog.data.local.EntryEntity
import org.unishiasy.vehiclelog.data.local.OdometerReadingEntity
import org.unishiasy.vehiclelog.data.local.ReminderEntity
import org.unishiasy.vehiclelog.data.local.VehicleEntity

class BackupCodecTest {
    private val time = Instant.parse("2026-11-14T12:00:00.123456789Z")
    private val car = VehicleEntity(id(1), "car", "پژو LX 🚙", "catalog-key", 1400, "solar_hijri")
    private val other = VehicleEntity(id(2), "car", "خودرو")
    private val fuel = EntryEntity(id(3), car.id, "fuel", "2120-03-21", 42,
        " یادداشت\n \"سوخت\" ", Long.MAX_VALUE, 32450)
    private val service = EntryEntity(id(4), other.id, "oil_change", "2028-02-29", -1, null, null, null)
    private val linked = OdometerReadingEntity(id(5), car.id, fuel.occurredOn, 120, fuel.id)
    private val standalone = OdometerReadingEntity(id(6), car.id, fuel.occurredOn, 100, null)
    private val later = OdometerReadingEntity(id(7), car.id, "2120-03-22", 120, null)
    private val date = ReminderEntity(id(8), car.id, "تعویض روغن", "2028-02-29", null, "active")
    private val mileage = ReminderEntity(id(9), other.id, "بازبینی", null, Long.MAX_VALUE, "completed")
    private val canceled = ReminderEntity(id(10), car.id, "سرویس", null, 0, "canceled")
    private val full = BackupSnapshot(time, listOf(car, other), listOf(fuel, service),
        listOf(linked, standalone, later), listOf(date, mileage, canceled))

    @Test fun fullRoundTripPreservesEveryFieldAndExactLongs() {
        val encoded = encode(full)
        assertTrue(encoded.decodeToString().contains("\"amountRials\":9223372036854775807"))
        val restored = success(BackupCodec.decode(encoded))
        assertEquals(full, restored)
        assertArrayEquals(encoded, encode(restored))
    }

    @Test fun emptySnapshotRoundTrips() {
        val empty = BackupSnapshot(time)
        assertEquals(empty, success(BackupCodec.decode(encode(empty))))
    }

    @Test fun deterministicOrderingAcrossAllCollections() {
        assertArrayEquals(encode(full), encode(full.copy(
            vehicles = full.vehicles.reversed(), entries = full.entries.reversed(),
            readings = full.readings.reversed(), reminders = full.reminders.reversed(),
        )))
    }

    @Test fun acceptsExactByteLimitAndRejectsBeforeParsingAboveIt() {
        val json = encode(BackupSnapshot(time))
        val padded = ByteArray(BackupCodec.MAX_BYTES) { ' '.code.toByte() }
        json.copyInto(padded)
        assertEquals(BackupSnapshot(time), success(BackupCodec.decode(padded)))
        failure(BackupError.SizeLimit, BackupCodec.decode(ByteArray(BackupCodec.MAX_BYTES + 1)))

        val emptyNotes = full.copy(entries = listOf(fuel.copy(notes = ""), service))
        val overhead = encode(emptyNotes).size
        val notes = "x".repeat(BackupCodec.MAX_BYTES - overhead)
        val maxSnapshot = emptyNotes.copy(entries = listOf(fuel.copy(notes = notes), service))
        val encoded = encode(maxSnapshot)
        assertEquals(BackupCodec.MAX_BYTES, encoded.size)
        assertEquals(maxSnapshot, success(BackupCodec.decode(encoded)))
        failure(BackupError.SizeLimit, BackupCodec.encode(
            maxSnapshot.copy(entries = listOf(fuel.copy(notes = notes + "é"), service)),
        ))
    }

    @Test fun collectionLimitAppliesToBothDirections() {
        val vehicles = List(BackupCodec.MAX_VEHICLES) { car.copy(id = id(it + 20)) }
        val snapshot = BackupSnapshot(time, vehicles = vehicles)
        assertEquals(snapshot, success(BackupCodec.decode(encode(snapshot))))
        failure(BackupError.CollectionLimit, BackupCodec.encode(snapshot.copy(vehicles = vehicles + other)))
        val extra = encode(BackupSnapshot(time, vehicles = listOf(other))).decodeToString()
            .substringAfter("\"vehicles\":[").substringBefore("],\"entries")
        altered(BackupError.CollectionLimit, encode(snapshot).decodeToString().replace("],\"entries", ",$extra],\"entries"))
    }

    @Test fun rejectsVersionCurrencyAndMissingOrWrongTypeFields() {
        val json = encode(full).decodeToString()
        altered(BackupError.UnsupportedVersion, json.replace("\"formatVersion\":1", "\"formatVersion\":2"))
        altered(BackupError.UnsupportedCurrency, json.replace("legacy_irr", "toman"))
        altered(BackupError.MissingField, json.replace("\"catalogKey\":null,", ""))
        altered(BackupError.MissingField, json.replace("\"formatVersion\":1,", ""))
        altered(BackupError.InvalidField, json.replace("9223372036854775807", "9223372036854775808"))
        altered(BackupError.InvalidField, json.replace("9223372036854775807", "\"9223372036854775807\""))
        altered(BackupError.InvalidField, json.replace("9223372036854775807", "1e3"))
        altered(BackupError.InvalidField, json.replace(time.toString(), "2026-11-14"))
    }

    @Test fun rejectsMalformedJsonUtf8AndUnicodeLoss() {
        val json = encode(full).decodeToString()
        for (malformed in listOf(
            "{", json + "{}", json.replace("\"formatVersion\"", "formatVersion"),
            json.replace("legacy_irr", "legacy\u0001irr"),
            json.replace(":32450", ":032450"), json.dropLast(1) + ",}",
            "[".repeat(10_000) + "]".repeat(10_000),
        )) altered(BackupError.MalformedJson, malformed)
        failure(BackupError.MalformedJson, BackupCodec.decode(byteArrayOf(0xc3.toByte(), 0x28)))
        altered(BackupError.CollectionLimit, "[" + "0,".repeat(350_000) + "0]")
        altered(BackupError.InvalidField, json.replace("catalog-key", "\\uD800"))
        failure(BackupError.InvalidField, BackupCodec.encode(full.copy(vehicles = listOf(car.copy(displayLabel = "\uD800"), other))))
    }

    @Test fun excludesDeviceMetadataAndRejectsUnknownAndDuplicateKeys() {
        val json = encode(full).decodeToString()
        for (field in listOf("deliveryMarkers", "workManager", "permissionState", "signingCredentials")) {
            assertFalse(json.contains(field))
            altered(BackupError.UnknownField, json.dropLast(1) + ",\"$field\":null}")
        }
        altered(BackupError.UnknownField, json.replace("\"state\":\"active\"", "\"state\":\"active\",\"delivered\":true"))
        altered(BackupError.DuplicateField, json.replace("\"formatVersion\":1", "\"formatVersion\":1,\"formatVersion\":1"))
        altered(BackupError.DuplicateField, json.replace("\"state\":\"active\"", "\"state\":\"active\",\"st\\u0061te\":\"active\""))
    }

    @Test fun rejectsInvalidAndDuplicateIdsAndBrokenLinks() {
        invalid(BackupError.InvalidId, full.copy(vehicles = listOf(car.copy(id = "1-1-1-1-1"), other)))
        invalid(BackupError.DuplicateId, full.copy(vehicles = listOf(car, car)))
        invalid(BackupError.DuplicateId, full.copy(entries = listOf(fuel, fuel)))
        invalid(BackupError.DuplicateId, full.copy(readings = listOf(linked, linked)))
        invalid(BackupError.DuplicateId, full.copy(reminders = listOf(date, date)))
        invalid(BackupError.BrokenReference, full.copy(vehicles = listOf(car)))
        invalid(BackupError.BrokenReference, full.copy(readings = listOf(linked.copy(entryId = id(20)))))
        invalid(BackupError.BrokenReference, full.copy(readings = listOf(linked.copy(vehicleId = other.id))))
        invalid(BackupError.BrokenReference, full.copy(readings = listOf(linked.copy(occurredOn = "2120-03-22"))))
        invalid(BackupError.InvalidReading, full.copy(readings = listOf(linked, linked.copy(id = id(20)))))
    }

    @Test fun rejectsInvalidStoredValues() {
        invalid(BackupError.InvalidVehicle, full.copy(vehicles = listOf(car.copy(productionYear = null), other)))
        for (badEntry in listOf(
            fuel.copy(occurredOn = "2026-02-29"), fuel.copy(amountRials = 0), fuel.copy(amountRials = null),
            fuel.copy(fuelVolumeMl = -1), fuel.copy(category = "unknown"), fuel.copy(category = "wash"),
        )) invalid(BackupError.InvalidEntry, full.copy(entries = listOf(badEntry, service)))
        invalid(BackupError.InvalidReading, full.copy(readings = listOf(linked.copy(kilometers = -1))))
        invalid(BackupError.InvalidReading, full.copy(readings = listOf(standalone.copy(occurredOn = "0000-01-01"))))
        for (badReminder in listOf(
            date.copy(dueOn = "2026-02-29"), date.copy(dueKilometers = 1), date.copy(dueOn = null),
            date.copy(title = " "), date.copy(state = "pending"), mileage.copy(dueKilometers = -1),
        )) invalid(BackupError.InvalidReminder, full.copy(reminders = listOf(badReminder)))
    }

    @Test fun checksAcrossDaysUsingEarlierMaximumAndResetsPerVehicle() {
        invalid(BackupError.MileageOrder, full.copy(readings = listOf(linked, standalone, later.copy(kilometers = 119))))
        val separateVehicle = later.copy(vehicleId = other.id, kilometers = 0)
        val valid = full.copy(readings = listOf(linked, standalone, separateVehicle))
        assertEquals(valid, success(BackupCodec.decode(encode(valid))))
    }

    private fun encode(snapshot: BackupSnapshot) = success(BackupCodec.encode(snapshot))
    private fun <T> success(result: BackupResult<T>): T = (result as BackupResult.Success).value
    private fun altered(reason: BackupError, json: String) = failure(reason, BackupCodec.decode(json.encodeToByteArray()))
    private fun invalid(reason: BackupError, snapshot: BackupSnapshot) = failure(reason, BackupCodec.encode(snapshot))
    private fun failure(reason: BackupError, result: BackupResult<*>) = assertEquals(BackupResult.Failure(reason), result)
    private fun id(value: Int) = UUID(0, value.toLong()).toString()
}
