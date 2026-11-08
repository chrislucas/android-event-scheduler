package com.br.androideventscheduler.feature.alarm.presentation.effect

internal sealed interface FullScreenAlarmUiEffect {
    /** Fecha a tela; [cancelNotification] também remove a notificação ativa do item. */
    data class CloseScreen(
        val cancelNotification: Boolean,
    ) : FullScreenAlarmUiEffect
}
