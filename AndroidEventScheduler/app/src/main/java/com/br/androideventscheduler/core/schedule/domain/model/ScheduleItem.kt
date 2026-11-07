package com.br.androideventscheduler.core.schedule.domain.model

import java.time.LocalDate
import java.time.LocalTime

/** Agendamento (evento ou lembrete). Para itens recorrentes, [startDate] é o início da série. */
data class ScheduleItem(
    val id: String,
    val title: String,
    val type: ScheduleType,
    val startDate: LocalDate,
    val endDate: LocalDate = startDate,
    val startTime: LocalTime,
    val endTime: LocalTime? = null,
    val isAllDay: Boolean = false,
    val location: String = "",
    val notes: String = "",
    val colorHex: Long = EventColorOption.LIGHT_BLUE.hex,
    val calendarSource: CalendarSource = CalendarSource.defaultFor(type),
    val reminderOffset: ReminderOffset = ReminderOffset.DEFAULT,
    val recurrencePattern: RecurrencePattern = RecurrencePattern.None,
    val stickerEmoji: String? = null,
) {
    init {
        require(!endDate.isBefore(startDate)) { "endDate ($endDate) must not be before startDate ($startDate)" }
    }
}

/** Uma ocorrência concreta de um item (a própria data, para itens sem recorrência). */
data class ScheduleOccurrence(
    val item: ScheduleItem,
    val startDate: LocalDate,
    val endDate: LocalDate,
)
