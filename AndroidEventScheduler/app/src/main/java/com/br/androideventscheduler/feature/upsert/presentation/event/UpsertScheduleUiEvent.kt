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
    // Famílias: o ViewModel despacha por família e cada uma tem o seu `when` exaustivo. Assim o `onEvent` fica
    // pequeno (detekt `CyclomaticComplexMethod`) e uma spec nova acrescenta uma família em vez de crescer um `when` único.

    /** Conteúdo do formulário: modo, título, alerta, calendário, local e notas. */
    sealed interface FieldEvent : UpsertScheduleUiEvent

    /** Datas, horas, dia inteiro, atalhos e seletores inline. */
    sealed interface ScheduleTimeEvent : UpsertScheduleUiEvent

    /** Diálogo de repetição (spec 08). */
    sealed interface RecurrenceEvent : UpsertScheduleUiEvent

    data class OnModeSelected(
        val mode: ScheduleType,
    ) : FieldEvent

    data class OnTitleChanged(
        val title: String,
    ) : FieldEvent

    data class OnAllDayToggled(
        val isAllDay: Boolean,
    ) : ScheduleTimeEvent

    data class OnPickerToggled(
        val picker: ActiveInlinePicker,
    ) : ScheduleTimeEvent

    /** [target] é [ActiveInlinePicker.START_DATE] ou [ActiveInlinePicker.END_DATE]. */
    data class OnDateSelected(
        val target: ActiveInlinePicker,
        val date: LocalDate,
    ) : ScheduleTimeEvent

    /** [target] é [ActiveInlinePicker.START_TIME] ou [ActiveInlinePicker.END_TIME]. */
    data class OnTimeChanged(
        val target: ActiveInlinePicker,
        val time: LocalTime,
    ) : ScheduleTimeEvent

    data class OnQuickDateChipClicked(
        val daysOffset: Int,
    ) : ScheduleTimeEvent

    data class OnQuickTimeChipClicked(
        val quickTime: QuickTime,
    ) : ScheduleTimeEvent

    data class OnReminderOffsetSelected(
        val offset: ReminderOffset,
    ) : FieldEvent

    data class OnCalendarSourceSelected(
        val source: CalendarSource,
    ) : FieldEvent

    data class OnLocationChanged(
        val location: String,
    ) : FieldEvent

    data class OnNotesChanged(
        val notes: String,
    ) : FieldEvent

    data object OnRecurrenceRowClicked : RecurrenceEvent

    data object OnRecurrenceDialogDismissed : RecurrenceEvent

    data class OnRecurrencePatternConfirmed(
        val pattern: RecurrencePattern,
    ) : RecurrenceEvent

    data object OnSaveClicked : UpsertScheduleUiEvent

    data object OnCancelClicked : UpsertScheduleUiEvent
}
