package org.unishiasy.vehiclelog.ui.ledger

import java.time.LocalDate
import org.unishiasy.vehiclelog.data.local.HistoryEntry
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.Money
import org.unishiasy.vehiclelog.domain.normalizeInputDigits
import org.unishiasy.vehiclelog.domain.withPersianDigits

internal data class HistoryFilterState(
    val query: String = "",
    val categoryKey: String? = null,
) {
    val isActive: Boolean get() = query.isNotBlank() || categoryKey != null
}

internal data class HistoryMonthGroup(
    val monthKey: String,
    val headerLabel: String,
    val entries: List<HistoryEntry>,
)

internal fun normalizeSearchText(raw: String): String {
    val normalizedDigits = raw.trim().normalizeInputDigits()
    val sb = StringBuilder(normalizedDigits.length)
    for (ch in normalizedDigits) {
        when (ch) {
            'ي' -> sb.append('ی')
            'ك' -> sb.append('ک')
            '\u200c', ',', '٬', ' ' -> Unit
            else -> sb.append(ch.lowercaseChar())
        }
    }
    return sb.toString()
}

internal fun filterAndGroupHistory(
    entries: List<HistoryEntry>,
    filter: HistoryFilterState,
    categoryLabels: Map<String, String>,
    fallbackGroupLabel: String,
): List<HistoryMonthGroup> {
    val normalizedQuery = normalizeSearchText(filter.query)
    val filtered = entries.filter { entry ->
        if (filter.categoryKey != null && entry.category != filter.categoryKey) return@filter false
        if (normalizedQuery.isEmpty()) return@filter true

        val label = categoryLabels[entry.category].orEmpty()
        val notes = entry.notes.orEmpty()
        val jalaliLabel = JalaliDates.formatGregorian(entry.occurredOn).orEmpty()
        val jalaliSlash = JalaliDates.formatJalaliInput(entry.occurredOn).orEmpty()
        val amountText = entry.amountRials?.let { Money.formatTomanInput(it) }.orEmpty()
        val kmText = entry.linkedOdometerKm?.toString().orEmpty()
        val litersText = entry.fuelVolumeMl?.let {
            java.math.BigDecimal.valueOf(it, 3).stripTrailingZeros().toPlainString()
        }.orEmpty()

        val haystack = normalizeSearchText("$label $notes $jalaliLabel $jalaliSlash $amountText $kmText $litersText")
        normalizedQuery in haystack
    }

    if (filtered.isEmpty()) return emptyList()

    val groups = LinkedHashMap<String, Pair<String, MutableList<HistoryEntry>>>()
    for (entry in filtered) {
        val parts = runCatching { LocalDate.parse(entry.occurredOn) }.getOrNull()?.let(JalaliDates::jalaliPartsOf)
        val (key, header) = if (parts != null) {
            val k = "${parts.year}-${parts.month}"
            val h = "${JalaliDates.monthName(parts.month)} ${parts.year.toString().withPersianDigits()}"
            k to h
        } else {
            "other" to fallbackGroupLabel
        }
        val bucket = groups.getOrPut(key) { header to mutableListOf() }
        bucket.second.add(entry)
    }

    return groups.map { (key, value) ->
        HistoryMonthGroup(monthKey = key, headerLabel = value.first, entries = value.second)
    }
}
