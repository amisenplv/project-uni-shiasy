package org.unishiasy.vehiclelog.ui.tools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.util.Locale
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.domain.PersianDigits
import org.unishiasy.vehiclelog.domain.toPersianGroupedDigits
import org.unishiasy.vehiclelog.domain.withPersianDigits
import org.unishiasy.vehiclelog.ui.components.PersianNumericTransformation

/**
 * State holding inputs and computed results for the trip cost calculator.
 *
 * `totalFuelLiters` and `totalCostToman` are computed properties so `.copy()`
 * recomputes results reliably without stale cached values.
 */
data class TripCalculatorState(
    val distanceKm: String = "",
    val ratePerLiterToman: String = "",
    val consumptionL100Km: String = "",
) {
    /**
     * Computed total fuel required in Liters based on distance and consumption.
     * Null if distance or consumption is missing, non-positive, or unparseable.
     */
    val totalFuelLiters: Double?
        get() = calculateFuel(distanceKm, consumptionL100Km)

    /**
     * Computed total estimated fuel cost in Toman.
     * Null if fuel is null or rate is missing, non-positive, or unparseable.
     */
    val totalCostToman: Long?
        get() = calculateCost(totalFuelLiters, ratePerLiterToman)

    val isDistanceInvalid: Boolean
        get() = distanceKm.isNotBlank() && (parseInput(distanceKm).let { it == null || it <= 0.0 })

    val isConsumptionInvalid: Boolean
        get() = consumptionL100Km.isNotBlank() && (parseInput(consumptionL100Km).let { it == null || it <= 0.0 })

    val isRateInvalid: Boolean
        get() = ratePerLiterToman.isNotBlank() && (parseInput(ratePerLiterToman).let { it == null || it <= 0.0 })

    /**
     * Secondary constructor for backward compatibility with 5-parameter callers.
     */
    constructor(
        distanceKm: String,
        ratePerLiterToman: String,
        consumptionL100Km: String,
        @Suppress("UNUSED_PARAMETER") totalFuelLiters: Double? = null,
        @Suppress("UNUSED_PARAMETER") totalCostToman: Long? = null,
    ) : this(distanceKm, ratePerLiterToman, consumptionL100Km)

    companion object {
        fun calculateFuel(distanceKm: String, consumptionL100Km: String): Double? {
            val dist = parseInput(distanceKm)
            val cons = parseInput(consumptionL100Km)
            return if (dist != null && cons != null && dist > 0.0 && cons > 0.0) {
                (dist * cons) / 100.0
            } else {
                null
            }
        }

        fun calculateCost(totalFuelLiters: Double?, ratePerLiterToman: String): Long? {
            if (totalFuelLiters == null || totalFuelLiters <= 0.0) return null
            val rate = parseInput(ratePerLiterToman)
            return if (rate != null && rate > 0.0) {
                kotlin.math.round(totalFuelLiters * rate).toLong()
            } else {
                null
            }
        }

        fun parseInput(input: String): Double? {
            val cleaned = PersianDigits.normalize(input)
                .replace("٬", "")
                .replace(",", "")
                .replace("٫", ".")
                .replace("/", ".")
                .trim()
            return cleaned.toDoubleOrNull()
        }
    }
}

/**
 * Stateful Trip Calculator Screen.
 */
@Composable
fun TripCalculatorScreen(
    modifier: Modifier = Modifier,
    initialConsumption: Double? = null,
    onBack: (() -> Unit)? = null,
) {
    var distance by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var consumption by rememberSaveable {
        mutableStateOf(
            if (initialConsumption != null && initialConsumption > 0) {
                String.format(Locale.ROOT, "%.1f", initialConsumption)
            } else {
                ""
            }
        )
    }

    val state = remember(distance, rate, consumption) {
        TripCalculatorState(
            distanceKm = distance,
            ratePerLiterToman = rate,
            consumptionL100Km = consumption,
        )
    }

    TripCalculatorScreen(
        state = state,
        onDistanceChange = { distance = it },
        onRateChange = { rate = it },
        onConsumptionChange = { consumption = it },
        onReset = {
            distance = ""
            rate = ""
            consumption = ""
        },
        modifier = modifier,
        onBack = onBack,
    )
}

/**
 * Stateless Trip Calculator Screen Composable.
 *
 * Uses semantic MaterialTheme.colorScheme tokens to properly support both dark
 * and light modes while matching the dark slate and lavender cab aesthetic in dark mode.
 */
@Composable
fun TripCalculatorScreen(
    state: TripCalculatorState,
    onDistanceChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onConsumptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onReset: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .testTag("tripCalculatorScreen")
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("tripBackButton"),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.date_picker_close),
                            tint = colors.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = stringResource(R.string.trip_calculator_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }

            // Results Summary Card
            TripResultCard(
                state = state,
            )

            // Input Fields Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface,
                ),
                border = BorderStroke(1.dp, colors.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Distance Input
                    OutlinedTextField(
                        value = state.distanceKm,
                        onValueChange = onDistanceChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("tripDistanceInput"),
                        label = { Text(stringResource(R.string.trip_distance_label)) },
                        placeholder = { Text(stringResource(R.string.trip_distance_hint)) },
                        trailingIcon = {
                            Text(
                                text = stringResource(R.string.trip_unit_km),
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                        },
                        isError = state.isDistanceInvalid,
                        supportingText = if (state.isDistanceInvalid) {
                            { Text(stringResource(R.string.trip_distance_invalid)) }
                        } else null,
                        singleLine = true,
                        visualTransformation = PersianNumericTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )

                    // Consumption Input
                    OutlinedTextField(
                        value = state.consumptionL100Km,
                        onValueChange = onConsumptionChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("tripConsumptionInput"),
                        label = { Text(stringResource(R.string.trip_consumption_label)) },
                        placeholder = { Text(stringResource(R.string.trip_consumption_hint)) },
                        trailingIcon = {
                            Text(
                                text = stringResource(R.string.trip_unit_liter),
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                        },
                        isError = state.isConsumptionInvalid,
                        supportingText = if (state.isConsumptionInvalid) {
                            { Text(stringResource(R.string.trip_consumption_invalid)) }
                        } else null,
                        singleLine = true,
                        visualTransformation = PersianNumericTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )

                    // Fuel Rate Input
                    OutlinedTextField(
                        value = state.ratePerLiterToman,
                        onValueChange = onRateChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("tripRateInput"),
                        label = { Text(stringResource(R.string.trip_rate_label)) },
                        placeholder = { Text(stringResource(R.string.trip_rate_hint)) },
                        trailingIcon = {
                            Text(
                                text = stringResource(R.string.trip_unit_toman),
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                        },
                        isError = state.isRateInvalid,
                        supportingText = if (state.isRateInvalid) {
                            { Text(stringResource(R.string.trip_rate_invalid)) }
                        } else null,
                        singleLine = true,
                        visualTransformation = PersianNumericTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )

                    // Quick Rate Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onRateChange("1500") },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("ratePreset:1500"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colors.primary,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.trip_preset_ration),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        OutlinedButton(
                            onClick = { onRateChange("3000") },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("ratePreset:3000"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colors.primary,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.trip_preset_free),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            // Reset Button
            OutlinedButton(
                onClick = {
                    if (onReset != null) {
                        onReset()
                    } else {
                        onDistanceChange("")
                        onRateChange("")
                        onConsumptionChange("")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("tripResetButton"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.onSurfaceVariant,
                ),
            ) {
                Text(
                    text = stringResource(R.string.trip_reset),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun TripResultCard(
    state: TripCalculatorState,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val totalFuelLiters = state.totalFuelLiters
    val totalCostToman = state.totalCostToman

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface,
        ),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Estimated Cost Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.trip_total_cost_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))

                    val (costText, costColor, costStyle) = when {
                        totalCostToman != null -> Triple(
                            "${totalCostToman.toPersianGroupedDigits()} ${stringResource(R.string.trip_unit_toman)}",
                            colors.primary,
                            MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        state.isRateInvalid -> Triple(
                            stringResource(R.string.trip_rate_invalid),
                            colors.error,
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        totalFuelLiters != null && state.ratePerLiterToman.isBlank() -> Triple(
                            stringResource(R.string.trip_cost_rate_required),
                            colors.onSurfaceVariant,
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        )
                        else -> Triple(
                            stringResource(R.string.trip_cost_prompt),
                            colors.onSurfaceVariant,
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        )
                    }

                    Text(
                        text = costText,
                        style = costStyle,
                        color = costColor,
                        modifier = Modifier.testTag("tripTotalCost"),
                    )
                }
            }

            HorizontalDivider(
                color = colors.outlineVariant,
                thickness = 1.dp,
            )

            // Required Fuel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.trip_total_fuel_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )

                val (fuelText, fuelColor, fuelStyle) = when {
                    totalFuelLiters != null -> {
                        val fuelFormatted = if (totalFuelLiters % 1.0 == 0.0) {
                            totalFuelLiters.toLong().toString().withPersianDigits()
                        } else {
                            String.format(Locale.ROOT, "%.1f", totalFuelLiters)
                                .replace('.', '٫')
                                .withPersianDigits()
                        }
                        Triple(
                            "$fuelFormatted ${stringResource(R.string.trip_unit_liter)}",
                            colors.onSurface,
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    state.isDistanceInvalid -> Triple(
                        stringResource(R.string.trip_distance_invalid),
                        colors.error,
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    )
                    state.isConsumptionInvalid -> Triple(
                        stringResource(R.string.trip_consumption_invalid),
                        colors.error,
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    )
                    else -> Triple(
                        stringResource(R.string.trip_fuel_prompt),
                        colors.onSurfaceVariant,
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    )
                }

                Text(
                    text = fuelText,
                    style = fuelStyle,
                    color = fuelColor,
                    modifier = Modifier.testTag("tripTotalFuel"),
                )
            }
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary,
)
