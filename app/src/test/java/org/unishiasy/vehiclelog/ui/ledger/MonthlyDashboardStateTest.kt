package org.unishiasy.vehiclelog.ui.ledger

import java.math.BigInteger
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.unishiasy.vehiclelog.data.MonthlyReport
import org.unishiasy.vehiclelog.data.ReportTotals
import org.unishiasy.vehiclelog.data.VehicleMonthlyReport
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.JalaliMonth

class MonthlyDashboardStateTest {
    private val current = JalaliMonth(1405, 1)
    private val previous = JalaliMonth(1404, 12)
    private val older = JalaliMonth(1404, 11)

    @Test
    fun defaultUsesTehranBoundaryAndKeepsEmptyCurrentMonthSelectable() {
        val newYork = ZoneId.of("America/New_York")
        val before = Clock.fixed(Instant.parse("2026-03-20T20:29:59Z"), newYork)
        val after = Clock.fixed(Instant.parse("2026-03-20T20:30:00Z"), newYork)
        assertEquals(previous, MonthlyDashboardState.open(before).selectedMonth)

        val state = MonthlyDashboardState.open(after).copy(report = report(previous, older))
        assertEquals(current, state.selectedMonth)
        assertNull(state.selectedReport)
        assertEquals(listOf(current, previous, older), state.availableMonths)
        assertEquals("فروردین ۱۴۰۵", JalaliDates.formatMonth(current))
    }

    @Test
    fun selectedMonthSurvivesRefreshEvenIfItsLastEntryIsRemoved() {
        val selected = MonthlyDashboardState(current, report = report(previous, older)).select(previous)
        assertEquals(previous, selected.selectedReport?.month)
        val refreshed = selected.copy(report = report(older))
        assertEquals(previous, refreshed.selectedMonth)
        assertNull(refreshed.selectedReport)
        assertEquals(listOf(current, previous, older), refreshed.availableMonths)
        assertEquals(refreshed, refreshed.select(JalaliMonth(1400, 1)))
        assertEquals(listOf(current, older), refreshed.select(current).availableMonths)
    }

    @Test
    fun unsupportedCurrentDateDoesNotSubstituteAnOlderReportMonth() {
        val clock = Clock.fixed(Instant.parse("2120-03-21T12:00:00Z"), ZoneId.of("UTC"))
        val state = MonthlyDashboardState.open(clock).copy(report = report(previous))
        assertNull(state.selectedMonth)
        assertEquals(listOf(previous), state.availableMonths)
        assertEquals(previous, state.select(previous).selectedReport?.month)
    }

    @Test
    fun presentsKnownCategoriesInLedgerOrderAndKeepsAnyUnknownSubtotal() {
        assertEquals(
            listOf("fuel", "oil_change", "repair", "insurance", "wash", "parts", "other", "unknown"),
            orderedReportCategories(setOf("other", "parts", "wash", "unknown", "insurance", "repair", "oil_change", "fuel")),
        )
    }

    private fun report(vararg months: JalaliMonth) = VehicleMonthlyReport(
        months.map { MonthlyReport(it, ReportTotals(1, BigInteger.ONE), emptyMap()) },
        emptyList(),
    )
}
