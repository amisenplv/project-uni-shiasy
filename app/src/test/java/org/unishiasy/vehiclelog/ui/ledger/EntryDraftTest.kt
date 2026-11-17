package org.unishiasy.vehiclelog.ui.ledger

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.EntryWithReading
import org.unishiasy.vehiclelog.data.LinkedReadingEdit
import org.unishiasy.vehiclelog.data.local.EntryEntity
import org.unishiasy.vehiclelog.data.local.OdometerReadingEntity
import org.unishiasy.vehiclelog.data.parseFuelVolumeMl

class EntryDraftTest {
    private val instant = Instant.parse("2026-03-20T20:30:00Z")
    private val clock = Clock.fixed(instant, ZoneId.of("America/New_York"))
    private val vehicleId = "vehicle-a"

    @Test
    fun capturesTehranDateAndSeparateTimestampAndConvertsExactValues() {
        val draft = EntryDraft.create(vehicleId, clock)
        assertEquals("۱۴۰۵/۰۱/۰۱", draft.date)
        assertEquals(instant.toEpochMilli(), draft.createdAt)
        val edited = draft.copy(amount = "١٢٬۰۰۰٫۱", volume = "۳٢٫۴۵۰", mileage = "۱٢0۰")
        val validation = edited.validate() as DraftValidation.Valid
        assertEquals(draft.id, edited.id)
        assertEquals(draft.createdAt, edited.createdAt)
        assertEquals("2026-03-21", validation.input.occurredOn)
        assertEquals(120001L, validation.input.amountRials)
        assertEquals(32450L, parseFuelVolumeMl(checkNotNull(validation.input.fuelVolumeLiters)))
        assertEquals(1200L, validation.kilometers)
    }

    @Test
    fun distinguishesBlankServiceAmountFromInvalidAmountAndValidatesAllFuelFields() {
        val draft = EntryDraft.create(vehicleId, clock)
        val invalid = draft.copy(date = "۱۴۰۴/۱۲/۳۰", mileage = "۱۲٫۱").validate() as DraftValidation.Invalid
        assertEquals(
            mapOf(
                EntryField.Amount to R.string.ledger_amount_required,
                EntryField.Date to R.string.ledger_date_invalid,
                EntryField.Volume to R.string.ledger_volume_invalid,
                EntryField.Mileage to R.string.ledger_mileage_invalid,
            ),
            invalid.errors,
        )
        val service = draft.copy(category = EntryCategory.Parts, volume = "invalid")
        val valid = service.validate() as DraftValidation.Valid
        assertNull(valid.input.amountRials)
        assertNull(valid.input.fuelVolumeLiters)
        assertEquals(
            mapOf(EntryField.Amount to R.string.ledger_amount_invalid),
            (service.copy(amount = "۱۲٫۰۰").validate() as DraftValidation.Invalid).errors,
        )
    }

    @Test
    fun editPreservesUnsupportedDateAndReadingUntilExplicitReplacementOrRemoval() {
        val id = UUID.randomUUID()
        val entry = EntryEntity(id.toString(), vehicleId, "fuel", "2120-03-21", 15L, null, Long.MAX_VALUE, Long.MAX_VALUE)
        val reading = OdometerReadingEntity(UUID.randomUUID().toString(), vehicleId, entry.occurredOn, 700L, entry.id)
        val draft = checkNotNull(EntryDraft.edit(EntryWithReading(entry, reading)))
        assertEquals("", draft.date)
        assertEquals(id, draft.id)
        assertEquals(15L, draft.createdAt)
        val kept = draft.copy(mileage = "", notes = "بازبینی").validate() as DraftValidation.Valid
        assertEquals(LinkedReadingEdit.Keep, kept.readingEdit)
        assertEquals(entry.occurredOn, kept.input.occurredOn)
        assertEquals(Long.MAX_VALUE, kept.input.amountRials)
        assertEquals(Long.MAX_VALUE, parseFuelVolumeMl(checkNotNull(kept.input.fuelVolumeLiters)))
        val set = draft.copy(date = "۱۴۰۵/۰۱/۰۱", mileage = "۰").validate() as DraftValidation.Valid
        assertEquals("2026-03-21", set.input.occurredOn)
        assertEquals(LinkedReadingEdit.Set(0), set.readingEdit)
        val removed = draft.copy(omitMileage = true).validate() as DraftValidation.Valid
        assertEquals(LinkedReadingEdit.Remove, removed.readingEdit)
    }
}
