package com.br.androideventscheduler.feature.settings.presentation.effect

import androidx.annotation.StringRes

internal sealed interface SettingsUiEffect {
    data object NavigateBack : SettingsUiEffect

    /** Abre a tela de importar/exportar agenda (spec 17); a navegação é do host. */
    data object NavigateToBackup : SettingsUiEffect

    /** Confirmação que sobrevive à saída da tela (Toast). */
    data object ShowSavedConfirmation : SettingsUiEffect

    /** Erro exibido na própria tela (Snackbar). */
    data class ShowError(
        @param:StringRes val messageRes: Int,
    ) : SettingsUiEffect
}
