package org.unishiasy.vehiclelog.data

import org.unishiasy.vehiclelog.data.local.VehicleEntity

data class VehicleInput(
    val displayLabel: String,
    val catalogKey: String? = null,
    val productionYear: String? = null,
    val productionYearCalendar: String? = null,
)

enum class VehicleInputError {
    BlankDisplayLabel,
    MalformedProductionYear,
    UnsupportedYearCalendar,
    YearAndCalendarMustBePaired,
}

internal sealed interface VehicleInputValidation {
    data class Valid(val vehicle: VehicleEntity) : VehicleInputValidation
    data class Invalid(val reason: VehicleInputError) : VehicleInputValidation
}

internal fun validateVehicleInput(id: String, input: VehicleInput): VehicleInputValidation {
    val label = input.displayLabel.trim()
    if (label.isBlank()) {
        return VehicleInputValidation.Invalid(VehicleInputError.BlankDisplayLabel)
    }

    val yearText = input.productionYear?.trim()?.takeIf { it.isNotEmpty() }
    val calendar = input.productionYearCalendar?.trim()?.takeIf { it.isNotEmpty() }
    if (calendar != null && calendar != "solar_hijri" && calendar != "gregorian") {
        return VehicleInputValidation.Invalid(VehicleInputError.UnsupportedYearCalendar)
    }
    if ((yearText == null) != (calendar == null)) {
        return VehicleInputValidation.Invalid(VehicleInputError.YearAndCalendarMustBePaired)
    }

    val year = yearText?.let { text ->
        val digits = text.map { character ->
            when (character) {
                in '۰'..'۹' -> '0' + (character - '۰')
                in '٠'..'٩' -> '0' + (character - '٠')
                else -> character
            }
        }.joinToString("")
        if (digits.length != 4 || digits.any { it !in '0'..'9' } || digits == "0000") {
            return VehicleInputValidation.Invalid(VehicleInputError.MalformedProductionYear)
        }
        digits.toInt()
    }

    return VehicleInputValidation.Valid(
        VehicleEntity(
            id = id,
            kind = "car",
            displayLabel = label,
            catalogKey = input.catalogKey?.takeUnless { it.isBlank() },
            productionYear = year,
            productionYearCalendar = calendar,
        ),
    )
}
