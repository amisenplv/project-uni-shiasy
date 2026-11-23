package org.unishiasy.vehiclelog.domain

import org.unishiasy.vehiclelog.data.local.HistoryEntry

/**
 * Data structure representing a calculated consumption interval between two full-tank fills.
 */
data class FuelConsumptionInterval(
    val previousOdometerKm: Long,
    val currentOdometerKm: Long,
    val distanceKm: Long,
    val fuelLiters: Double,
    val consumptionL100Km: Double,
)

/**
 * Calculator for vehicle fuel consumption (L / 100km).
 *
 * Uses the standard full-tank method:
 * - Driver fills tank completely.
 * - Driver drives distance X km.
 * - Driver fills tank completely again with Y liters.
 * - Consumption = (Y / X) * 100.
 */
object FuelConsumptionCalculator {

    /**
     * Calculates the overall average fuel consumption in Liters per 100 Kilometers (L/100km).
     *
     * Rules:
     * - Filter entries where [HistoryEntry.isFullTank] is true, [HistoryEntry.fuelVolumeMl] is not null and > 0,
     *   and [HistoryEntry.linkedOdometerKm] is not null.
     * - Entries are grouped by [HistoryEntry.vehicleId] to prevent interleaving odometer readings
     *   across different vehicles.
     * - Within each vehicle, entries are sorted ascending by odometer reading (and date).
     * - Requires at least 2 full-tank entries for a vehicle.
     * - For consecutive full-tank entries, deltaKm = current.linkedOdometerKm - prev.linkedOdometerKm.
     * - If deltaKm > 0, the interval consumption is (current.fuelVolumeMl / 1000.0 / deltaKm) * 100.
     * - Overall average = (total liters consumed between full tanks / total distance delta) * 100.
     * - Returns null if fewer than 2 valid entries or total distance delta <= 0.
     */
    fun calculateAverageL100Km(
        entries: List<HistoryEntry>,
        targetVehicleId: String? = null,
    ): Double? {
        val filtered = entries.filter { entry ->
            (targetVehicleId == null || entry.vehicleId == targetVehicleId) &&
                (entry.category.isEmpty() || entry.category.equals("fuel", ignoreCase = true)) &&
                entry.isFullTank &&
                (entry.fuelVolumeMl ?: 0L) > 0L &&
                entry.linkedOdometerKm != null
        }

        if (filtered.isEmpty()) return null

        val vehicleGroups = filtered.groupBy { it.vehicleId }

        var totalDistanceKm = 0L
        var totalFuelMl = 0L

        for ((_, vehicleEntries) in vehicleGroups) {
            val sorted = vehicleEntries.sortedWith(
                compareBy<HistoryEntry> { it.linkedOdometerKm }
                    .thenBy { it.occurredOn }
                    .thenBy { it.createdAtEpochMillis }
            )

            if (sorted.size < 2) continue

            for (i in 1 until sorted.size) {
                val prev = sorted[i - 1]
                val curr = sorted[i]

                val deltaKm = (curr.linkedOdometerKm ?: 0L) - (prev.linkedOdometerKm ?: 0L)
                if (deltaKm > 0) {
                    totalDistanceKm += deltaKm
                    totalFuelMl += (curr.fuelVolumeMl ?: 0L)
                }
            }
        }

        if (totalDistanceKm <= 0L || totalFuelMl <= 0L) return null

        val totalLiters = totalFuelMl / 1000.0
        return (totalLiters / totalDistanceKm) * 100.0
    }

    /**
     * Calculates consumption per interval between consecutive full-tank entries,
     * grouped safely by vehicle.
     */
    fun calculateIntervals(
        entries: List<HistoryEntry>,
        targetVehicleId: String? = null,
    ): List<FuelConsumptionInterval> {
        val filtered = entries.filter { entry ->
            (targetVehicleId == null || entry.vehicleId == targetVehicleId) &&
                (entry.category.isEmpty() || entry.category.equals("fuel", ignoreCase = true)) &&
                entry.isFullTank &&
                (entry.fuelVolumeMl ?: 0L) > 0L &&
                entry.linkedOdometerKm != null
        }

        if (filtered.isEmpty()) return emptyList()

        val vehicleGroups = filtered.groupBy { it.vehicleId }
        val intervals = mutableListOf<FuelConsumptionInterval>()

        for ((_, vehicleEntries) in vehicleGroups) {
            val sorted = vehicleEntries.sortedWith(
                compareBy<HistoryEntry> { it.linkedOdometerKm }
                    .thenBy { it.occurredOn }
                    .thenBy { it.createdAtEpochMillis }
            )

            if (sorted.size < 2) continue

            for (i in 1 until sorted.size) {
                val prev = sorted[i - 1]
                val curr = sorted[i]

                val prevOdo = prev.linkedOdometerKm ?: continue
                val currOdo = curr.linkedOdometerKm ?: continue
                val deltaKm = currOdo - prevOdo
                val fuelMl = curr.fuelVolumeMl ?: continue

                if (deltaKm > 0 && fuelMl > 0) {
                    val liters = fuelMl / 1000.0
                    val consumption = (liters / deltaKm) * 100.0
                    intervals.add(
                        FuelConsumptionInterval(
                            previousOdometerKm = prevOdo,
                            currentOdometerKm = currOdo,
                            distanceKm = deltaKm,
                            fuelLiters = liters,
                            consumptionL100Km = consumption,
                        )
                    )
                }
            }
        }
        return intervals
    }
}
