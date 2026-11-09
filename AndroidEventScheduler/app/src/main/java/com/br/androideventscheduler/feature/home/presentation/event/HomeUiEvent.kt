package com.br.androideventscheduler.feature.home.presentation.event

import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import java.time.LocalDate
import java.time.YearMonth

sealed interface HomeUiEvent {
    // Famílias: o ViewModel despacha por família e cada uma tem o seu `when` exaustivo (detekt `CyclomaticComplexMethod`).

    /** Gaveta lateral: abrir/fechar, filtros de calendário e atalho de Configurações (spec 06). */
    sealed interface DrawerEvent : HomeUiEvent

    /** Adesivo do dia selecionado (spec 09). */
    sealed interface DayStickerEvent : HomeUiEvent

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

    data object OnMenuClicked : DrawerEvent

    data object OnDrawerDismissed : DrawerEvent

    data class OnCalendarSourceToggled(
        val source: CalendarSource,
    ) : DrawerEvent

    data object OnSettingsClicked : DrawerEvent

    data object OnDayStickerClicked : DayStickerEvent

    data object OnDayStickerPickerDismissed : DayStickerEvent

    /** `null` remove o adesivo do dia selecionado. */
    data class OnDayStickerSelected(
        val emoji: String?,
    ) : DayStickerEvent
}
