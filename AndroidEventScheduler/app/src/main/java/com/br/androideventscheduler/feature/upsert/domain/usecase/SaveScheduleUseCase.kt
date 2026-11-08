package com.br.androideventscheduler.feature.upsert.domain.usecase

import com.br.androideventscheduler.core.alarm.domain.usecase.ScheduleNextAlarmUseCase
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.feature.upsert.domain.model.SaveScheduleError
import java.time.LocalDateTime

/**
 * Valida, persiste e agenda o alarme de um agendamento (criação ou edição).
 *
 * Por que é UseCase: orquestra o repositório e o alarme (duas fontes que precisam ficar consistentes) e
 * concentra a validação. Falha **apenas** do alarme não derruba a gravação.
 */
class SaveScheduleUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val scheduleNextAlarm: ScheduleNextAlarmUseCase,
) {
    /** Sucesso devolve o item normalizado (título com `trim`); falha traz [SaveScheduleError] ou o erro de I/O. */
    suspend operator fun invoke(item: ScheduleItem): Result<ScheduleItem> =
        runCatching {
            val normalizedItem = item.copy(title = item.title.trim())
            if (normalizedItem.title.isBlank()) throw SaveScheduleError.BlankTitle
            if (endsBeforeStart(normalizedItem)) throw SaveScheduleError.EndBeforeStart

            scheduleRepository.upsert(normalizedItem)
            // O item já está gravado: uma falha ao agendar o alarme não pode invalidar o salvamento.
            runCatching { scheduleNextAlarm(normalizedItem) }
            normalizedItem
        }

    /** `ScheduleItem` já impede data final anterior à inicial. Dia inteiro compara só datas; lembretes (sem `endTime`) nunca terminam antes de começar. */
    private fun endsBeforeStart(item: ScheduleItem): Boolean {
        if (item.isAllDay) return false
        val endTime = item.endTime ?: return false
        val start = LocalDateTime.of(item.startDate, item.startTime)
        val end = LocalDateTime.of(item.endDate, endTime)
        return end.isBefore(start)
    }
}
