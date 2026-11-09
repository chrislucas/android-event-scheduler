package com.br.androideventscheduler.feature.backup.presentation.component

import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.feature.backup.domain.model.ImportPreview
import java.time.LocalDate
import java.time.LocalTime

// Sem preview: dados de exemplo usados apenas pelas previews dos componentes e da tela.

/** Pré-visualização de exemplo: [newCount] novos, [existingCount] já existentes e [invalidCount] ignorados. */
internal fun previewImport(
    newCount: Int = 3,
    existingCount: Int = 2,
    invalidCount: Int = 1,
): ImportPreview {
    fun item(id: String) =
        ScheduleItem(
            id = id,
            title = "Agendamento $id",
            type = ScheduleType.EVENT,
            startDate = LocalDate.of(2026, 11, 5),
            startTime = LocalTime.of(10, 0),
        )
    val existing = List(existingCount) { item("existente-$it") }
    return ImportPreview(
        validItems = List(newCount) { item("novo-$it") } + existing,
        newCount = newCount,
        existingCount = existingCount,
        invalidCount = invalidCount,
        existingIds = existing.map { it.id }.toSet(),
    )
}
