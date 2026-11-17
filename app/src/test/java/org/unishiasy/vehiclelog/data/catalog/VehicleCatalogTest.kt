package org.unishiasy.vehiclelog.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleCatalogTest {
    @Test
    fun normalizesArabicLettersDigitSetsWhitespaceAndJoiners() {
        assertEquals("خیالگرد کیت 203 vx", normalizeVehicleSearch(" \tخيال‌گرد\u00a0كىت ۲٠3\nVX  "))
    }

    @Test
    fun matchesPartialTermsAcrossLabelsAndAliasesWithoutChangingDisplayText() {
        val catalog = fixtureCatalog()

        val match = catalog.search("KiTE خيالگرد ٢0۳ vx").single()

        assertEquals("fixture-sky-kite", match.key)
        assertEquals("آزمایشی خیال‌گرد", match.manufacturer)
        assertEquals("بادبادک ۲۰۳", match.model)
        assertEquals("VX", match.variant)
        assertEquals(listOf("Sky Kite", "بادک ۲۰۳"), match.aliases)
        assertEquals(listOf(match), catalog.search("بادك SKY"))
        assertEquals("fixture-cloud-lantern", catalog.search("CLOUD lant فانوس").single().key)
        assertTrue(catalog.search("kite ناموجود").isEmpty())
    }

    @Test
    fun returnsEmptyQueryAndMultipleMatchesInStableKeyOrder() {
        val catalog = fixtureCatalog()
        val expected = listOf("fixture-cloud-lantern", "fixture-sky-kite")

        for (query in listOf("", " \t‌ ", "۲۰۳", "آزمایشی 203")) {
            assertEquals(expected, catalog.search(query).map { it.key })
        }
    }

    @Test
    fun allowsEmptyCatalogAndOmittedOptionalFields() {
        assertTrue(load("""{"schemaVersion":1,"entries":[]}""").search("").isEmpty())
        assertEquals(
            VehicleCatalogEntry("fictional", "خیالی", "نمونه"),
            load(document("""{"key":"fictional","manufacturer":"خیالی","model":"نمونه"}""")).search("").single(),
        )
    }

    @Test
    fun rejectsDuplicateKeysEvenWhenLabelsDiffer() {
        val first = """{"key":"same","manufacturer":"خیالی","model":"یک"}"""
        val second = """{"key":"same","manufacturer":"خیالی","model":"دو"}"""

        assertRejected(document("$first,$second"), "Duplicate vehicle catalog key")
    }

    @Test
    fun rejectsMalformedDocumentsAndUnsupportedVersions() {
        assertRejected("""{"schemaVersion":1,"entries":[""", "Malformed vehicle catalog JSON")
        assertRejected("[]", "must be a JSON object")
        assertRejected("""{"entries":[]}""", "schemaVersion")
        assertRejected("""{"schemaVersion":"1","entries":[]}""", "schemaVersion")
        assertRejected("""{"schemaVersion":2,"entries":[]}""", "schemaVersion")
        assertRejected("""{"schemaVersion":1,"entries":{}}""", "entries must be an array")
    }

    @Test
    fun rejectsMalformedRecordsAndInvalidFieldTypes() {
        assertRejected(document("null"), "entries[0] must be an object")
        assertRejected(document("""{"manufacturer":"خیالی","model":"نمونه"}"""), "entries[0].key")
        assertRejected(document("""{"key":"bad key","manufacturer":"خیالی","model":"نمونه"}"""), "entries[0].key")
        assertRejected(document("""{"key":"valid","model":"نمونه"}"""), "entries[0].manufacturer")
        assertRejected(document("""{"key":"valid","manufacturer":"خیالی","model":" "}"""), "entries[0].model")
        val record = """"key":"valid","manufacturer":"خیالی","model":"نمونه""""
        assertRejected(document("""{$record,"variant":23}"""), "entries[0].variant")
        assertRejected(document("""{$record,"aliases":"نمونه"}"""), "entries[0].aliases")
        assertRejected(document("""{$record,"aliases":["نمونه",false]}"""), "entries[0].aliases[1]")
    }

    private fun fixtureCatalog(): VehicleCatalog = checkNotNull(
        javaClass.getResourceAsStream("/vehicle_catalog_fixture.json"),
    ).bufferedReader(Charsets.UTF_8).use(VehicleCatalogLoader::load)

    private fun load(json: String): VehicleCatalog = json.reader().use(VehicleCatalogLoader::load)

    private fun document(records: String): String = """{"schemaVersion":1,"entries":[$records]}"""

    private fun assertRejected(json: String, message: String) {
        val error = assertThrows(IllegalArgumentException::class.java) { load(json) }
        assertTrue(error.message, error.message.orEmpty().contains(message))
    }
}
