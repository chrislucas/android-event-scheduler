package com.br.androideventscheduler.core.schedule.domain.model

/** Lançada quando uma operação referencia um agendamento que não existe mais. */
class ScheduleNotFoundException(
    val scheduleId: String,
) : NoSuchElementException("Schedule not found: $scheduleId")
