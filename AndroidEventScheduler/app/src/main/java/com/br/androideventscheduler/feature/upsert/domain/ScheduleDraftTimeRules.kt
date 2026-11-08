package com.br.androideventscheduler.feature.upsert.domain

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** Datas e horas do rascunho em edição. Quando [isAllDay], só as datas importam (as horas ficam ocultas). */
data class ScheduleDraftTimes(
    val startDate: LocalDate,
    val startTime: LocalTime,
    val endDate: LocalDate,
    val endTime: LocalTime,
    val isAllDay: Boolean = false,
) {
    val start: LocalDateTime get() = LocalDateTime.of(startDate, startTime)
    val end: LocalDateTime get() = LocalDateTime.of(endDate, endTime)
}

/**
 * Regras puras de data e hora do formulário (sem I/O, usadas só pela tela de criar/editar):
 * duração preservada ao mover o início, fim nunca antes do início, atalhos rápidos e troca de modo.
 */
object ScheduleDraftTimeRules {
    private const val DEFAULT_EVENT_DURATION_HOURS = 1L

    /** Move o início para [newStart] mantendo a duração; o fim acompanha (inclusive virando o dia). */
    fun withNewStart(
        draft: ScheduleDraftTimes,
        newStart: LocalDateTime,
    ): ScheduleDraftTimes {
        val duration = Duration.between(draft.start, draft.end).coerceAtLeast(Duration.ZERO)
        val newEnd = newStart.plus(duration)
        return draft.copy(
            startDate = newStart.toLocalDate(),
            startTime = newStart.toLocalTime(),
            endDate = newEnd.toLocalDate(),
            endTime = newEnd.toLocalTime(),
        )
    }

    /** Altera o fim para [newEnd]; se ficar antes do início, o fim passa a ser o próprio início. */
    fun withNewEnd(
        draft: ScheduleDraftTimes,
        newEnd: LocalDateTime,
    ): ScheduleDraftTimes {
        val endsBeforeStart =
            if (draft.isAllDay) {
                newEnd.toLocalDate().isBefore(draft.startDate)
            } else {
                newEnd.isBefore(draft.start)
            }
        val clampedEnd = if (endsBeforeStart) draft.start else newEnd
        return draft.copy(endDate = clampedEnd.toLocalDate(), endTime = clampedEnd.toLocalTime())
    }

    /** Evento → Lembrete: o fim colapsa no início e "dia inteiro" é descartado. */
    fun switchToReminder(draft: ScheduleDraftTimes): ScheduleDraftTimes =
        draft.copy(endDate = draft.startDate, endTime = draft.startTime, isAllDay = false)

    /** Lembrete → Evento: o fim passa a ser o início + 1 h. */
    fun switchToEvent(draft: ScheduleDraftTimes): ScheduleDraftTimes {
        val newEnd = draft.start.plusHours(DEFAULT_EVENT_DURATION_HOURS)
        return draft.copy(endDate = newEnd.toLocalDate(), endTime = newEnd.toLocalTime())
    }

    /** Atalho de data: hoje + [daysOffset] dias. */
    fun quickDate(
        daysOffset: Int,
        today: LocalDate,
    ): LocalDate = today.plusDays(daysOffset.toLong())

    /** Atalho "Daqui a 1 hora": agora + 1 h, sem segundos (pode cair no dia seguinte). */
    fun inOneHour(now: LocalDateTime): LocalDateTime = now.plusHours(1).truncatedTo(ChronoUnit.MINUTES)

    /** Início padrão de um novo agendamento: a próxima hora cheia. */
    fun nextFullHour(now: LocalDateTime): LocalDateTime = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
}
