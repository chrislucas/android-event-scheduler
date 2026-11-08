package com.br.androideventscheduler.feature.home.presentation.component

import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleOccurrence
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.feature.home.domain.calendar.MonthGridBuilder
import com.br.androideventscheduler.feature.home.presentation.state.CalendarDayCell
import com.br.androideventscheduler.feature.home.presentation.state.HomeUiState
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

// Sem preview: dados de exemplo usados apenas pelas previews dos componentes e da tela.

internal val PreviewDate: LocalDate = LocalDate.of(2026, 11, 5)

internal fun previewOccurrence(
    id: String = "preview-1",
    title: String = "Reunião de planejamento",
    date: LocalDate = PreviewDate,
    endDate: LocalDate = date,
    startTime: LocalTime = LocalTime.of(22, 0),
    endTime: LocalTime? = null,
    allDay: Boolean = false,
    sticker: String? = null,
    colorHex: Long = 0xFF26A69A,
    source: CalendarSource = CalendarSource.PERSONAL_ACCOUNT,
    type: ScheduleType = ScheduleType.EVENT,
): ScheduleOccurrence =
    ScheduleOccurrence(
        item =
            ScheduleItem(
                id = id,
                title = title,
                type = type,
                startDate = date,
                endDate = endDate,
                startTime = startTime,
                endTime = endTime,
                isAllDay = allDay,
                colorHex = colorHex,
                calendarSource = source,
                stickerEmoji = sticker,
            ),
        startDate = date,
        endDate = endDate,
    )

/** Conteúdo de exemplo: novembro de 2026, hoje = dia 7, selecionado = dia 5, alguns dias com compromissos. */
internal fun previewContent(
    daySchedules: List<ScheduleOccurrence> = previewDaySchedules(),
    isMonthPickerVisible: Boolean = false,
    isDrawerOpen: Boolean = false,
): HomeUiState.Content {
    val month = YearMonth.of(2026, 11)
    val today = LocalDate.of(2026, 11, 7)
    val colors = listOf(0xFF26A69A, 0xFFAB47BC, 0xFFEF5350, 0xFF5C6BC0)
    val cells =
        MonthGridBuilder().build(month).map { date ->
            val count = if (date.dayOfMonth % 5 == 0) (date.dayOfMonth / 5) else 0
            CalendarDayCell(
                date = date,
                isInVisibleMonth = YearMonth.from(date) == month,
                isToday = date == today,
                isSelected = date == PreviewDate,
                indicatorColors = colors.take(count.coerceAtMost(3)),
                scheduleCount = count,
            )
        }
    return HomeUiState.Content(
        visibleMonth = month,
        selectedDate = PreviewDate,
        cells = cells,
        daySchedules = daySchedules,
        accountEmail = "bia@exemplo.com",
        isMonthPickerVisible = isMonthPickerVisible,
        isDrawerOpen = isDrawerOpen,
    )
}

internal fun previewDaySchedules(): List<ScheduleOccurrence> =
    listOf(
        previewOccurrence(id = "p1", title = "Feriado", allDay = true, source = CalendarSource.FAMILY, colorHex = 0xFFAB47BC),
        previewOccurrence(id = "p2", title = "Academia", sticker = "🥋", startTime = LocalTime.of(7, 0), endTime = LocalTime.of(8, 0)),
        previewOccurrence(id = "p3", title = "Jantar", startTime = LocalTime.of(22, 0), endTime = LocalTime.of(23, 30)),
    )
