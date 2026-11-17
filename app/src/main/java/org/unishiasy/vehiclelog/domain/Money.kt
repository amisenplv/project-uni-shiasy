package org.unishiasy.vehiclelog.domain

import java.math.BigInteger

internal sealed interface TomanParseResult {
    data class Amount(val rials: Long) : TomanParseResult
    data object Blank : TomanParseResult
    data object Invalid : TomanParseResult
}

internal object Money {
    private const val rialsPerToman = 10
    private val amountPattern = Regex(
        """([0-9]++|[0-9]{1,3}(?:,[0-9]{3})++|[0-9]{1,3}(?:٬[0-9]{3})++)(?:[.٫]([0-9]))?""",
    )

    // The caller passes Blank as null to the existing category validation.
    fun parseToman(input: String): TomanParseResult {
        val text = input.trim()
        if (text.isEmpty()) return TomanParseResult.Blank
        val match = amountPattern.matchEntire(text.normalizeInputDigits()) ?: return TomanParseResult.Invalid
        val (whole, fraction) = match.destructured
        val wholeDigits = whole.replace(",", "").replace("٬", "")
        val rials = (wholeDigits + fraction.ifEmpty { "0" }).toLongOrNull()
        return if (rials != null && rials > 0) TomanParseResult.Amount(rials) else TomanParseResult.Invalid
    }

    fun formatTomanInput(amountRials: Long?): String {
        if (amountRials == null) return ""
        require(amountRials > 0)
        return formatTomanTotal(BigInteger.valueOf(amountRials))
    }

    fun formatTomanTotal(amountRials: BigInteger): String {
        val (wholeToman, remainder) = amountRials.abs().divideAndRemainder(BigInteger.valueOf(rialsPerToman.toLong()))
        val whole = wholeToman.toString().reversed().chunked(3).joinToString("٬").reversed()
        val amount = if (remainder == BigInteger.ZERO) whole else "$whole٫$remainder"
        val sign = if (amountRials.signum() < 0) "−" else ""
        return "$sign$amount".withPersianDigits()
    }

    fun formatTomanLabel(amountRials: Long?): String =
        if (amountRials == null) "مبلغ ثبت نشده" else "${formatTomanInput(amountRials)} تومان"
}
