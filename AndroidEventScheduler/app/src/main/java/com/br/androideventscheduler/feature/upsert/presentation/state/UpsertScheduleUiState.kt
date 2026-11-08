package com.br.androideventscheduler.feature.upsert.presentation.state

import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.EventColorOption
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import java.time.LocalDate
import java.time.LocalTime

internal sealed class UpsertScheduleUiState {
    data object Loading : UpsertScheduleUiState()

    /**
     * Rascunho do formulário. [isRecurrenceDialogVisible], [isStickerPickerVisible] e [isColorPickerVisible]
     * controlam os diálogos de repetição, adesivo e cor. [colorHex] fora do catálogo é exibido como a cor padrão.
     */
    data class Content(
        val scheduleId: String? = null,
        val mode: ScheduleType = ScheduleType.REMINDER,
        val title: String = "",
        val isAllDay: Boolean = false,
        val startDate: LocalDate,
        val startTime: LocalTime,
        val endDate: LocalDate,
        val endTime: LocalTime,
        val activePicker: ActiveInlinePicker = ActiveInlinePicker.NONE,
        val reminderOffset: ReminderOffset,
        val calendarSource: CalendarSource,
        /** E-mail da conta ativa: rótulo do calendário [CalendarSource.PERSONAL_ACCOUNT]. */
        val accountEmail: String = "",
        val recurrencePattern: RecurrencePattern = RecurrencePattern.None,
        val isRecurrenceDialogVisible: Boolean = false,
        val colorHex: Long = EventColorOption.LIGHT_BLUE.hex,
        val stickerEmoji: String? = null,
        val isStickerPickerVisible: Boolean = false,
        val isColorPickerVisible: Boolean = false,
        val location: String = "",
        val notes: String = "",
        val isSaving: Boolean = false,
        val titleError: Boolean = false,
    ) : UpsertScheduleUiState() {
        val isEditing: Boolean get() = scheduleId != null
    }
}
