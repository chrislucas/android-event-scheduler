package com.br.androideventscheduler.feature.home.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.br.androideventscheduler.core.navigation.Home
import com.br.androideventscheduler.feature.home.presentation.screen.HomeScreen

/**
 * API pública da feature: registra a rota [Home]. [onNavigateToUpsert] recebe o dia selecionado
 * (`LocalDate.toEpochDay()`) como data inicial da criação; [onNavigateToDetail] recebe o id do agendamento.
 * [onNavigateToSettings] é o atalho "Configurações" da gaveta lateral.
 */
fun EntryProviderScope<NavKey>.homeEntry(
    onNavigateToSearch: () -> Unit,
    onNavigateToUpsert: (initialDateEpochDay: Long) -> Unit,
    onNavigateToDetail: (scheduleId: String) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    entry<Home> {
        HomeScreen(
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToUpsert = onNavigateToUpsert,
            onNavigateToDetail = onNavigateToDetail,
            onNavigateToSettings = onNavigateToSettings,
        )
    }
}
