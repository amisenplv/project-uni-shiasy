package org.unishiasy.vehiclelog.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.domain.JalaliDateParts
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.withPersianDigits
import org.unishiasy.vehiclelog.ui.theme.VehicleLogShapes

private val weekdayHeaders = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

@Composable
internal fun JalaliDatePickerDialog(
    initialDateText: String,
    todayDate: LocalDate,
    onSelectDate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val todayParts = JalaliDates.jalaliPartsOf(todayDate)
        ?: JalaliDateParts(JalaliDates.firstSupportedYear, 1, 1)
    val initialParts = JalaliDates.parseJalaliParts(initialDateText) ?: todayParts

    var viewYear by rememberSaveable { mutableIntStateOf(initialParts.year) }
    var viewMonth by rememberSaveable { mutableIntStateOf(initialParts.month) }

    val monthLength = JalaliDates.monthLength(viewYear, viewMonth)
    val startWeekday = JalaliDates.dayOfWeekOfFirst(viewYear, viewMonth) ?: 0
    val monthTitle = "${JalaliDates.monthName(viewMonth)} ${viewYear.toString().withPersianDigits()}"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .testTag("jalaliDatePickerDialog"),
            shape = VehicleLogShapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.date_picker_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Month & Year Navigation Header
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = VehicleLogShapes.medium,
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val prevMonthDesc = stringResource(R.string.date_picker_prev_month)
                        TextButton(
                            onClick = {
                                if (viewMonth > 1) {
                                    viewMonth -= 1
                                } else if (viewYear > JalaliDates.firstSupportedYear) {
                                    viewYear -= 1
                                    viewMonth = 12
                                }
                            },
                            enabled = viewYear > JalaliDates.firstSupportedYear || viewMonth > 1,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("jalaliPickerPrevMonth")
                                .semantics { contentDescription = prevMonthDesc },
                        ) {
                            Text("‹", style = MaterialTheme.typography.titleLarge)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val prevYearDesc = stringResource(R.string.date_picker_prev_year)
                            TextButton(
                                onClick = { if (viewYear > JalaliDates.firstSupportedYear) viewYear -= 1 },
                                enabled = viewYear > JalaliDates.firstSupportedYear,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("jalaliPickerPrevYear")
                                    .semantics { contentDescription = prevYearDesc },
                            ) {
                                Text("−", style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                text = monthTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("jalaliPickerMonthTitle"),
                            )
                            val nextYearDesc = stringResource(R.string.date_picker_next_year)
                            TextButton(
                                onClick = { if (viewYear < JalaliDates.lastSupportedYear) viewYear += 1 },
                                enabled = viewYear < JalaliDates.lastSupportedYear,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("jalaliPickerNextYear")
                                    .semantics { contentDescription = nextYearDesc },
                            ) {
                                Text("+", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        val nextMonthDesc = stringResource(R.string.date_picker_next_month)
                        TextButton(
                            onClick = {
                                if (viewMonth < 12) {
                                    viewMonth += 1
                                } else if (viewYear < JalaliDates.lastSupportedYear) {
                                    viewYear += 1
                                    viewMonth = 1
                                }
                            },
                            enabled = viewYear < JalaliDates.lastSupportedYear || viewMonth < 12,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("jalaliPickerNextMonth")
                                .semantics { contentDescription = nextMonthDesc },
                        ) {
                            Text("›", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }

                // Saturday-first Weekday Header
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (header in weekdayHeaders) {
                        Text(
                            text = header,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Day Grid
                val totalCells = startWeekday + monthLength
                val rowCount = (totalCells + 6) / 7
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until rowCount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val day = cellIndex - startWeekday + 1
                                if (day in 1..monthLength) {
                                    val isSelected = viewYear == initialParts.year &&
                                        viewMonth == initialParts.month &&
                                        day == initialParts.day
                                    val isToday = viewYear == todayParts.year &&
                                        viewMonth == todayParts.month &&
                                        day == todayParts.day
                                    val containerColor = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                    val contentColor = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(containerColor)
                                            .clickable {
                                                onSelectDate(JalaliDates.formatJalaliParts(viewYear, viewMonth, day))
                                            }
                                            .testTag("jalaliPickerDay:$day"),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = day.toString().withPersianDigits(),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            ),
                                            color = contentColor,
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).heightIn(min = 48.dp))
                                }
                            }
                        }
                    }
                }

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("jalaliPickerDismiss"),
                    ) {
                        Text(stringResource(R.string.date_picker_close))
                    }
                    Button(
                        onClick = {
                            onSelectDate(
                                JalaliDates.formatJalaliParts(todayParts.year, todayParts.month, todayParts.day),
                            )
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("jalaliPickerToday"),
                        shape = VehicleLogShapes.medium,
                    ) {
                        Text(stringResource(R.string.date_chip_today))
                    }
                }
            }
        }
    }
}
