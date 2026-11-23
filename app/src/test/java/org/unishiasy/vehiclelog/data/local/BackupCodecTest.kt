package org.unishiasy.vehiclelog.data.local

import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.unishiasy.vehiclelog.data.backup.BackupCodec
import org.unishiasy.vehiclelog.data.backup.BackupError
import org.unishiasy.vehiclelog.data.backup.BackupResult
import org.unishiasy.vehiclelog.data.backup.BackupSnapshot

class BackupCodecTest {
    private val time = Instant.parse("2026-11-23T12:00:00Z")
    private val vehicle = VehicleEntity(
        id = UUID(0, 1).toString(),
        kind = "car",
        displayLabel = "پژو ۲۰۶",
    )

    @Test
    fun roundTripPreservesStationNameAndFullTankFlag() {
        val entry = EntryEntity(
            id = UUID(0, 2).toString(),
            vehicleId = vehicle.id,
            category = "fuel",
            occurredOn = "2026-11-23",
            createdAtEpochMillis = 1_794_268_800_000L,
            notes = "بنزین سوپر",
            amountRials = 300_000L,
            fuelVolumeMl = 30_000L,
            stationName = "جایگاه بهاران",
            isFullTank = true,
        )
        val snapshot = BackupSnapshot(
            exportedAt = time,
            vehicles = listOf(vehicle),
            entries = listOf(entry),
        )

        val encoded = (BackupCodec.encode(snapshot) as BackupResult.Success).value
        val decoded = (BackupCodec.decode(encoded) as BackupResult.Success).value

        val decodedEntry = decoded.entries.single()
        assertEquals("جایگاه بهاران", decodedEntry.stationName)
        assertTrue(decodedEntry.isFullTank)
        assertEquals(entry, decodedEntry)
    }

    @Test
    fun decodesLegacyJsonWithoutStationNameOrFullTankWithDefaults() {
        // Legacy JSON from v4 without stationName or isFullTank
        val legacyJson = """
        {
          "formatVersion": 1,
          "exportedAt": "2026-11-23T12:00:00Z",
          "currencyUnit": "legacy_irr",
          "vehicles": [
            {
              "id": "${vehicle.id}",
              "kind": "car",
              "displayLabel": "پژو ۲۰۶",
              "catalogKey": null,
              "productionYear": null,
              "productionYearCalendar": null
            }
          ],
          "entries": [
            {
              "id": "${UUID(0, 2)}",
              "vehicleId": "${vehicle.id}",
              "category": "fuel",
              "occurredOn": "2026-11-23",
              "createdAtEpochMillis": 1794268800000,
              "notes": "بنزین",
              "amountRials": 300000,
              "fuelVolumeMl": 30000
            }
          ],
          "readings": [],
          "reminders": []
        }
        """.trimIndent()

        val decoded = (BackupCodec.decode(legacyJson.toByteArray(Charsets.UTF_8)) as BackupResult.Success).value
        val entry = decoded.entries.single()
        assertNull(entry.stationName)
        assertFalse(entry.isFullTank)
    }

    @Test
    fun rejectsInvalidStationNameOrFullTankType() {
        val entry = EntryEntity(
            id = UUID(0, 2).toString(),
            vehicleId = vehicle.id,
            category = "fuel",
            occurredOn = "2026-11-23",
            createdAtEpochMillis = 1_794_268_800_000L,
            notes = "بنزین",
            amountRials = 300_000L,
            fuelVolumeMl = 30_000L,
            stationName = "جایگاه",
            isFullTank = true,
        )
        val snapshot = BackupSnapshot(time, listOf(vehicle), listOf(entry))
        val validJson = (BackupCodec.encode(snapshot) as BackupResult.Success).value.decodeToString()

        // Invalid boolean (string instead of boolean)
        val invalidBoolJson = validJson.replace("\"isFullTank\":true", "\"isFullTank\":\"true\"")
        assertEquals(
            BackupResult.Failure(BackupError.InvalidField),
            BackupCodec.decode(invalidBoolJson.toByteArray(Charsets.UTF_8)),
        )

        // Invalid station name (unpaired surrogate)
        val invalidSurrogateJson = validJson.replace("جایگاه", "\\uD800")
        assertEquals(
            BackupResult.Failure(BackupError.InvalidField),
            BackupCodec.decode(invalidSurrogateJson.toByteArray(Charsets.UTF_8)),
        )
    }
}
