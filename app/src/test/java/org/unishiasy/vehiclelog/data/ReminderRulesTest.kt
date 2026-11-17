package org.unishiasy.vehiclelog.data

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.unishiasy.vehiclelog.data.local.ReminderEntity
import org.unishiasy.vehiclelog.domain.JalaliDates

class ReminderRulesTest {
    private val day = LocalDate.parse("2026-03-21")
    private val date = ReminderEntity("id", "vehicle", "Service", day.toString(), null, "active")
    private val mileage = date.copy(dueOn = null, dueKilometers = 100)

    @Test
    fun dateThresholdIsInclusive() {
        assertFalse(date.isDue(day.minusDays(1), null))
        assertTrue(date.isDue(day, null))
        assertTrue(date.isDue(day.plusDays(1), null))
    }

    @Test
    fun dateThresholdUsesTehranMidnight() {
        fun tehranDate(instant: String) =
            JalaliDates.captureDefaultDate(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC))

        assertFalse(date.isDue(tehranDate("2026-03-20T20:29:59Z"), null))
        assertTrue(date.isDue(tehranDate("2026-03-20T20:30:00Z"), null))
    }

    @Test
    fun mileageThresholdIsInclusiveAndRequiresKnownMileage() {
        assertFalse(mileage.isDue(day, 99))
        assertTrue(mileage.isDue(day, 100))
        assertTrue(mileage.isDue(day, 101))
        assertFalse(mileage.isDue(day, null))
        assertTrue(mileage.copy(dueKilometers = 0).isDue(day, 0))
    }

    @Test
    fun terminalStatesAreNeverDue() {
        for (state in listOf(ReminderState.Completed, ReminderState.Canceled)) {
            assertFalse(date.copy(state = state.key).isDue(day, 100))
            assertFalse(mileage.copy(state = state.key).isDue(day, 100))
        }
    }

    @Test
    fun validatesReminderInputs() {
        for (input in listOf(
            ReminderInput(" "),
            ReminderInput(" ", dueKilometers = 0),
            ReminderInput("Service"),
            ReminderInput("Service", "2026-03-21", 0),
            ReminderInput("Service", "2026-02-29"),
            ReminderInput("Service", "2026-3-21"),
            ReminderInput("Service", "0000-01-01"),
            ReminderInput("Service", dueKilometers = -1),
        )) {
            assertFalse(input.toString(), input.isValid())
        }
        assertTrue(ReminderInput("Service", "2028-02-29").isValid())
        assertTrue(ReminderInput("Service", dueKilometers = Long.MAX_VALUE).isValid())
    }
}
