package org.unishiasy.vehiclelog.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import kotlinx.coroutines.launch
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.domain.JalaliDateParts
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.withPersianDigits
import org.unishiasy.vehiclelog.ui.theme.VehicleLogShapes

@Composable
internal fun JalaliDatePickerDialog(
    initialDateText: String,
    todayDate: LocalDate,
    onSelectDate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val firstYear = JalaliDates.firstSupportedYear
    val today = JalaliDates.jalaliPartsOf(todayDate)
    val initial = JalaliDates.parseJalaliParts(initialDateText) ?: today ?: JalaliDateParts(firstYear, 1, 1)
    var todayRequest by rememberSaveable { mutableIntStateOf(0) }
    val startingDate = today.takeIf { todayRequest > 0 } ?: initial
    val (yearWheel, monthWheel, dayWheel) = key(todayRequest) {
        Triple(
            rememberLazyListState(startingDate.year - firstYear),
            rememberLazyListState(startingDate.month - 1),
            rememberLazyListState(startingDate.day - 1),
        )
    }
    val density = LocalDensity.current
    val rowHeight = if (density.fontScale >= 1.3f) 64.dp else 48.dp
    val rowPixels = with(density) { rowHeight.roundToPx() }
    val years = firstYear..JalaliDates.lastSupportedYear
    val months = 1..12
    val year = years.first + yearWheel.centerIndex(rowPixels, years.count())
    val month = months.first + monthWheel.centerIndex(rowPixels, months.count())
    val monthLength = JalaliDates.monthLength(year, month)
    var settledMonthLength by remember(yearWheel, monthWheel) { mutableIntStateOf(monthLength) }
    LaunchedEffect(yearWheel, monthWheel, rowPixels) {
        snapshotFlow {
            if (yearWheel.isScrollInProgress || monthWheel.isScrollInProgress) null else {
                val settledYear = years.first + yearWheel.centerIndex(rowPixels, years.count())
                val settledMonth = months.first + monthWheel.centerIndex(rowPixels, months.count())
                JalaliDates.monthLength(settledYear, settledMonth)
            }
        }.collect { length -> if (length != null) settledMonthLength = length }
    }
    val days = 1..settledMonthLength
    val day = days.first + dayWheel.centerIndex(rowPixels, days.count())
    val settled = !yearWheel.isScrollInProgress && !monthWheel.isScrollInProgress &&
        !dayWheel.isScrollInProgress && settledMonthLength == monthLength

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth().testTag("jalaliDatePickerDialog"),
                shape = VehicleLogShapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DismissibleHeading(
                        title = stringResource(R.string.date_picker_title),
                        onClose = onDismiss,
                        closeModifier = Modifier.testTag("jalaliPickerDismiss"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DateWheel(R.string.date_picker_day, "Day", days, dayWheel, rowHeight, rowPixels, Modifier.weight(1f))
                        DateWheel(
                            R.string.date_picker_month, "Month", months, monthWheel, rowHeight, rowPixels,
                            Modifier.weight(1.5f), JalaliDates::monthName,
                        )
                        DateWheel(R.string.date_picker_year, "Year", years, yearWheel, rowHeight, rowPixels, Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { todayRequest++ },
                            enabled = today != null && settled,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("jalaliPickerToday"),
                        ) {
                            Text(stringResource(R.string.date_chip_today))
                        }
                        Button(
                            onClick = { onSelectDate(JalaliDates.formatJalaliParts(year, month, day)) },
                            enabled = settled,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("jalaliPickerConfirm"),
                        ) {
                            Text(stringResource(R.string.date_picker_confirm))
                        }
                    }
                }
            }
        }
    }
}

// LazyColumn excludes its leading content padding from the first visible index.
// The padding puts that item in the center at rest.
private fun LazyListState.centerIndex(rowPixels: Int, itemCount: Int): Int =
    (firstVisibleItemIndex + if (firstVisibleItemScrollOffset >= (rowPixels + 1) / 2) 1 else 0)
        .coerceIn(0, itemCount - 1)

@Composable
private fun DateWheel(
    @StringRes heading: Int,
    tag: String,
    values: IntRange,
    state: LazyListState,
    rowHeight: Dp,
    rowPixels: Int,
    modifier: Modifier,
    format: (Int) -> String = { it.toString().withPersianDigits() },
) {
    val label = stringResource(heading)
    val selectedIndex = state.centerIndex(rowPixels, values.count())
    val scope = rememberCoroutineScope()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box(Modifier.fillMaxWidth().height(rowHeight * 3), contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxWidth().height(rowHeight)
                    .clip(VehicleLogShapes.small).background(MaterialTheme.colorScheme.primaryContainer),
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(rowHeight * 3)
                    .testTag("jalaliPicker${tag}Wheel")
                    .selectableGroup()
                    .semantics { contentDescription = label },
                state = state,
                contentPadding = PaddingValues(vertical = rowHeight),
                flingBehavior = rememberSnapFlingBehavior(state),
            ) {
                items(values.count(), key = { values.first + it }) { index ->
                    val value = values.first + index
                    val selected = index == selectedIndex
                    Box(
                        modifier = Modifier.fillMaxWidth().height(rowHeight)
                            .selectable(selected = selected, onClick = { scope.launch { state.animateScrollToItem(index) } })
                            .testTag("jalaliPicker$tag:$value"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            format(value),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
