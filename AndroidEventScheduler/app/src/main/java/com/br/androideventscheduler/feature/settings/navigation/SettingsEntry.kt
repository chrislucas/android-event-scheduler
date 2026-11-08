package com.br.androideventscheduler.feature.settings.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.br.androideventscheduler.core.navigation.Settings
import com.br.androideventscheduler.feature.settings.presentation.screen.SettingsScreen

/** API pública da feature: registra a rota [Settings] (aberta pela gaveta da Home, spec 06). */
fun EntryProviderScope<NavKey>.settingsEntry(onBack: () -> Unit) {
    entry<Settings> { SettingsScreen(onBack = onBack) }
}
