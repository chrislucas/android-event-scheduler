package com.br.androideventscheduler.feature.detail.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.br.androideventscheduler.core.navigation.ScheduleDetail
import com.br.androideventscheduler.feature.detail.presentation.screen.ScheduleDetailScreen

/**
 * API pública da feature: registra a rota [ScheduleDetail]; [onEdit] recebe o id do agendamento e [onOpenCopy]
 * o id da cópia criada (o host substitui o detalhe atual pelo da cópia).
 */
fun EntryProviderScope<NavKey>.detailEntry(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenCopy: (String) -> Unit,
) {
    entry<ScheduleDetail> { key ->
        ScheduleDetailScreen(scheduleId = key.scheduleId, onBack = onBack, onEdit = onEdit, onOpenCopy = onOpenCopy)
    }
}
