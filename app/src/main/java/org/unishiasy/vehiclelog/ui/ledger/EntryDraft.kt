package org.unishiasy.vehiclelog.ui.ledger

import androidx.annotation.StringRes
import java.time.Clock
import java.time.ZoneOffset
import java.util.UUID
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.EntryInput
import org.unishiasy.vehiclelog.data.EntryWithReading
import org.unishiasy.vehiclelog.data.LinkedReadingEdit
import org.unishiasy.vehiclelog.data.allowsUnpricedEntry
import org.unishiasy.vehiclelog.data.parseFuelVolumeMl
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.Money
import org.unishiasy.vehiclelog.domain.TomanParseResult
import org.unishiasy.vehiclelog.domain.normalizeInputDigits
import org.unishiasy.vehiclelog.domain.withPersianDigits

internal enum class EntryCategory(val key: String, @field:StringRes val label: Int) {
    Fuel("fuel", R.string.ledger_fuel),
    OilChange("oil_change", R.string.ledger_oil_change),
    Repair("repair", R.string.ledger_repair),
    Insurance("insurance", R.string.ledger_insurance),
    Wash("wash", R.string.ledger_wash),
    Parts("parts", R.string.ledger_parts),
    Other("other", R.string.ledger_other),
}

internal enum class EntryField { Amount, Date, Volume, Mileage, Notes }

internal data class EntryDraft(
    val vehicleId: String,
    val id: UUID,
    val createdAt: Long,
    val editing: Boolean = false,
    val category: EntryCategory = EntryCategory.Fuel,
    val amount: String = "",
    val date: String = "",
    val originalGregorianDate: String? = null,
    val volume: String = "",
    val mileage: String = "",
    val originalMileage: Long? = null,
    val omitMileage: Boolean = false,
    val notes: String = "",
) {
    companion object {
        fun create(vehicleId: String, clock: Clock): EntryDraft {
            val instant = clock.instant()
            val date = JalaliDates.captureDefaultDate(Clock.fixed(instant, ZoneOffset.UTC))
            return EntryDraft(
                vehicleId, UUID.randomUUID(), instant.toEpochMilli(),
                date = JalaliDates.formatJalaliInput(date.toString()).orEmpty(),
            )
        }

        fun edit(saved: EntryWithReading): EntryDraft? {
            val entry = saved.entry
            val id = runCatching { UUID.fromString(entry.id) }.getOrNull() ?: return null
            val category = EntryCategory.entries.firstOrNull { it.key == entry.category } ?: return null
            val jalali = JalaliDates.formatJalaliInput(entry.occurredOn)
            return EntryDraft(
                vehicleId = entry.vehicleId,
                id = id,
                createdAt = entry.createdAtEpochMillis,
                editing = true,
                category = category,
                amount = Money.formatTomanInput(entry.amountRials),
                date = jalali.orEmpty(),
                originalGregorianDate = entry.occurredOn.takeIf { jalali == null },
                volume = entry.fuelVolumeMl?.let {
                    java.math.BigDecimal.valueOf(it, 3).stripTrailingZeros().toPlainString()
                        .replace('.', '٫').withPersianDigits()
                }.orEmpty(),
                mileage = saved.reading?.kilometers?.toString()?.withPersianDigits().orEmpty(),
                originalMileage = saved.reading?.kilometers,
                notes = entry.notes.orEmpty(),
            )
        }
    }

    fun validate(): DraftValidation {
        val errors = mutableMapOf<EntryField, Int>()
        val rials = when (val parsed = Money.parseToman(amount)) {
            is TomanParseResult.Amount -> parsed.rials
            TomanParseResult.Blank -> {
                if (!allowsUnpricedEntry(category.key)) errors[EntryField.Amount] = R.string.ledger_amount_required
                null
            }
            TomanParseResult.Invalid -> {
                errors[EntryField.Amount] = R.string.ledger_amount_invalid
                null
            }
        }
        val gregorian = if (date.isBlank()) originalGregorianDate else JalaliDates.parseJalali(date)
        if (gregorian == null) errors[EntryField.Date] = R.string.ledger_date_invalid
        val liters = volume.takeIf { category == EntryCategory.Fuel }
        if (liters != null && parseFuelVolumeMl(liters) == null) {
            errors[EntryField.Volume] = R.string.ledger_volume_invalid
        }
        val kilometers = if (omitMileage || mileage.isBlank()) {
            null
        } else {
            val digits = mileage.trim().normalizeInputDigits()
            val parsed = digits.takeIf { it.all { digit -> digit in '0'..'9' } }?.toLongOrNull()
            if (parsed == null) errors[EntryField.Mileage] = R.string.ledger_mileage_invalid
            parsed
        }
        if (errors.isNotEmpty() || gregorian == null) return DraftValidation.Invalid(errors)

        val edit = when {
            omitMileage -> LinkedReadingEdit.Remove
            kilometers == null || kilometers == originalMileage -> LinkedReadingEdit.Keep
            else -> LinkedReadingEdit.Set(kilometers)
        }
        return DraftValidation.Valid(EntryInput(vehicleId, category.key, gregorian, rials, liters, notes), kilometers, edit)
    }
}

internal sealed interface DraftValidation {
    data class Invalid(val errors: Map<EntryField, Int>) : DraftValidation
    data class Valid(
        val input: EntryInput,
        val kilometers: Long?,
        val readingEdit: LinkedReadingEdit,
    ) : DraftValidation
}
