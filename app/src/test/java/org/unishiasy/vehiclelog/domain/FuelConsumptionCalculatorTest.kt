package org.unishiasy.vehiclelog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.unishiasy.vehiclelog.data.local.HistoryEntry

class FuelConsumptionCalculatorTest {

    private fun createEntry(
        id: String = "entry-1",
        odometerKm: Long? = 10_000L,
        fuelVolumeMl: Long? = 40_000L,
        isFullTank: Boolean = true,
        occurredOn: String = "2026-03-01",
        category: String = "fuel",
    ): HistoryEntry {
        return HistoryEntry(
            id = id,
            vehicleId = "veh-1",
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
}
