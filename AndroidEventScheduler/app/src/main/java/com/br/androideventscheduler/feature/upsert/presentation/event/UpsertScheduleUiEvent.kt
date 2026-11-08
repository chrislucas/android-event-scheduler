package com.br.androideventscheduler.feature.upsert.presentation.event

import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.feature.upsert.presentation.state.ActiveInlinePicker
import java.time.LocalDate
import java.time.LocalTime

/** Atalho de hora do seletor inline. */
internal sealed interface QuickTime {
    /** Agora + 1 h (pode mudar a data para o dia seguinte). */
    data object InOneHour : QuickTime

    data class At(
        val time: LocalTime,
    ) : QuickTime
}

internal sealed interface UpsertScheduleUiEvent {
    data class OnModeSelected(
        val mode: ScheduleType,
    ) : UpsertScheduleUiEvent

    data class OnTitleChanged(
        val title: String,
    ) : UpsertScheduleUiEvent

    data class OnAllDayToggled(
        val isAllDay: Boolean,
    ) : UpsertScheduleUiEvent

    data class OnPickerToggled(
        val picker: ActiveInlinePicker,
    ) : UpsertScheduleUiEvent

    /** [target] é [ActiveInlinePicker.START_DATE] ou [ActiveInlinePicker.END_DATE]. */
    data class OnDateSelected(
        val target: ActiveInlinePicker,
        val date: LocalDate,
    ) : UpsertScheduleUiEvent

    /** [target] é [ActiveInlinePicker.START_TIME] ou [ActiveInlinePicker.END_TIME]. */
    data class OnTimeChanged(
        val target: ActiveInlinePicker,
        val time: LocalTime,
    ) : UpsertScheduleUiEvent

    data class OnQuickDateChipClicked(
        val daysOffset: Int,
    ) : UpsertScheduleUiEvent

    data class OnQuickTimeChipClicked(
        val quickTime: QuickTime,
    ) : UpsertScheduleUiEvent

    data class OnReminderOffsetSelected(
        val offset: ReminderOffset,
    ) : UpsertScheduleUiEvent

    data class OnCalendarSourceSelected(
        val source: CalendarSource,
    ) : UpsertScheduleUiEvent

    data class OnLocationChanged(
        val location: String,
    ) : UpsertScheduleUiEvent

    data class OnNotesChanged(
        val notes: String,
    ) : UpsertScheduleUiEvent

    data object OnRecurrenceRowClicked : UpsertScheduleUiEvent

    data object OnRecurrenceDialogDismissed : UpsertScheduleUiEvent

    data class OnRecurrencePatternConfirmed(
        val pattern: RecurrencePattern,
    ) : UpsertScheduleUiEvent

    data object OnSaveClicked : UpsertScheduleUiEvent

    data object OnCancelClicked : UpsertScheduleUiEvent
}
