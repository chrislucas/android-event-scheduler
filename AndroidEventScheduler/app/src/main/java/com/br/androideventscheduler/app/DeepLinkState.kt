package com.br.androideventscheduler.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.dsl.module

/**
 * Id do agendamento pendente de abertura vindo de fora do app (toque na notificação). Fica num `single` do
 * Koin porque a Activity grava (`onCreate`/`onNewIntent`) e o `AppNavHost` consome depois da Splash.
 * O id é consumido uma única vez.
 */
class DeepLinkState {
    private val _pendingScheduleId = MutableStateFlow<String?>(null)
    val pendingScheduleId: StateFlow<String?> = _pendingScheduleId.asStateFlow()

    /** Registra o id a abrir; um novo toque substitui um id ainda não consumido. `null`/vazio é ignorado. */
    fun set(scheduleId: String?) {
        if (!scheduleId.isNullOrBlank()) _pendingScheduleId.value = scheduleId
    }

    /** Limpa o id pendente, mas só se ainda for [scheduleId] (não descarta um id mais novo que chegou antes). */
    fun consume(scheduleId: String) {
        _pendingScheduleId.compareAndSet(scheduleId, null)
    }
}

val deepLinkModule =
    module {
        single { DeepLinkState() }
    }
