package org.unishiasy.vehiclelog.ui.ledger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.unishiasy.vehiclelog.data.local.HistoryEntry

class HistoryFilterStateTest {
    private val labels = mapOf(
        "fuel" to "سوخت",
        "oil_change" to "تعویض روغن",
        "repair" to "تعمیر",
        "other" to "سایر",
    )

    private fun entry(
        id: String,
        category: String,
        occurredOn: String,
        amountRials: Long? = null,
        notes: String? = null,
        km: Long? = null,
        volumeMl: Long? = null,
    ) = HistoryEntry(
        id = id,
        vehicleId = "v1",
        category = category,
        occurredOn = occurredOn,
        createdAtEpochMillis = 1L,
        notes = notes,
        amountRials = amountRials,
        fuelVolumeMl = volumeMl,
        linkedOdometerKm = km,
    )

    @Test
    fun normalizeSearchTextHandlesPersianArabicDigitsAndVariants() {
        assertEquals("123456", normalizeSearchText("۱۲۳٬۴۵۶"))
        assertEquals("7890", normalizeSearchText("٧٨٩٠"))
        assertEquals("کیلومترروغنموتور", normalizeSearchText("كيلومتر روغن\u200cموتور"))
    }

    @Test
    fun filterAndGroupHistoryGroupsByJalaliMonthAndFiltersNotesDigitsAndCategory() {
        val fuelFarvardin = entry(
            id = "e1",
            category = "fuel",
            occurredOn = "2026-03-21", // 1405-01-01
            amountRials = 2_500_000L, // 250,000 Toman
            notes = "بنزین سوپر",
            km = 42_000L,
            volumeMl = 35_000L,
        )
        val oilEsfand = entry(
            id = "e2",
            category = "oil_change",
            occurredOn = "2026-03-10", // 1404-12-19
            amountRials = 12_000_000L, // 1,200,000 Toman
            notes = "تعويض روغن و فيلتر", // Arabic ي
            km = 41_500L,
        )
        val repairMalformed = entry(
            id = "e3",
            category = "repair",
            occurredOn = "bad-date",
            amountRials = null,
            notes = null,
        )
        val all = listOf(fuelFarvardin, oilEsfand, repairMalformed)

        // All entries -> 3 groups
        val ungrouped = filterAndGroupHistory(all, HistoryFilterState(), labels, "سایر تاریخ‌ها")
        assertEquals(listOf("1405-1", "1404-12", "other"), ungrouped.map { it.monthKey })
        assertTrue(ungrouped[0].headerLabel.contains("فروردین"))
        assertTrue(ungrouped[1].headerLabel.contains("اسفند"))
        assertEquals("سایر تاریخ‌ها", ungrouped[2].headerLabel)

        // Arabic/Persian letter normalization in notes query
        val byNote = filterAndGroupHistory(all, HistoryFilterState(query = "فیلتر"), labels, "سایر تاریخ‌ها")
        assertEquals(listOf("e2"), byNote.flatMap { g -> g.entries.map { it.id } })

        // Persian digits Toman amount query
        val byAmount = filterAndGroupHistory(all, HistoryFilterState(query = "۲۵۰٬۰۰۰"), labels, "سایر تاریخ‌ها")
        assertEquals(listOf("e1"), byAmount.flatMap { g -> g.entries.map { it.id } })

        // Odometer query
        val byKm = filterAndGroupHistory(all, HistoryFilterState(query = "41500"), labels, "سایر تاریخ‌ها")
        assertEquals(listOf("e2"), byKm.flatMap { g -> g.entries.map { it.id } })

        // Category chip filter
        val byCategory = filterAndGroupHistory(all, HistoryFilterState(categoryKey = "fuel"), labels, "سایر تاریخ‌ها")
        assertEquals(listOf("e1"), byCategory.flatMap { g -> g.entries.map { it.id } })

        // Empty result
        val empty = filterAndGroupHistory(all, HistoryFilterState(query = "عبارت ناموجود"), labels, "سایر تاریخ‌ها")
        assertTrue(empty.isEmpty())

        assertFalse(HistoryFilterState().isActive)
        assertTrue(HistoryFilterState(query = "a").isActive)
        assertTrue(HistoryFilterState(categoryKey = "fuel").isActive)
    }
}
