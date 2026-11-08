package com.br.androideventscheduler.core.alarm.domain.model

/**
 * Resultado de reagendar todos os alarmes.
 *
 * @property scheduled itens que ficaram com um alarme futuro agendado.
 * @property skipped itens sem alarme (único no passado, sem antecedência ou série encerrada).
 * @property failed itens cujo reagendamento lançou exceção (os demais seguiram normalmente).
 */
data class RescheduleSummary(
    val scheduled: Int,
    val skipped: Int,
    val failed: Int,
)
