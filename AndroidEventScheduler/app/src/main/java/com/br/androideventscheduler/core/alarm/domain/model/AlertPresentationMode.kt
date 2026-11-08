package com.br.androideventscheduler.core.alarm.domain.model

import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType

/** Como o alerta de um agendamento é apresentado quando o alarme dispara. */
enum class AlertPresentationMode {
    /** Apenas uma notificação de alta prioridade (Lembrete). */
    NOTIFICATION_ONLY,

    /** Alerta em tela cheia (Evento), degradando para notificação quando não for possível. */
    FULL_SCREEN_EVENT,
    ;

    companion object {
        fun forType(type: ScheduleType): AlertPresentationMode =
            when (type) {
                ScheduleType.EVENT -> FULL_SCREEN_EVENT
                ScheduleType.REMINDER -> NOTIFICATION_ONLY
            }
    }
}
