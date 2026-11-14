package com.br.androideventscheduler.feature.detail.domain.usecase

import com.br.androideventscheduler.core.alarm.domain.scheduler.AlarmScheduler
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository

/**
 * Remove um agendamento e cancela o alarme pendente. A ordem é regra de negócio: **banco primeiro**.
 * Se a remoção falhar, nada mais acontece (o alarme continua válido para um item que ainda existe);
 * se só o cancelamento falhar, o item continua excluído (o receiver ignora alarmes de itens inexistentes).
 */
class DeleteScheduleUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val alarmScheduler: AlarmScheduler,
) {
    suspend operator fun invoke(scheduleId: String): Result<Unit> =
        runCatching {
            scheduleRepository.delete(scheduleId)
            // Falha no cancelamento não desfaz a exclusão (ver KDoc): o resultado é descartado de propósito.
            runCatching { alarmScheduler.cancel(scheduleId) }
        }
}
