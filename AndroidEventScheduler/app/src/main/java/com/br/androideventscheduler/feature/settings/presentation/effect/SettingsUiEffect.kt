package com.br.androideventscheduler.feature.settings.presentation.effect

import androidx.annotation.StringRes

internal sealed interface SettingsUiEffect {
    data object NavigateBack : SettingsUiEffect

    /** Confirmação que sobrevive à saída da tela (Toast). */
    data object ShowSavedConfirmation : SettingsUiEffect

    /** Erro exibido na própria tela (Snackbar). */
    data class ShowError(
        @StringRes val messageRes: Int,
    ) : SettingsUiEffect
}
