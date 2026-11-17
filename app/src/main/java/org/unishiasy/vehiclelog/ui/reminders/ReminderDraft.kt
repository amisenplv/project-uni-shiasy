package org.unishiasy.vehiclelog.ui.reminders

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import org.unishiasy.vehiclelog.data.ReminderInput
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.normalizeInputDigits

internal enum class ReminderThreshold { Date, Mileage }

internal data class ReminderDraft(
    val id: UUID = UUID.randomUUID(),
    val title: String = "",
    val threshold: ReminderThreshold = ReminderThreshold.Date,
    val date: String = "",
    val mileage: String = "",
) {
    fun input(): ReminderInput? {
        if (title.isBlank()) return null
        return when (threshold) {
            ReminderThreshold.Date -> JalaliDates.parseJalali(date)?.let { ReminderInput(title, dueOn = it) }
            ReminderThreshold.Mileage -> parseReminderMileage(mileage)?.let { ReminderInput(title, dueKilometers = it) }
        }
    }
}

internal fun parseReminderMileage(input: String): Long? {
    val digits = input.trim().normalizeInputDigits()
    return digits.takeIf { it.isNotEmpty() && it.all { digit -> digit in '0'..'9' } }?.toLongOrNull()
}

internal fun millisUntilTehranMidnight(now: Instant): Long {
    val tehran = ZoneId.of("Asia/Tehran")
    val nextMidnight = now.atZone(tehran).toLocalDate().plusDays(1).atStartOfDay(tehran)
    return Duration.between(now, nextMidnight.toInstant()).toMillis().coerceAtLeast(1)
}
