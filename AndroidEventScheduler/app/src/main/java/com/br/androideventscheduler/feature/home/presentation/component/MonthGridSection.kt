package com.br.androideventscheduler.feature.home.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.home.domain.calendar.MonthGridBuilder
import com.br.androideventscheduler.feature.home.presentation.mapper.toMonthTitle
import com.br.androideventscheduler.feature.home.presentation.mapper.toSpokenDate
import com.br.androideventscheduler.feature.home.presentation.state.CalendarDayCell
import java.time.LocalDate
import java.time.YearMonth

private val CELL_HEIGHT = 48.dp
private val DAY_CIRCLE_SIZE = 34.dp
private val INDICATOR_SIZE = 5.dp
private val SWIPE_THRESHOLD = 80.dp
private const val OUTSIDE_MONTH_ALPHA = 0.38f

/** Grade 6×7 do mês visível; o deslize horizontal (≥ 80 dp) troca o mês. */
@Composable
internal fun MonthGridSection(
    visibleMonth: YearMonth,
    cells: List<CalendarDayCell>,
    onDayClick: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipeThresholdPx = with(LocalDensity.current) { SWIPE_THRESHOLD.toPx() }
    val currentPrevious by rememberUpdatedState(onPreviousMonth)
    val currentNext by rememberUpdatedState(onNextMonth)
    val gridDescription = stringResource(R.string.home_calendar_description, visibleMonth.toMonthTitle())

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(SemanticsTags.HOME_CALENDAR_GRID)
                .semantics { contentDescription = gridDescription }
                .pointerInput(swipeThresholdPx) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragCancel = { totalDrag = 0f },
                        onDragEnd = {
                            when {
                                totalDrag <= -swipeThresholdPx -> currentNext()
                                totalDrag >= swipeThresholdPx -> currentPrevious()
                            }
                            totalDrag = 0f
                        },
                    ) { _, dragAmount -> totalDrag += dragAmount }
                },
    ) {
        WeekdayHeader()
        cells.chunked(MonthGridBuilder.DAYS_PER_WEEK).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    DayCell(cell = cell, onClick = { onDayClick(cell.date) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val initials = stringArrayResource(R.array.home_weekday_initials)
    // Decorativo: os dias já são falados na descrição de cada célula.
    Row(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }) {
        initials.forEach { initial ->
            Text(
                text = initial,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayCell(
    cell: CalendarDayCell,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spokenDate = cell.date.toSpokenDate()
    val description =
        if (cell.scheduleCount == 0) {
            stringResource(R.string.home_day_description_empty, spokenDate)
        } else {
            pluralStringResource(R.plurals.home_day_description_schedules, cell.scheduleCount, spokenDate, cell.scheduleCount)
        }
    val colors = MaterialTheme.colorScheme
    val numberColor =
        when {
            cell.isToday -> colors.onPrimary
            cell.isInVisibleMonth -> colors.onSurface
            else -> colors.onSurface.copy(alpha = OUTSIDE_MONTH_ALPHA)
        }
    Column(
        modifier =
            modifier
                .height(CELL_HEIGHT)
                .testTag(SemanticsTags.HOME_DAY_CELL_PREFIX + cell.date.toEpochDay())
                .semantics {
                    contentDescription = description
                    selected = cell.isSelected
                }.clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(DAY_CIRCLE_SIZE)
                    .then(if (cell.isToday) Modifier.background(colors.primary, CircleShape) else Modifier)
                    .then(
                        if (cell.isSelected) {
                            Modifier.border(2.dp, if (cell.isToday) colors.onSurface else colors.primary, CircleShape)
                        } else {
                            Modifier
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = cell.date.dayOfMonth.toString(), color = numberColor, style = MaterialTheme.typography.bodyMedium)
        }
        // Indicadores decorativos: a contagem já está na descrição da célula.
        Row(
            modifier = Modifier.padding(top = 2.dp).height(INDICATOR_SIZE),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            cell.indicatorColors.forEach { colorHex ->
                Box(modifier = Modifier.size(INDICATOR_SIZE).background(Color(colorHex), CircleShape))
            }
        }
    }
}

@Preview(showBackground = true, name = "Novembro de 2026")
@Composable
private fun MonthGridSectionPreview() {
    AndroidEventSchedulerTheme {
        val content = previewContent()
        MonthGridSection(
            visibleMonth = content.visibleMonth,
            cells = content.cells,
            onDayClick = {},
            onPreviousMonth = {},
            onNextMonth = {},
        )
    }
}
