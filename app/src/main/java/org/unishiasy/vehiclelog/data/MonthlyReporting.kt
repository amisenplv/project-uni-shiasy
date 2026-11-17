package org.unishiasy.vehiclelog.data

import java.math.BigInteger
import org.unishiasy.vehiclelog.data.local.EntryEntity
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.JalaliMonth

internal data class VehicleMonthlyReport(
    val months: List<MonthlyReport>,
    val unassignedEntries: List<UnassignedReportEntry>,
)

internal data class MonthlyReport(
    val month: JalaliMonth,
    val totals: ReportTotals,
    val categories: Map<String, ReportTotals>,
)

internal data class ReportTotals(val entryCount: Int, val totalRials: BigInteger, val unpricedEntryCount: Int = 0) {
    // A null result signals that a Long-based consumer cannot represent the exact total.
    fun totalRialsAsLongOrNull(): Long? = totalRials.takeIf { it.bitLength() <= 63 }?.toLong()
}

internal data class UnassignedReportEntry(val entry: EntryEntity, val reason: ReportDateIssue)

internal enum class ReportDateIssue {
    OutsideSupportedJalaliRange,
    InvalidGregorianDate,
}

internal fun calculateMonthlyReport(vehicleId: String, entries: List<EntryEntity>): VehicleMonthlyReport {
    val byMonth = mutableMapOf<JalaliMonth, MutableList<EntryEntity>>()
    val unassigned = mutableListOf<UnassignedReportEntry>()
    for (entry in entries) {
        if (entry.vehicleId != vehicleId) continue
        val date = JalaliDates.parseGregorian(entry.occurredOn)
        val month = date?.let(JalaliDates::monthOf)
        when {
            date == null -> unassigned.add(UnassignedReportEntry(entry, ReportDateIssue.InvalidGregorianDate))
            month == null -> unassigned.add(UnassignedReportEntry(entry, ReportDateIssue.OutsideSupportedJalaliRange))
            else -> byMonth.getOrPut(month) { mutableListOf() }.add(entry)
        }
    }

    val months = byMonth
        .map { (month, monthEntries) ->
            val categories = monthEntries.groupBy { it.category }.toSortedMap().mapValues { (_, categoryEntries) ->
                val rials = categoryEntries.fold(BigInteger.ZERO) { sum, entry ->
                    entry.amountRials?.let { sum + BigInteger.valueOf(it) } ?: sum
                }
                ReportTotals(categoryEntries.size, rials, categoryEntries.count { it.amountRials == null })
            }
            val totals = ReportTotals(
                entryCount = categories.values.sumOf { it.entryCount },
                totalRials = categories.values.fold(BigInteger.ZERO) { sum, category -> sum + category.totalRials },
                unpricedEntryCount = categories.values.sumOf { it.unpricedEntryCount },
            )
            MonthlyReport(month, totals, categories)
        }
        .sortedWith(compareByDescending<MonthlyReport> { it.month.year }.thenByDescending { it.month.month })
    return VehicleMonthlyReport(
        months = months,
        unassignedEntries = unassigned.sortedWith(
            compareByDescending<UnassignedReportEntry> { it.entry.occurredOn }
                .thenByDescending { it.entry.createdAtEpochMillis }
                .thenBy { it.entry.id },
        ),
    )
}
