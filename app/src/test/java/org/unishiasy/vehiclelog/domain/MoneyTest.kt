package org.unishiasy.vehiclelog.domain

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test
    fun formatsExactDashboardTotalsIncludingZeroAndValuesBeyondLong() {
        assertEquals("۰", Money.formatTomanTotal(BigInteger.ZERO))
        assertEquals("۰٫۱", Money.formatTomanTotal(BigInteger.ONE))
        assertEquals("۱٬۸۴۴٬۶۷۴٬۴۰۷٬۳۷۰٬۹۵۵٬۱۶۱٫۴", Money.formatTomanTotal(BigInteger("18446744073709551614")))
        assertEquals("۹۲۲٬۳۳۷٬۲۰۳٬۶۸۵٬۴۷۷٬۵۸۱", Money.formatTomanTotal(BigInteger("9223372036854775810")))
    }

    @Test
    fun parsesExactRialsFromAllSupportedDigitsAndSeparators() {
        for (input in listOf("12000.1", "۱۲٬۰۰۰٫۱", "١٢,٠٠٠.١", "  ۱٢٬0۰٠٫١  ")) {
            assertEquals(input, TomanParseResult.Amount(120001), Money.parseToman(input))
        }
        assertEquals(TomanParseResult.Amount(120000), Money.parseToman("12,000.0"))
        assertEquals(TomanParseResult.Amount(1), Money.parseToman("۰٫۱"))
    }

    @Test
    fun rejectsMalformedAmountsWithoutTreatingThemAsBlank() {
        val invalid = listOf(
            "12,00", "1٬23٬456", "1,234٬567", "12 000", ",100", "12,", "1.2,000",
            "+1", "-1", "−۱", "۱ تومان", "12e3", "１２", "1.01", "1٫00", "1.", ".1", "0", "۰٫۰",
            "1" + ",000".repeat(5000),
        )
        for (input in invalid) {
            assertEquals(input.take(40), TomanParseResult.Invalid, Money.parseToman(input))
        }
        assertEquals(TomanParseResult.Blank, Money.parseToman(" \t "))
    }

    @Test
    fun acceptsLongMaximumAndRejectsFirstOverflowAndLargerAmounts() {
        assertEquals(TomanParseResult.Amount(Long.MAX_VALUE), Money.parseToman("922337203685477580.7"))
        for (input in listOf("922337203685477580.8", "922337203685477581", "92233720368547758070")) {
            assertEquals(input, TomanParseResult.Invalid, Money.parseToman(input))
        }
    }

    @Test
    fun formatsPersianTomanLabelsAndNullableAmounts() {
        assertEquals("۱۲٬۰۰۰ تومان", Money.formatTomanLabel(120000))
        assertEquals("۱۲٬۰۰۰٫۱ تومان", Money.formatTomanLabel(120001))
        assertEquals("۹۲۲٬۳۳۷٬۲۰۳٬۶۸۵٬۴۷۷٬۵۸۰٫۷ تومان", Money.formatTomanLabel(Long.MAX_VALUE))
        assertEquals("مبلغ ثبت نشده", Money.formatTomanLabel(null))
        assertEquals("", Money.formatTomanInput(null))
    }

    @Test
    fun formattedInputsRoundTripToTheSameRials() {
        for (rials in listOf(1L, 9L, 10L, 11L, 120001L, 9_007_199_254_740_993L, Long.MAX_VALUE)) {
            assertEquals(TomanParseResult.Amount(rials), Money.parseToman(Money.formatTomanInput(rials)))
        }
        assertEquals(TomanParseResult.Blank, Money.parseToman(Money.formatTomanInput(null)))
    }
}
