package com.br.androideventscheduler.core.schedule.domain.recurrence

import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleOccurrence
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Calcula as ocorrências de um item. Puro e determinístico: não lê relógio.
 *
 * A série começa em `item.startDate`; o limite de ocorrências é contado a partir da **primeira**
 * ocorrência, nunca a partir da data consultada.
 */
class RecurrenceEngine {
    /** Datas de início das ocorrências a partir de [onOrAfter] (inclusive), em ordem crescente. Preguiçosa. */
    fun occurrenceDates(
        item: ScheduleItem,
        onOrAfter: LocalDate = item.startDate,
    ): Sequence<LocalDate> = seriesDates(item).dropWhile { occurrenceStart -> occurrenceStart < onOrAfter }

    /** Ocorrências cujo intervalo `[início, fim]` intersecta `[from, to]`. */
    fun expand(
        item: ScheduleItem,
        from: LocalDate,
        to: LocalDate,
    ): List<ScheduleOccurrence> {
        val durationDays = ChronoUnit.DAYS.between(item.startDate, item.endDate)
        return seriesDates(item)
            .takeWhile { occurrenceStart -> occurrenceStart <= to }
            .map { occurrenceStart -> ScheduleOccurrence(item, occurrenceStart, occurrenceStart.plusDays(durationDays)) }
            .filter { occurrence -> occurrence.endDate >= from }
            .toList()
    }

    private fun seriesDates(item: ScheduleItem): Sequence<LocalDate> {
        val candidateDates: Sequence<LocalDate> =
            when (val pattern = item.recurrencePattern) {
                RecurrencePattern.None -> {
                    sequenceOf(item.startDate)
                }

                is RecurrencePattern.Daily -> {
                    consecutiveDaysFrom(item.startDate)
                }

                is RecurrencePattern.Weekly -> {
                    consecutiveDaysFrom(item.startDate).filter { candidate -> candidate.dayOfWeek in pattern.daysOfWeek }
                }
            }
        val occurrenceLimit = item.recurrencePattern.occurrenceLimit
        return if (occurrenceLimit == null) candidateDates else candidateDates.take(occurrenceLimit)
    }

    private fun consecutiveDaysFrom(firstDay: LocalDate): Sequence<LocalDate> = generateSequence(firstDay) { day -> day.plusDays(1) }
}
