package org.unishiasy.vehiclelog.domain

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

internal data class JalaliMonth(val year: Int, val month: Int)

internal object JalaliDates {
    const val firstSupportedYear = 1300
    const val lastSupportedYear = 1498
    private const val daysInFirstSixMonths = 186

    private val tehran = ZoneId.of("Asia/Tehran")
    private val inputPattern = Regex("""([0-9]{4})([/\-])([0-9]{1,2})\2([0-9]{1,2})""")
    private val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    // University of Tehran Calendar Center, Common and Leap Solar Hijri Years 1206–1498.
    // The first Farvardin of 1300 is 1921-03-21. Each marked leap year contributes one extra day.
    // https://web.archive.org/web/20230712052459if_/https://calendar.ut.ac.ir/Fa/News/Data/Doc/KabiseShamsi1206-1498-new.pdf
    private val leapYears = setOf(
        1300, 1304, 1309, 1313, 1317, 1321, 1325, 1329, 1333, 1337,
        1342, 1346, 1350, 1354, 1358, 1362, 1366, 1370, 1375, 1379,
        1383, 1387, 1391, 1395, 1399, 1403, 1408, 1412, 1416, 1420,
        1424, 1428, 1432, 1436, 1441, 1445, 1449, 1453, 1457, 1461,
        1465, 1469, 1474, 1478, 1482, 1486, 1490, 1494, 1498,
    )
    private val yearStarts = LongArray(lastSupportedYear - firstSupportedYear + 2).apply {
        this[0] = LocalDate.of(1921, 3, 21).toEpochDay()
        for (index in 1 until size) {
            val previousYear = firstSupportedYear + index - 1
            this[index] = this[index - 1] + if (previousYear in leapYears) 366 else 365
        }
    }

    fun parseJalali(input: String): String? {
        val match = inputPattern.matchEntire(input.trim().normalizeInputDigits()) ?: return null
        val (yearText, _, monthText, dayText) = match.destructured
        val year = yearText.toInt()
        val month = monthText.toInt()
        val day = dayText.toInt()
        if (year !in firstSupportedYear..lastSupportedYear || month !in 1..12) return null
        val monthLength = when {
            month <= 6 -> 31
            month == 12 && year !in leapYears -> 29
            else -> 30
        }
        if (day !in 1..monthLength) return null

        val epochDay = yearStarts[year - firstSupportedYear] + daysBeforeMonth(month) + day - 1
        return LocalDate.ofEpochDay(epochDay).toString()
    }

    fun formatGregorian(occurredOn: String): String? {
        val date = parseGregorian(occurredOn) ?: return null
        val jalali = jalaliParts(date) ?: return "${occurredOn.withPersianDigits()} میلادی"
        return "${jalali.day} ${monthNames[jalali.month - 1]} ${jalali.year}".withPersianDigits()
    }

    fun formatJalaliInput(occurredOn: String): String? {
        val date = parseGregorian(occurredOn) ?: return null
        val jalali = jalaliParts(date) ?: return null
        val month = jalali.month.toString().padStart(2, '0')
        val day = jalali.day.toString().padStart(2, '0')
        return "${jalali.year}/$month/$day".withPersianDigits()
    }

    fun monthOf(date: LocalDate): JalaliMonth? =
        jalaliParts(date)?.let { JalaliMonth(it.year, it.month) }

    fun formatMonth(month: JalaliMonth): String =
        "${monthNames[month.month - 1]} ${month.year}".withPersianDigits()

    fun parseGregorian(occurredOn: String): LocalDate? {
        val date = try {
            LocalDate.parse(occurredOn)
        } catch (_: DateTimeParseException) {
            return null
        }
        return date.takeIf { it.year in 1..9999 && it.toString() == occurredOn }
    }

    private fun jalaliParts(date: LocalDate): JalaliParts? {
        val epochDay = date.toEpochDay()
        if (epochDay < yearStarts.first() || epochDay >= yearStarts.last()) return null

        val search = yearStarts.binarySearch(epochDay)
        val yearIndex = if (search >= 0) search else -search - 2
        val dayIndex = (epochDay - yearStarts[yearIndex]).toInt()
        val month = if (dayIndex < daysInFirstSixMonths) {
            dayIndex / 31 + 1
        } else {
            (dayIndex - daysInFirstSixMonths) / 30 + 7
        }
        val day = dayIndex - daysBeforeMonth(month) + 1
        return JalaliParts(firstSupportedYear + yearIndex, month, day)
    }

    private data class JalaliParts(val year: Int, val month: Int, val day: Int)

    // Call once at draft creation and retain the returned civil date with the draft.
    fun captureDefaultDate(clock: Clock): LocalDate = clock.instant().atZone(tehran).toLocalDate()

    private fun daysBeforeMonth(month: Int): Int =
        if (month <= 6) (month - 1) * 31 else daysInFirstSixMonths + (month - 7) * 30
}
