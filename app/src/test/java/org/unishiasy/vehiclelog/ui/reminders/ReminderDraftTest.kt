package org.unishiasy.vehiclelog.ui.reminders

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderDraftTest {
    @Test
    fun dateDraftUsesOnlyTheSelectedThreshold() {
        val draft = ReminderDraft(title = "روغن", date = "۱۴۰۵/٠١/01", mileage = "۱۲۳")
        val date = requireNotNull(draft.input())
        assertEquals("2026-03-21", date.dueOn)
        assertNull(date.dueKilometers)
        val mileage = requireNotNull(draft.copy(threshold = ReminderThreshold.Mileage).input())
        assertEquals(123L, mileage.dueKilometers)
        assertNull(mileage.dueOn)
    }

    @Test
    fun rejectsMissingOrInvalidFieldsAndAcceptsWholeMileage() {
        val draft = ReminderDraft(title = "سرویس")
        assertNull(draft.input())
        assertNull(draft.copy(date = "۱۴۰۴/۱۲/۳۰").input())
        assertNull(draft.copy(title = " ", date = "۱۴۰۵/۰۱/۰۱").input())
        assertNull(draft.copy(threshold = ReminderThreshold.Mileage).input())
        for (value in listOf("", "-۱", "+1", "۲٫۵", "۱٬۰۰۰", "9223372036854775808")) {
            assertNull(value, parseReminderMileage(value))
        }
        assertEquals(0L, parseReminderMileage("٠"))
        assertEquals(Long.MAX_VALUE, parseReminderMileage("9223372036854775807"))
    }

    @Test
    fun schedulesTheNextTehranMidnight() {
        assertEquals(1_000L, millisUntilTehranMidnight(Instant.parse("2026-03-20T20:29:59Z")))
        assertEquals(86_400_000L, millisUntilTehranMidnight(Instant.parse("2026-03-20T20:30:00Z")))
        assertEquals(1_000L, millisUntilTehranMidnight(Instant.parse("2026-07-10T20:29:59Z")))
    }
}
