package com.br.androideventscheduler.feature.settings.presentation.event

import com.br.androideventscheduler.core.preferences.domain.ThemeMode
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset

internal sealed interface SettingsUiEvent {
    data class OnEmailChanged(
        val email: String,
    ) : SettingsUiEvent

    data class OnReminderSelected(
        val offset: ReminderOffset,
    ) : SettingsUiEvent

    data class OnSnoozeSelected(
        val minutes: Int,
    ) : SettingsUiEvent

    data class OnFullScreenToggled(
        val enabled: Boolean,
    ) : SettingsUiEvent

    data object OnSaveClicked : SettingsUiEvent

    data object OnBackClicked : SettingsUiEvent

    /** Abre a tela de importar/exportar agenda (spec 17). */
    data object OnBackupClicked : SettingsUiEvent

    /** Família da seção Aparência (spec 19): grava na hora, fora do rascunho e do botão Salvar. */
    sealed interface AppearanceEvent : SettingsUiEvent {
        data class OnThemeModeSelected(
            val mode: ThemeMode,
        ) : AppearanceEvent
    }
}
