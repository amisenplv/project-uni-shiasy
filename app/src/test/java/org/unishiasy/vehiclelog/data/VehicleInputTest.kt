package org.unishiasy.vehiclelog.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleInputTest {
    private val input = VehicleInput(displayLabel = "پژو ۲۰۶")

    @Test
    fun trimsOuterWhitespaceAndPreservesPersianText() {
        val label = "خودروی‌من"
        val result = validateVehicleInput("saved-id", VehicleInput("  $label\t")) as
            VehicleInputValidation.Valid

        assertEquals(label, result.vehicle.displayLabel)
        assertEquals("saved-id", result.vehicle.id)
        assertEquals("car", result.vehicle.kind)
    }

    @Test
    fun rejectsBlankLabel() {
        assertInvalid(VehicleInputError.BlankDisplayLabel, VehicleInput(" \t\n"))
    }

    @Test
    fun normalizesMixedDigitsAndPreservesCalendarAndYear() {
        for (calendar in listOf("solar_hijri", "gregorian")) {
            val result = validateVehicleInput(
                "id",
                input.copy(productionYear = " ۱٤0۲ ", productionYearCalendar = calendar),
            ) as VehicleInputValidation.Valid

            assertEquals(1402, result.vehicle.productionYear)
            assertEquals(calendar, result.vehicle.productionYearCalendar)
        }
    }

    @Test
    fun permitsOmittedOrBlankYearAndCalendar() {
        for (candidate in listOf(input, input.copy(productionYear = " ", productionYearCalendar = "\t"))) {
            val result = validateVehicleInput("id", candidate) as VehicleInputValidation.Valid
            assertNull(result.vehicle.productionYear)
            assertNull(result.vehicle.productionYearCalendar)
        }
    }

    @Test
    fun rejectsMalformedYears() {
        for (year in listOf("۱۴۰", "۱۴۰۲۰", "14 0۲", "+1402", "۱۴۰x", "٠٠٠٠")) {
            assertInvalid(
                VehicleInputError.MalformedProductionYear,
                input.copy(productionYear = year, productionYearCalendar = "solar_hijri"),
            )
        }
    }

    @Test
    fun rejectsMissingOrUnsupportedCalendarAndCalendarWithoutYear() {
        assertInvalid(VehicleInputError.YearAndCalendarMustBePaired, input.copy(productionYear = "۱۴۰۲"))
        assertInvalid(
            VehicleInputError.YearAndCalendarMustBePaired,
            input.copy(productionYearCalendar = "gregorian"),
        )
        assertInvalid(
            VehicleInputError.UnsupportedYearCalendar,
            input.copy(productionYear = "1402", productionYearCalendar = "unknown"),
        )
    }

    @Test
    fun retainsNonblankCatalogKeyAsOpaqueMetadata() {
        val result = validateVehicleInput(
            "id",
            input.copy(catalogKey = "unverified-key"),
        ) as VehicleInputValidation.Valid

        assertEquals("unverified-key", result.vehicle.catalogKey)
        val withoutKey = validateVehicleInput("id", input.copy(catalogKey = " ")) as VehicleInputValidation.Valid
        assertNull(withoutKey.vehicle.catalogKey)
    }

    private fun assertInvalid(reason: VehicleInputError, input: VehicleInput) {
        assertEquals(VehicleInputValidation.Invalid(reason), validateVehicleInput("id", input))
    }
}
