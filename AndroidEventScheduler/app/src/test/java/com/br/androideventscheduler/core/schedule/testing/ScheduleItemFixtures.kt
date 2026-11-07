package com.br.androideventscheduler.core.schedule.testing

import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import java.time.LocalDate
import java.time.LocalTime

/** Fábrica de itens para testes; parâmetros nomeados deixam explícito o que cada teste varia. */
fun scheduleItem(
    id: String = "item-1",
    title: String = "Reunião",
    type: ScheduleType = ScheduleType.EVENT,
    startDate: LocalDate = LocalDate.of(2026, 11, 5),
    endDate: LocalDate = startDate,
    startTime: LocalTime = LocalTime.of(10, 0),
    endTime: LocalTime? = null,
    isAllDay: Boolean = false,
    location: String = "",
    notes: String = "",
    recurrencePattern: RecurrencePattern = RecurrencePattern.None,
) = ScheduleItem(
    id = id,
    title = title,
    type = type,
    startDate = startDate,
    endDate = endDate,
    startTime = startTime,
    endTime = endTime,
    isAllDay = isAllDay,
    location = location,
    notes = notes,
    recurrencePattern = recurrencePattern,
)
