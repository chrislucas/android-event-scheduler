package com.br.androideventscheduler.feature.detail.domain.usecase

import com.br.androideventscheduler.core.alarm.domain.usecase.ScheduleNextAlarmUseCase
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleNotFoundException
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import java.util.UUID

/**
 * Cria uma cópia de um agendamento (novo `id`, título com sufixo) e agenda o alarme dela.
 * Orquestra banco + alarme: a falha do alarme **não** desfaz a cópia já gravada; a falha da gravação
 * impede qualquer agendamento. O sufixo vem da apresentação, pois o domínio não conhece resources.
 */
class DuplicateScheduleUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val scheduleNextAlarm: ScheduleNextAlarmUseCase,
    private val idProvider: () -> String = { UUID.randomUUID().toString() },
) {
    suspend operator fun invoke(
        sourceId: String,
        titleSuffix: String,
    ): Result<ScheduleItem> =
        runCatching {
            val original = scheduleRepository.getById(sourceId) ?: throw ScheduleNotFoundException(sourceId)
            val copy = original.copy(id = idProvider(), title = original.title + titleSuffix)
            scheduleRepository.upsert(copy)
            runCatching { scheduleNextAlarm(copy) }
            copy
        }
}
