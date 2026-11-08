package com.br.androideventscheduler.feature.home.presentation.state

import java.time.LocalDate

/** Uma célula da grade mensal; [indicatorColors] tem no máximo 3 cores distintas, na ordem de horário. */
data class CalendarDayCell(
    val date: LocalDate,
    val isInVisibleMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val indicatorColors: List<Long>,
    val scheduleCount: Int,
)
