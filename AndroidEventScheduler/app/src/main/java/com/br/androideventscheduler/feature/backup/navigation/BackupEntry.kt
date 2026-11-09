package com.br.androideventscheduler.feature.backup.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.br.androideventscheduler.core.navigation.AgendaBackup
import com.br.androideventscheduler.feature.backup.presentation.screen.BackupScreen

/**
 * API pública da feature: registra a rota [AgendaBackup]. Com `pendingImportUri` (arquivo recebido de outro app), a
 * tela abre direto na pré-visualização da importação.
 */
fun EntryProviderScope<NavKey>.backupEntry(onBack: () -> Unit) {
    entry<AgendaBackup> { key -> BackupScreen(pendingImportUri = key.pendingImportUri, onBack = onBack) }
}
