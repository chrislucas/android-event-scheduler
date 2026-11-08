package com.br.androideventscheduler.core.alarm.domain.usecase

import com.br.androideventscheduler.core.alarm.domain.model.AlarmRequest
import com.br.androideventscheduler.core.alarm.domain.model.AlertPresentationMode
import com.br.androideventscheduler.core.alarm.domain.scheduler.AlarmScheduler
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.recurrence.RecurrenceEngine
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Agenda o **próximo** alarme de um item: (início da ocorrência) − antecedência, estritamente depois de agora.
 * Cancela o alarme do item quando não há antecedência (`NONE`) ou ocorrência futura. Compartilhado por
 * Upsert, Duplicar, pelo receiver (reagenda a série) e pela restauração pós-boot.
 */
class ScheduleNextAlarmUseCase(
    private val recurrenceEngine: RecurrenceEngine,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock,
) {
    /** Devolve o instante agendado (epoch millis) ou `null` se o alarme foi cancelado. */
    operator fun invoke(item: ScheduleItem): Long? {
        val offsetMinutes = item.reminderOffset.minutes ?: return cancelAndReturnNull(item.id)
        val nowMillis = clock.millis()
        // Um dia antes de hoje cobre o fuso/virada de dia; a sequência é crescente, então firstOrNull é correto.
        val searchStartDate = LocalDate.now(clock).minusDays(1)
        val nextTriggerMillis =
            recurrenceEngine
                .occurrenceDates(item, onOrAfter = searchStartDate)
                .map { occurrenceDate -> triggerMillis(item, occurrenceDate, offsetMinutes) }
                .firstOrNull { triggerMillis -> triggerMillis > nowMillis }
                ?: return cancelAndReturnNull(item.id)

        alarmScheduler.schedule(
            AlarmRequest(
                scheduleId = item.id,
                triggerAtEpochMillis = nextTriggerMillis,
                presentationMode = AlertPresentationMode.forType(item.type),
            ),
        )
        return nextTriggerMillis
    }

    private fun triggerMillis(
        item: ScheduleItem,
        occurrenceDate: LocalDate,
        offsetMinutes: Int,
    ): Long {
        val occurrenceStartTime = if (item.isAllDay) ALL_DAY_ALERT_TIME else item.startTime
        return LocalDateTime
            .of(occurrenceDate, occurrenceStartTime)
            .atZone(clock.zone)
            .toInstant()
            .toEpochMilli() - offsetMinutes * MILLIS_PER_MINUTE
    }

    private fun cancelAndReturnNull(scheduleId: String): Long? {
        alarmScheduler.cancel(scheduleId)
        return null
    }

    companion object {
        /** Horário local em que um item de dia inteiro é "iniciado" para efeito de alerta. */
        val ALL_DAY_ALERT_TIME: LocalTime = LocalTime.of(9, 0)

        private const val MILLIS_PER_MINUTE = 60_000L
    }
}
