package com.br.androideventscheduler.core.alarm.domain.usecase

import com.br.androideventscheduler.core.alarm.domain.model.RescheduleSummary
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import kotlinx.coroutines.CancellationException

/**
 * Reagenda o próximo alarme de **todos** os itens (após boot, atualização do app, mudança de fuso/hora
 * ou da permissão de alarme exato). Não altera o banco: a próxima ocorrência vem do `RecurrenceEngine`
 * dentro de [ScheduleNextAlarmUseCase]. Alertas perdidos enquanto o aparelho estava desligado não são
 * disparados retroativamente. Idempotente: agendar substitui o alarme pendente do mesmo item.
 */
class RescheduleAllAlarmsUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val scheduleNextAlarm: ScheduleNextAlarmUseCase,
) {
    /** Falha (`Result.failure`) apenas se a leitura dos itens falhar; falhas por item vão em `failed`. */
    suspend operator fun invoke(): Result<RescheduleSummary> =
        try {
            Result.success(rescheduleItems())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (
            @Suppress("TooGenericExceptionCaught") failure: Exception,
        ) {
            Result.failure(failure)
        }

    private suspend fun rescheduleItems(): RescheduleSummary {
        var scheduledCount = 0
        var skippedCount = 0
        var failedCount = 0
        for (item in scheduleRepository.getAll()) {
            // scheduleNextAlarm não é suspend: runCatching aqui não engole cancelamento de corrotina.
            runCatching { scheduleNextAlarm(item) }
                .onSuccess { triggerMillis -> if (triggerMillis != null) scheduledCount++ else skippedCount++ }
                .onFailure { failedCount++ }
        }
        return RescheduleSummary(scheduled = scheduledCount, skipped = skippedCount, failed = failedCount)
    }
}
