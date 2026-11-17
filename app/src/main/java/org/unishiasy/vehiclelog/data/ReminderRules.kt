package org.unishiasy.vehiclelog.data

import java.time.LocalDate
import org.unishiasy.vehiclelog.data.local.ReminderEntity

data class ReminderInput(
    val title: String,
    val dueOn: String? = null,
    val dueKilometers: Long? = null,
)

enum class ReminderState(val key: String) {
    Active("active"),
    Completed("completed"),
    Canceled("canceled"),
}

internal fun ReminderInput.isValid(): Boolean =
    title.isNotBlank() && when {
        dueOn != null -> dueKilometers == null && isValidOccurrenceDate(dueOn)
        else -> dueKilometers != null && dueKilometers >= 0
    }

fun ReminderEntity.isDue(tehranDate: LocalDate, currentMileage: Long?): Boolean {
    if (state != ReminderState.Active.key) return false
    return if (dueOn != null) {
        tehranDate >= LocalDate.parse(dueOn)
    } else {
        dueKilometers != null && currentMileage != null && currentMileage >= dueKilometers
    }
}
