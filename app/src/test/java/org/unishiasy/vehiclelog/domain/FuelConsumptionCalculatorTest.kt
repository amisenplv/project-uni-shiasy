package org.unishiasy.vehiclelog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.unishiasy.vehiclelog.data.local.HistoryEntry

class FuelConsumptionCalculatorTest {

    private fun createEntry(
        id: String = "entry-1",
        vehicleId: String = "veh-1",
        odometerKm: Long? = 10_000L,
        fuelVolumeMl: Long? = 40_000L,
        isFullTank: Boolean = true,
        occurredOn: String = "2026-03-01",
        category: String = "fuel",
    ): HistoryEntry {
        return HistoryEntry(
            id = id,
            vehicleId = vehicleId,
            category = category,
            occurredOn = occurredOn,
            createdAtEpochMillis = 1700000000000L,
            notes = null,
            amountRials = 1_200_000L,
            fuelVolumeMl = fuelVolumeMl,
            linkedOdometerKm = odometerKm,
            isFullTank = isFullTank,
        )
    }

    @Test
    fun returnsNullWhenEntriesEmpty() {
        val result = FuelConsumptionCalculator.calculateAverageL100Km(emptyList())
        assertNull(result)
    }

    @Test
    fun returnsNullWhenLessThanTwoFullTankEntries() {
        val singleEntry = listOf(createEntry(odometerKm = 10_000L, fuelVolumeMl = 40_000L, isFullTank = true))
        assertNull(FuelConsumptionCalculator.calculateAverageL100Km(singleEntry))

        val twoEntriesOneNotFull = listOf(
            createEntry(id = "1", odometerKm = 10_000L, fuelVolumeMl = 40_000L, isFullTank = true),
            createEntry(id = "2", odometerKm = 10_500L, fuelVolumeMl = 30_000L, isFullTank = false),
        )
        assertNull(FuelConsumptionCalculator.calculateAverageL100Km(twoEntriesOneNotFull))
    }

    @Test
    fun returnsNullWhenOdometerOrVolumeIsNull() {
        val nullOdometer = listOf(
            createEntry(id = "1", odometerKm = null, fuelVolumeMl = 40_000L, isFullTank = true),
            createEntry(id = "2", odometerKm = 10_500L, fuelVolumeMl = 40_000L, isFullTank = true),
        )
        assertNull(FuelConsumptionCalculator.calculateAverageL100Km(nullOdometer))

        val nullVolume = listOf(
            createEntry(id = "1", odometerKm = 10_000L, fuelVolumeMl = 40_000L, isFullTank = true),
            createEntry(id = "2", odometerKm = 10_500L, fuelVolumeMl = null, isFullTank = true),
        )
        assertNull(FuelConsumptionCalculator.calculateAverageL100Km(nullVolume))
    }

    @Test
    fun calculatesAverageForConsecutiveFullTankFillUps() {
        // 40L over 500km = 8.0 L/100km
        val entries = listOf(
            createEntry(id = "1", odometerKm = 10_000L, fuelVolumeMl = 50_000L, isFullTank = true),
            createEntry(id = "2", odometerKm = 10_500L, fuelVolumeMl = 40_000L, isFullTank = true),
        )
        val result = FuelConsumptionCalculator.calculateAverageL100Km(entries)
        assertEquals(8.0, result!!, 0.001)
    }

    @Test
    fun calculatesAverageAcrossMultipleIntervals() {
        // Interval 1: 500km, 40L
        // Interval 2: 500km, 50L
        // Total: 1000km, 90L -> 9.0 L/100km
        val entries = listOf(
            createEntry(id = "1", odometerKm = 10_000L, fuelVolumeMl = 40_000L, isFullTank = true),
            createEntry(id = "2", odometerKm = 10_500L, fuelVolumeMl = 40_000L, isFullTank = true),
            createEntry(id = "3", odometerKm = 11_000L, fuelVolumeMl = 50_000L, isFullTank = true),
        )
        val result = FuelConsumptionCalculator.calculateAverageL100Km(entries)
        assertEquals(9.0, result!!, 0.001)
    }

    @Test
    fun sortsUnorderedEntriesCorrectly() {
        val entries = listOf(
            createEntry(id = "2", odometerKm = 10_500L, fuelVolumeMl = 40_000L, isFullTank = true),
            createEntry(id = "1", odometerKm = 10_000L, fuelVolumeMl = 50_000L, isFullTank = true),
        )
        val result = FuelConsumptionCalculator.calculateAverageL100Km(entries)
        assertEquals(8.0, result!!, 0.001)
    }

    @Test
    fun returnsNullWhenTotalDistanceDeltaIsZeroOrNegative() {
        val entries = listOf(
            createEntry(id = "1", odometerKm = 10_500L, fuelVolumeMl = 40_000L, isFullTank = true),
            createEntry(id = "2", odometerKm = 10_500L, fuelVolumeMl = 40_000L, isFullTank = true),
        )
        assertNull(FuelConsumptionCalculator.calculateAverageL100Km(entries))
    }

    @Test
    fun mixedVehiclesDoNotInterleaveOdometers() {
        // Vehicle 1: 10,000km -> 10,500km (delta 500km, 40L -> 8.0 L/100km)
        // Vehicle 2: 50,000km -> 50,500km (delta 500km, 40L -> 8.0 L/100km)
        // If odometers were sorted globally without grouping by vehicleId,
        // it would calculate delta 10,500 -> 50,000 (39,500km) which would corrupt calculations.
        val entries = listOf(
            createEntry(id = "v1-1", vehicleId = "veh-1", odometerKm = 10_000L, fuelVolumeMl = 50_000L),
            createEntry(id = "v2-1", vehicleId = "veh-2", odometerKm = 50_000L, fuelVolumeMl = 50_000L),
            createEntry(id = "v1-2", vehicleId = "veh-1", odometerKm = 10_500L, fuelVolumeMl = 40_000L),
            createEntry(id = "v2-2", vehicleId = "veh-2", odometerKm = 50_500L, fuelVolumeMl = 40_000L),
        )

        val average = FuelConsumptionCalculator.calculateAverageL100Km(entries)
        // Total distance = 500 + 500 = 1000km, Total fuel = 40L + 40L = 80L -> 8.0 L/100km
        assertEquals(8.0, average!!, 0.001)

        val intervals = FuelConsumptionCalculator.calculateIntervals(entries)
        assertEquals(2, intervals.size)
        assertEquals(500L, intervals[0].distanceKm)
        assertEquals(8.0, intervals[0].consumptionL100Km, 0.001)
        assertEquals(500L, intervals[1].distanceKm)
        assertEquals(8.0, intervals[1].consumptionL100Km, 0.001)
    }

    @Test
    fun filtersByTargetVehicleIdWhenSpecified() {
        val entries = listOf(
            // veh-1: 500km delta, 40L -> 8.0 L/100km
            createEntry(id = "v1-1", vehicleId = "veh-1", odometerKm = 10_000L, fuelVolumeMl = 50_000L),
            createEntry(id = "v1-2", vehicleId = "veh-1", odometerKm = 10_500L, fuelVolumeMl = 40_000L),
            // veh-2: 500km delta, 50L -> 10.0 L/100km
            createEntry(id = "v2-1", vehicleId = "veh-2", odometerKm = 50_000L, fuelVolumeMl = 50_000L),
            createEntry(id = "v2-2", vehicleId = "veh-2", odometerKm = 50_500L, fuelVolumeMl = 50_000L),
        )

        val veh1Result = FuelConsumptionCalculator.calculateAverageL100Km(entries, targetVehicleId = "veh-1")
        assertEquals(8.0, veh1Result!!, 0.001)

        val veh2Result = FuelConsumptionCalculator.calculateAverageL100Km(entries, targetVehicleId = "veh-2")
        assertEquals(10.0, veh2Result!!, 0.001)

        val nonExistentResult = FuelConsumptionCalculator.calculateAverageL100Km(entries, targetVehicleId = "veh-999")
        assertNull(nonExistentResult)
    }
}
