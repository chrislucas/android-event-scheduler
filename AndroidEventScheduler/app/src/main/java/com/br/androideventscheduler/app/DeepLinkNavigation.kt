package com.br.androideventscheduler.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.br.androideventscheduler.core.navigation.Home
import com.br.androideventscheduler.core.navigation.ScheduleDetail

// Sem preview: efeito de navegação sem UI própria.

/**
 * Abre o detalhe do agendamento pendente do deep link: se o topo do back stack é a [Home] (ou seja, depois da
 * Splash), empilha `ScheduleDetail(id)` por cima. Devolve `true` quando o id foi usado (e deve ser consumido).
 */
internal fun MutableList<NavKey>.openPendingScheduleDetail(pendingScheduleId: String?): Boolean {
    if (pendingScheduleId == null || lastOrNull() != Home) return false
    add(ScheduleDetail(pendingScheduleId))
    return true
}

/** Observa o [DeepLinkState] e aplica [openPendingScheduleDetail] sempre que o id ou o topo da pilha mudam. */
@Composable
internal fun DeepLinkEffect(
    backStack: MutableList<NavKey>,
    deepLinkState: DeepLinkState,
) {
    val pendingScheduleId by deepLinkState.pendingScheduleId.collectAsStateWithLifecycle()
    val currentTop = backStack.lastOrNull()
    LaunchedEffect(pendingScheduleId, currentTop) {
        val scheduleId = pendingScheduleId
        if (scheduleId != null && backStack.openPendingScheduleDetail(scheduleId)) {
            deepLinkState.consume(scheduleId)
        }
    }
}
