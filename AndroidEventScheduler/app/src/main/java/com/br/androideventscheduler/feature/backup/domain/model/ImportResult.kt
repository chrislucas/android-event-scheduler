package com.br.androideventscheduler.feature.backup.domain.model

/**
 * Resumo de uma importação concluída. [skippedCount] são itens que já existiam e foram mantidos
 * ([ImportStrategy.KEEP_EXISTING]); [alarmFailures] são alarmes que não puderam ser agendados (a importação vale).
 */
data class ImportResult(
    val importedCount: Int,
    val skippedCount: Int,
    val alarmFailures: Int,
)
