package com.br.androideventscheduler.core.alarm.domain.scheduler

import com.br.androideventscheduler.core.alarm.domain.model.AlarmRequest

/** Agenda e cancela alarmes. No máximo um alarme pendente por agendamento: agendar substitui o anterior. */
interface AlarmScheduler {
    fun schedule(request: AlarmRequest)

    /** Idempotente: cancelar um alarme inexistente não faz nada. */
    fun cancel(scheduleId: String)
}
