package com.br.androideventscheduler.feature.settings.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.br.androideventscheduler.core.navigation.Settings
import com.br.androideventscheduler.feature.settings.presentation.screen.SettingsScreen

/**
 * API pública da feature: registra a rota [Settings] (aberta pela gaveta da Home, spec 06). [onOpenBackup] abre a
 * tela de importar/exportar agenda (spec 17); a feature não conhece a rota, o host navega.
 */
fun EntryProviderScope<NavKey>.settingsEntry(
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
) {
    entry<Settings> { SettingsScreen(onBack = onBack, onOpenBackup = onOpenBackup) }
}
