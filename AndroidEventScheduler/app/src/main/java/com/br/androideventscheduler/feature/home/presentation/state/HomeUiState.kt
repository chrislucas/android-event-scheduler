package com.br.androideventscheduler.feature.home.presentation.state

import androidx.annotation.StringRes
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleOccurrence
import java.time.LocalDate
import java.time.YearMonth

sealed class HomeUiState {
    /** Somente na primeira carga; trocar de mês mantém o [Content] anterior até o novo chegar. */
    data object Loading : HomeUiState()

    data class Content(
        val visibleMonth: YearMonth,
        val selectedDate: LocalDate,
        val cells: List<CalendarDayCell>,
        val daySchedules: List<ScheduleOccurrence>,
        /** Rótulo do calendário "conta pessoal" (e-mail ativo das configurações). */
        val accountEmail: String,
        val isMonthPickerVisible: Boolean = false,
        val monthPickerYear: Int = visibleMonth.year,
        val isDrawerOpen: Boolean = false,
        /** Calendários visíveis na grade e na lista (todos, por padrão). */
        val enabledSources: Set<CalendarSource> = CalendarSource.entries.toSet(),
    ) : HomeUiState()

    data class Error(
        @StringRes val messageRes: Int,
    ) : HomeUiState()
}
