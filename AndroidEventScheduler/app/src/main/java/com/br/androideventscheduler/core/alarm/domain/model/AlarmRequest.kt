package com.br.androideventscheduler.core.alarm.domain.model

/** Pedido de alarme: só o essencial; o receiver relê o item atualizado do banco ao disparar. */
data class AlarmRequest(
    val scheduleId: String,
    val triggerAtEpochMillis: Long,
    val presentationMode: AlertPresentationMode,
)
