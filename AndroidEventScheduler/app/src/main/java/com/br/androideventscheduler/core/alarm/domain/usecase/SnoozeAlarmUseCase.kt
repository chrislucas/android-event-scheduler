package com.br.androideventscheduler.core.alarm.domain.usecase

import com.br.androideventscheduler.core.alarm.domain.model.AlarmRequest
import com.br.androideventscheduler.core.alarm.domain.model.AlertPresentationMode
import com.br.androideventscheduler.core.alarm.domain.scheduler.AlarmScheduler
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleNotFoundException
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import kotlinx.coroutines.CancellationException
import java.time.Clock

/**
 * Adia o alerta de um item: agenda um novo disparo para `agora + N minutos`, **sem alterar** o item
 * (data, hora e recorrência continuam os originais). O [AlarmScheduler] mantém no máximo um alarme por item,
 * então o novo disparo substitui o pendente (a próxima ocorrência da série, reagendada no disparo anterior).
 * Quando o alarme adiado dispara, o receiver chama o [ScheduleNextAlarmUseCase] e restaura a série.
 */
class SnoozeAlarmUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock,
) {
    /** Devolve o instante do novo disparo (epoch millis) ou a falha (validação, item inexistente ou agendamento). */
    suspend operator fun invoke(
        scheduleId: String,
        snoozeMinutes: Int,
    ): Result<Long> =
        runCatching {
            require(snoozeMinutes > 0) { "snoozeMinutes must be positive" }
            val item = scheduleRepository.getById(scheduleId) ?: throw ScheduleNotFoundException(scheduleId)
            val snoozedTriggerMillis = clock.millis() + snoozeMinutes * MILLIS_PER_MINUTE
            alarmScheduler.schedule(
                AlarmRequest(
                    scheduleId = item.id,
                    triggerAtEpochMillis = snoozedTriggerMillis,
                    presentationMode = AlertPresentationMode.forType(item.type),
                ),
            )
            snoozedTriggerMillis
        }.onFailure { failure -> if (failure is CancellationException) throw failure }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
