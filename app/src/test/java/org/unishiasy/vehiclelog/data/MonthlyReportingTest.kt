package org.unishiasy.vehiclelog.data

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.unishiasy.vehiclelog.data.local.EntryEntity
import org.unishiasy.vehiclelog.domain.JalaliMonth

class MonthlyReportingTest {
    @Test
    fun splitsAdjacentJalaliMonthsWithinTheSameGregorianMonth() {
        val report = calculateMonthlyReport("a", listOf(
            entry("1", "2026-04-20", amount = 11),
            entry("2", "2026-04-21", amount = 22),
        ))

        assertEquals(listOf(JalaliMonth(1405, 2), JalaliMonth(1405, 1)), report.months.map { it.month })
        assertEquals(listOf(22L, 11L), report.months.map { it.totals.totalRialsAsLongOrNull() })
    }

    @Test
    fun putsLeapEsfandDayInTheOldYearAndNowruzInTheNewYear() {
        val report = calculateMonthlyReport("a", listOf(
            entry("1", "2025-03-19", amount = 10),
            entry("2", "2025-03-20", amount = 20),
            entry("3", "2025-03-21", amount = 30),
        ))

        assertEquals(listOf(JalaliMonth(1404, 1), JalaliMonth(1403, 12)), report.months.map { it.month })
        assertEquals(listOf(1, 2), report.months.map { it.totals.entryCount })
        assertEquals(listOf(30L, 30L), report.months.map { it.totals.totalRialsAsLongOrNull() })
    }

    @Test
    fun filtersVehicleAndReconcilesCountsAndTotalsAcrossCategories() {
        val entries = listOf(
            entry("1", "2026-04-20", "repair", 7),
            entry("2", "2026-04-20", "parts", null),
            entry("3", "2026-04-20", "repair", null),
            entry("4", "2026-04-20", "fuel", 100),
            entry("5", "2026-04-20", "fuel", 500, vehicle = "b"),
        )
        val report = calculateMonthlyReport("a", entries)
        val month = report.months.single()

        assertEquals(listOf("fuel", "parts", "repair"), month.categories.keys.toList())
        assertEquals(ReportTotals(1, BigInteger.valueOf(100)), month.categories["fuel"])
        assertEquals(ReportTotals(1, BigInteger.ZERO, unpricedEntryCount = 1), month.categories["parts"])
        assertEquals(ReportTotals(2, BigInteger.valueOf(7), unpricedEntryCount = 1), month.categories["repair"])
        assertEquals(ReportTotals(4, BigInteger.valueOf(107), unpricedEntryCount = 2), month.totals)
        assertEquals(report, calculateMonthlyReport("a", entries.reversed()))
        assertEquals(500L, calculateMonthlyReport("b", entries).months.single().totals.totalRialsAsLongOrNull())
    }

    @Test
    fun reportsAnUnpricedOnlyMonthWithZeroRialsAndNonzeroCount() {
        val month = calculateMonthlyReport("a", listOf(
            entry("1", "2026-04-20", "repair", null),
            entry("2", "2026-04-20", "parts", null),
        )).months.single()

        assertEquals(ReportTotals(2, BigInteger.ZERO, unpricedEntryCount = 2), month.totals)
        assertEquals(0L, month.totals.totalRialsAsLongOrNull())
    }

    @Test
    fun keepsExactCategoryAndMonthlyTotalsBeyondLongMaximum() {
        val month = calculateMonthlyReport("a", listOf(
            entry("1", "2026-04-20", "repair", Long.MAX_VALUE),
            entry("2", "2026-04-20", "repair", 1),
            entry("3", "2026-04-20", "parts", Long.MAX_VALUE),
        )).months.single()
        val repair = checkNotNull(month.categories["repair"])
        val parts = checkNotNull(month.categories["parts"])

        assertEquals(BigInteger("9223372036854775808"), repair.totalRials)
        assertEquals(Long.MAX_VALUE, parts.totalRialsAsLongOrNull())
        assertEquals(BigInteger("18446744073709551615"), month.totals.totalRials)
        assertEquals(repair.totalRials + parts.totalRials, month.totals.totalRials)
        assertNull(repair.totalRialsAsLongOrNull())
        assertNull(month.totals.totalRialsAsLongOrNull())
    }

    @Test
    fun emptyOrOtherVehicleOnlyHistoryProducesNoBuckets() {
        val empty = VehicleMonthlyReport(emptyList(), emptyList())
        assertEquals(empty, calculateMonthlyReport("a", emptyList()))
        assertEquals(empty, calculateMonthlyReport("a", listOf(entry("1", "2120-03-21", vehicle = "b"))))
    }

    @Test
    fun retainsOutOfRangeDatesAndSeparatesMalformedDatesWithoutGuessing() {
        val before = entry("1", "1921-03-20", amount = 3)
        val firstSupported = entry("2", "1921-03-21")
        val lastSupported = entry("3", "2120-03-20")
        val after = entry("4", "2120-03-21", amount = 9)
        val malformed = entry("5", "2026-02-30")
        val entries = listOf(before, firstSupported, lastSupported, after, malformed)
        val report = calculateMonthlyReport("a", entries)

        assertEquals(listOf(JalaliMonth(1498, 12), JalaliMonth(1300, 1)), report.months.map { it.month })
        assertEquals(listOf(
            UnassignedReportEntry(after, ReportDateIssue.OutsideSupportedJalaliRange),
            UnassignedReportEntry(malformed, ReportDateIssue.InvalidGregorianDate),
            UnassignedReportEntry(before, ReportDateIssue.OutsideSupportedJalaliRange),
        ), report.unassignedEntries)
        assertEquals(report, calculateMonthlyReport("a", entries.reversed()))
        val outsideOnly = calculateMonthlyReport("a", listOf(after))
        assertEquals(emptyList<MonthlyReport>(), outsideOnly.months)
        assertEquals(after, outsideOnly.unassignedEntries.single().entry)
    }

    private fun entry(
        id: String,
        date: String,
        category: String = "repair",
        amount: Long? = 1,
        vehicle: String = "a",
    ) = EntryEntity(id, vehicle, category, date, 0, null, amount, null)
}
