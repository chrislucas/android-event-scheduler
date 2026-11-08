package com.br.androideventscheduler.feature.search.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.br.androideventscheduler.core.navigation.Search
import com.br.androideventscheduler.feature.search.presentation.screen.SearchScreen

/**
 * API pública da feature: registra a rota [Search] (aberta pela lupa da Home, spec 05).
 * [onOpenDetail] recebe o id do agendamento tocado.
 */
fun EntryProviderScope<NavKey>.searchEntry(
    onBack: () -> Unit,
    onOpenDetail: (scheduleId: String) -> Unit,
) {
    entry<Search> { SearchScreen(onBack = onBack, onOpenDetail = onOpenDetail) }
}
