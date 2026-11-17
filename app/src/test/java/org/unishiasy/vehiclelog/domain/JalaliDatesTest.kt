package org.unishiasy.vehiclelog.domain

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JalaliDatesTest {
    @Test
    fun convertsRequiredNowruzAndEsfandFixturesInBothDirections() {
        val fixtures = listOf(
            Triple("1403/01/01", "2024-03-20", "۱ فروردین ۱۴۰۳"),
            Triple("1403/12/30", "2025-03-20", "۳۰ اسفند ۱۴۰۳"),
            Triple("1404/01/01", "2025-03-21", "۱ فروردین ۱۴۰۴"),
            Triple("1404/12/29", "2026-03-20", "۲۹ اسفند ۱۴۰۴"),
            Triple("1405/01/01", "2026-03-21", "۱ فروردین ۱۴۰۵"),
        )
        for ((jalali, gregorian, label) in fixtures) {
            assertEquals(gregorian, JalaliDates.parseJalali(jalali))
            assertEquals(label, JalaliDates.formatGregorian(gregorian))
        }
        assertNull(JalaliDates.parseJalali("1404/12/30"))
    }

    @Test
    fun acceptsMixedDigitsAndRejectsMalformedOrImpossibleDates() {
        for (input in listOf("۱۴۰۵/۰۶/۲۹", "١٤٠٥-٠٦-٢٩", "  ۱۴0٥/6/۲٩  ")) {
            assertEquals("2026-09-20", JalaliDates.parseJalali(input))
        }
        val invalid = listOf(
            "1405/00/01", "1405/13/01", "1405/01/00", "1405/01/32", "1405/07/31",
            "1405/01-01", "1405.01.01", "1405 /01/01", "1405/001/01", "+1405/01/01",
        )
        for (input in invalid) assertNull(input, JalaliDates.parseJalali(input))
        for (input in listOf("2026-02-29", "2026-3-21", "0000-01-01", "۲۰۲۶-۰۳-۲۱")) {
            assertNull(input, JalaliDates.formatGregorian(input))
        }
    }

    @Test
    fun labelsGregorianDatesOutsideCoverageAndRejectsUnsupportedJalaliInput() {
        assertEquals("۱۹۲۱-۰۳-۲۰ میلادی", JalaliDates.formatGregorian("1921-03-20"))
        assertEquals("۲۱۲۰-۰۳-۲۱ میلادی", JalaliDates.formatGregorian("2120-03-21"))
        assertEquals("۳۰ اسفند ۱۴۹۸", JalaliDates.formatGregorian("2120-03-20"))
        for (year in listOf(1299, 1499, 1500)) assertNull(JalaliDates.parseJalali("$year/01/01"))
    }

    @Test
    fun checksEverySupportedDayAgainstPublishedGregorianYearStarts() {
        val starts = marchOffsets.mapIndexed { index, offset ->
            LocalDate.of(1921 + index, 3, 20 + offset.digitToInt())
        } + LocalDate.of(2120, 3, 21) // The table marks 1498 as leap.
        assertEquals(199, starts.size - 1)
        var checkedDays = 0
        for ((index, start) in starts.dropLast(1).withIndex()) {
            val year = 1300 + index
            val yearLength = (starts[index + 1].toEpochDay() - start.toEpochDay()).toInt()
            var date = start
            for ((monthIndex, name) in monthNames.withIndex()) {
                val month = monthIndex + 1
                val length = when (month) {
                    in 1..6 -> 31
                    12 -> yearLength - 336
                    else -> 30
                }
                for (day in 1..length) {
                    val input = "$year/$month/$day"
                    assertEquals(input, date.toString(), JalaliDates.parseJalali(input))
                    val label = "$day $name $year".map { "۰۱۲۳۴۵۶۷۸۹".getOrNull(it - '0') ?: it }.joinToString("")
                    assertEquals(input, label, JalaliDates.formatGregorian(date.toString()))
                    date = date.plusDays(1)
                    checkedDays++
                }
                assertNull(JalaliDates.parseJalali("$year/$month/${length + 1}"))
            }
            assertEquals(starts[index + 1], date)
        }
        assertEquals(72684, checkedDays)
    }

    @Test
    fun capturesTehranDateOnceAcrossNowruzMidnightAndClockZoneChange() {
        val clock = MutableClock(Instant.parse("2026-03-20T20:29:59Z"), ZoneId.of("America/New_York"))
        val captured = JalaliDates.captureDefaultDate(clock)
        clock.now = Instant.parse("2026-03-20T20:30:00Z")
        clock.zoneId = ZoneId.of("Asia/Tokyo")
        assertEquals(LocalDate.of(2026, 3, 20), captured)
        assertEquals(LocalDate.of(2026, 3, 21), JalaliDates.captureDefaultDate(clock))
        assertEquals(2, clock.instantReads)
    }

    @Test
    fun usesTehranMidnightInJulyWithoutDaylightSaving() {
        val zone = ZoneId.of("America/New_York")
        assertEquals(
            LocalDate.of(2026, 7, 1),
            JalaliDates.captureDefaultDate(Clock.fixed(Instant.parse("2026-07-01T20:29:59Z"), zone)),
        )
        assertEquals(
            LocalDate.of(2026, 7, 2),
            JalaliDates.captureDefaultDate(Clock.fixed(Instant.parse("2026-07-01T20:30:00Z"), zone)),
        )
    }

    @Test
    fun exposesLeapAwareMonthLengthSaturdayFirstWeekdaysAndMonthAddition() {
        assertEquals(30, JalaliDates.monthLength(1403, 12))
        assertEquals(29, JalaliDates.monthLength(1404, 12))
        assertEquals(31, JalaliDates.monthLength(1405, 1))
        assertEquals(0, JalaliDates.monthLength(1405, 13))

        // 1405/01/01 == 2026-03-21 == Saturday (0 = شنبه)
        assertEquals(0, JalaliDates.dayOfWeekOfFirst(1405, 1))
        // 1403/12/30 (2025-03-20) + 12 months clamps to 1404/12/29 (2026-03-20)
        assertEquals("2026-03-20", JalaliDates.addMonths("2025-03-20", 12))
        // 1405/01/31 (2026-04-20) + 6 months clamps to 1405/07/30 (2026-10-22)
        assertEquals("2026-10-22", JalaliDates.addMonths("2026-04-20", 6))
        // Out of range returns null
        assertNull(JalaliDates.addMonths("2120-03-20", 1))
    }

    private class MutableClock(var now: Instant, var zoneId: ZoneId) : Clock() {
        var instantReads = 0
            private set

        override fun getZone(): ZoneId = zoneId
        override fun withZone(zone: ZoneId): Clock = fixed(now, zone)
        override fun instant(): Instant {
            instantReads++
            return now
        }
    }

    private companion object {
        // Gregorian March day minus 20 for 1300–1498, transcribed from the Calendar Center
        // table's Farvardin column, independently of the production leap-year list.
        val marchOffsets = """
            1221122111
            2111211121
            1121112111
            2111211121
            1111111111
            1111111111
            1111111111
            1111101110
            1110111011
            1011101110
            1110111001
            1001100110
            0110011001
            1001100110
            0010001000
            1000100010
            0010001000
            1000000001
            1111111111
            111111111
        """.filterNot(Char::isWhitespace)
        val monthNames = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
        )
    }
}
