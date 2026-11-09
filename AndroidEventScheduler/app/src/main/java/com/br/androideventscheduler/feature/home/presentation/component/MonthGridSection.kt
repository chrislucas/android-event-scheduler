package com.br.androideventscheduler.feature.home.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
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

/**
 * Grade 6×7 do mês visível; o deslize horizontal (≥ 80 dp) troca o mês. O toque simples seleciona o dia na hora
 * ([onDayClick]); o toque duplo (ou a ação de acessibilidade personalizada) chama [onDayDoubleTap] (spec 18).
 */
@Composable
internal fun MonthGridSection(
    visibleMonth: YearMonth,
    cells: List<CalendarDayCell>,
    onDayClick: (LocalDate) -> Unit,
    onDayDoubleTap: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipeThresholdPx = with(LocalDensity.current) { SWIPE_THRESHOLD.toPx() }
    val currentPrevious by rememberUpdatedState(onPreviousMonth)
    val currentNext by rememberUpdatedState(onNextMonth)
    val gridDescription = stringResource(R.string.home_calendar_description, visibleMonth.toMonthTitle())
    val doubleTapTimeout = LocalViewConfiguration.current.doubleTapTimeoutMillis
    // Um detector para a grade toda: toques em dias diferentes zeram a contagem (R2.2).
    // Chaveado pela posição da célula: o 1º toque num dia de mês vizinho troca o mês e redesenha a grade (R3.1).
    val doubleTapDetector = remember(doubleTapTimeout) { DayDoubleTapDetector<Int>(doubleTapTimeout) }
    val firstTapDate = remember { FirstTapDate() }

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
                            doubleTapDetector.reset()
                            totalDrag = 0f
                        },
                    ) { _, dragAmount -> totalDrag += dragAmount }
                },
    ) {
        WeekdayHeader()
        cells.chunked(MonthGridBuilder.DAYS_PER_WEEK).forEachIndexed { weekIndex, week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEachIndexed { dayIndex, cell ->
                    DayCell(
                        cell = cell,
                        slot = weekIndex * MonthGridBuilder.DAYS_PER_WEEK + dayIndex,
                        doubleTapDetector = doubleTapDetector,
                        firstTapDate = firstTapDate,
                        onClick = { onDayClick(cell.date) },
                        onDoubleTap = onDayDoubleTap,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Data do primeiro toque do par: depois dele a posição pode mostrar outra data (mês vizinho). */
private class FirstTapDate {
    var value: LocalDate? = null
}

/**
 * Observa, sem consumir, cada toque completo na célula (passe `Initial`, antes do `clickable`), de modo que a seleção
 * continue imediata. Gestos que se deslocam além do `touchSlop` ou são cancelados (deslize de troca de mês) não contam (R2.5).
 */
private fun Modifier.observeDoubleTap(
    slot: Int,
    date: LocalDate,
    detector: DayDoubleTapDetector<Int>,
    firstTapDate: FirstTapDate,
    onDoubleTap: (LocalDate) -> Unit,
): Modifier =
    pointerInput(slot, date, detector) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
            val isTap = up != null && (up.position - down.position).getDistance() < viewConfiguration.touchSlop
            if (isTap) {
                if (detector.onTap(slot, up.uptimeMillis)) {
                    onDoubleTap(firstTapDate.value ?: date)
                } else {
                    firstTapDate.value = date
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
    slot: Int,
    doubleTapDetector: DayDoubleTapDetector<Int>,
    firstTapDate: FirstTapDate,
    onClick: () -> Unit,
    onDoubleTap: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spokenDate = cell.date.toSpokenDate()
    val addActionLabel = stringResource(R.string.home_day_add_action, spokenDate)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
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
                    // TalkBack consome o toque duplo como "ativar": a criação precisa de uma ação própria (R4.1).
                    customActions =
                        listOf(
                            CustomAccessibilityAction(addActionLabel) {
                                currentOnDoubleTap(cell.date)
                                true
                            },
                        )
                }.observeDoubleTap(slot, cell.date, doubleTapDetector, firstTapDate) { date -> currentOnDoubleTap(date) }
                .clickable(role = Role.Button, onClick = onClick),
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
                Box(
                    modifier =
                        Modifier
                            .size(INDICATOR_SIZE)
                            .background(Color(colorHex), CircleShape)
                            .testTag("${SemanticsTags.HOME_DAY_INDICATOR_PREFIX}${cell.date.toEpochDay()}_$colorHex"),
                )
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
            onDayDoubleTap = {},
            onPreviousMonth = {},
            onNextMonth = {},
        )
    }
}
