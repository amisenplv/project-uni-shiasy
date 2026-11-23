package org.unishiasy.vehiclelog.data

import java.time.LocalDate
import java.time.format.DateTimeParseException

data class EntryInput(
    val vehicleId: String,
    val category: String,
    val occurredOn: String,
    val amountRials: Long? = null,
    val fuelVolumeLiters: String? = null,
    val notes: String? = null,
    val stationName: String? = null,
    val isFullTank: Boolean = false,
)

private val datePattern = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")
private val litersPattern = Regex("[0-9]+(?:\\.[0-9]{1,3})?")

internal fun isValidOccurrenceDate(value: String): Boolean {
    if (!datePattern.matches(value)) return false
    return try {
        LocalDate.parse(value).year > 0
    } catch (_: DateTimeParseException) {
        false
    }
}

internal fun parseFuelVolumeMl(value: String): Long? {
    val normalized = value.trim().map { character ->
        when (character) {
            in '۰'..'۹', in '٠'..'٩' -> '0' + character.digitToInt()
            '٫' -> '.'
            else -> character
        }
    }.joinToString("")
    if (!litersPattern.matches(normalized)) return null

    return try {
        normalized.toBigDecimal().movePointRight(3).longValueExact().takeIf { it > 0 }
    } catch (_: ArithmeticException) {
        null
    }
}
