package org.unishiasy.vehiclelog.domain

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JalaliDatesDeviceTest {
    @Test
    fun verifiesOfficialBoundariesAndGregorianFallbackOnAndroid() {
        val fixtures = listOf(
            Triple("۱۳۰۰/۰۱/۰۱", "1921-03-21", "۱ فروردین ۱۳۰۰"),
            Triple("۱۴۰۳/۰۱/۰۱", "2024-03-20", "۱ فروردین ۱۴۰۳"),
            Triple("۱۴۰۳/۱۲/۳۰", "2025-03-20", "۳۰ اسفند ۱۴۰۳"),
            Triple("۱۴۰۴/۰۱/۰۱", "2025-03-21", "۱ فروردین ۱۴۰۴"),
            Triple("۱۴۰۴/۱۲/۲۹", "2026-03-20", "۲۹ اسفند ۱۴۰۴"),
            Triple("۱۴۰۵/۰۱/۰۱", "2026-03-21", "۱ فروردین ۱۴۰۵"),
            Triple("۱۴۶۹/۱۲/۳۰", "2091-03-20", "۳۰ اسفند ۱۴۶۹"),
            Triple("۱۴۹۸/۱۲/۳۰", "2120-03-20", "۳۰ اسفند ۱۴۹۸"),
        )
        for ((jalali, gregorian, label) in fixtures) {
            assertEquals(gregorian, JalaliDates.parseJalali(jalali))
            assertEquals(label, JalaliDates.formatGregorian(gregorian))
        }
        assertNull(JalaliDates.parseJalali("۱۴۰۴/۱۲/۳۰"))
        assertEquals("۲۱۲۰-۰۳-۲۱ میلادی", JalaliDates.formatGregorian("2120-03-21"))
    }

    @Test
    fun verifiesTehranMidnightInMarchAndJulyOnAndroid() {
        val zone = ZoneId.of("America/New_York")
        val fixtures = mapOf(
            "2026-03-20T20:29:59Z" to "2026-03-20",
            "2026-03-20T20:30:00Z" to "2026-03-21",
            "2026-07-01T20:29:59Z" to "2026-07-01",
            "2026-07-01T20:30:00Z" to "2026-07-02",
        )
        for ((instant, date) in fixtures) {
            assertEquals(instant, date, JalaliDates.captureDefaultDate(Clock.fixed(Instant.parse(instant), zone)).toString())
        }
    }
}
