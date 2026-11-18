package org.unishiasy.vehiclelog.ui.reminders

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import org.unishiasy.vehiclelog.data.ReminderInput
import org.unishiasy.vehiclelog.data.local.ReminderEntity
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.normalizeInputDigits
import org.unishiasy.vehiclelog.domain.withPersianDigits

internal enum class ReminderThreshold { Date, Mileage }

internal data class ReminderDraft(
    val id: UUID = UUID.randomUUID(),
    val editing: Boolean = false,
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

    companion object {
        fun edit(entity: ReminderEntity): ReminderDraft {
            val uuid = runCatching { UUID.fromString(entity.id) }.getOrElse { UUID.randomUUID() }
            return if (entity.dueOn != null) {
                ReminderDraft(
                    id = uuid,
                    editing = true,
                    title = entity.title,
                    threshold = ReminderThreshold.Date,
                    date = JalaliDates.formatJalaliInput(entity.dueOn).orEmpty(),
                )
            } else {
                ReminderDraft(
                    id = uuid,
                    editing = true,
                    title = entity.title,
                    threshold = ReminderThreshold.Mileage,
                    mileage = entity.dueKilometers?.toString()?.withPersianDigits().orEmpty(),
                )
            }
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
