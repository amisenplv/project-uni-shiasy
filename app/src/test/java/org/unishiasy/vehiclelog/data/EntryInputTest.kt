package org.unishiasy.vehiclelog.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryInputTest {
    @Test
    fun acceptsRealGregorianDatesIncludingLeapDays() {
        assertTrue(isValidOccurrenceDate("2024-02-29"))
        assertTrue(isValidOccurrenceDate("2000-02-29"))
    }

    @Test
    fun rejectsImpossibleDatesAndNoncanonicalText() {
        val invalid = listOf(
            "2023-02-29", "1900-02-29", "2024-04-31", "2024-13-01", "2024-01-00",
            "2024-2-09", "2024/02/09", "2024-02-09T00:00:00Z", "0000-01-01", "۲۰۲۴-۰۲-۲۹",
        )
        invalid.forEach { assertFalse(it, isValidOccurrenceDate(it)) }
    }

    @Test
    fun convertsLitersExactlyIncludingMixedDigits() {
        assertEquals(32000L, parseFuelVolumeMl("32"))
        assertEquals(32450L, parseFuelVolumeMl(" ۳٢٫۴5۰ "))
        assertEquals(1L, parseFuelVolumeMl("0.001"))
        assertEquals(Long.MAX_VALUE, parseFuelVolumeMl("9223372036854775.807"))
    }

    @Test
    fun rejectsNonpositiveMalformedOverprecisionAndOverflowVolumes() {
        val invalid = listOf("", "0", "0.000", "-1", "1.0000", "1.0019", "1e1", "1,5", "9223372036854775.808")
        invalid.forEach { assertNull(it, parseFuelVolumeMl(it)) }
    }
}
