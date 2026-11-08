package com.br.androideventscheduler.feature.home.presentation.event

import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import java.time.LocalDate
import java.time.YearMonth

sealed interface HomeUiEvent {
    data class OnDayClicked(
        val date: LocalDate,
    ) : HomeUiEvent

    data object OnPreviousMonthClicked : HomeUiEvent

    data object OnNextMonthClicked : HomeUiEvent

    data object OnMonthHeaderClicked : HomeUiEvent

    data object OnMonthPickerDismissed : HomeUiEvent

    data class OnMonthPickerYearChanged(
        val delta: Int,
    ) : HomeUiEvent

    data class OnMonthPicked(
        val month: YearMonth,
    ) : HomeUiEvent

    data object OnSearchClicked : HomeUiEvent

    data object OnQuickAddClicked : HomeUiEvent

    data class OnScheduleClicked(
        val scheduleId: String,
    ) : HomeUiEvent

    data object OnRetryClicked : HomeUiEvent

    data object OnMenuClicked : HomeUiEvent

    data object OnDrawerDismissed : HomeUiEvent

    data class OnCalendarSourceToggled(
        val source: CalendarSource,
    ) : HomeUiEvent

    data object OnSettingsClicked : HomeUiEvent
}
