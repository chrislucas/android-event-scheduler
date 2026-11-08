package com.br.androideventscheduler.feature.backup.domain.model

import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem

/**
 * Resultado da leitura e validação **completa** de um arquivo, antes de gravar qualquer coisa (R5).
 * [validItems] só contém itens que passaram na validação; [invalidCount] conta os ignorados.
 */
data class ImportPreview(
    val validItems: List<ScheduleItem>,
    val newCount: Int,
    val existingCount: Int,
    val invalidCount: Int,
    val existingIds: Set<String>,
) {
    /** Total de itens no arquivo (válidos + ignorados por dados inválidos). */
    val totalInFile: Int get() = validItems.size + invalidCount
}
