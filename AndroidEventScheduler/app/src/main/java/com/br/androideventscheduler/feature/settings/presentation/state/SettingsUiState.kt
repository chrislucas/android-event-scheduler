package com.br.androideventscheduler.feature.settings.presentation.state

import com.br.androideventscheduler.core.preferences.domain.ThemeMode
import com.br.androideventscheduler.core.preferences.domain.UserSettings

internal sealed class SettingsUiState {
    data object Loading : SettingsUiState()

    /**
     * [draft] é o que o usuário está editando; só vai para o armazenamento ao salvar. [themeMode] fica **fora** do
     * rascunho: a escolha do tema é gravada na hora, sem passar pelo Salvar (spec 19).
     */
    data class Content(
        val draft: UserSettings,
        val isSaving: Boolean = false,
        val emailError: Boolean = false,
        val themeMode: ThemeMode = ThemeMode.DEFAULT,
    ) : SettingsUiState()
}
