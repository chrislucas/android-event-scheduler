package com.br.androideventscheduler.feature.search.domain.model

import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType

/** Filtro por tipo da pesquisa. [targetType] `null` significa "sem filtro". O rótulo pt-BR é da apresentação. */
internal enum class SearchFilter(
    val targetType: ScheduleType?,
) {
    ALL(targetType = null),
    EVENTS(targetType = ScheduleType.EVENT),
    REMINDERS(targetType = ScheduleType.REMINDER),
}
