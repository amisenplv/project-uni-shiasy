package org.unishiasy.vehiclelog.ui.ledger

import java.time.Clock
import org.unishiasy.vehiclelog.data.VehicleMonthlyReport
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.JalaliMonth

internal data class MonthlyDashboardState(
    val currentMonth: JalaliMonth?,
    val selectedMonth: JalaliMonth? = currentMonth,
    val report: VehicleMonthlyReport? = null,
    val failed: Boolean = false,
) {
    val availableMonths: List<JalaliMonth>
        get() = (listOfNotNull(currentMonth, selectedMonth) + report?.months.orEmpty().map { it.month })
            .distinct()
            .sortedWith(compareByDescending<JalaliMonth> { it.year }.thenByDescending { it.month })

    val selectedReport get() = report?.months?.firstOrNull { it.month == selectedMonth }

    fun select(month: JalaliMonth): MonthlyDashboardState =
        if (month in availableMonths) copy(selectedMonth = month) else this

    companion object {
        fun open(clock: Clock): MonthlyDashboardState =
            MonthlyDashboardState(JalaliDates.monthOf(JalaliDates.captureDefaultDate(clock)))
    }
}

internal fun orderedReportCategories(keys: Set<String>): List<String> {
    val knownOrder = EntryCategory.entries.map { it.key }
    return knownOrder.filter { it in keys } + (keys - knownOrder.toSet()).sorted()
}
