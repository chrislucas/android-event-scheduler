package com.br.androideventscheduler.feature.settings.presentation.event

import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.security.domain.AppLockTimeout
import com.br.androideventscheduler.core.security.domain.AuthResult

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

    /** Família de eventos da seção Segurança (spec 16): tratada por um `when` exaustivo próprio. */
    sealed interface AppLockEvent : SettingsUiEvent {
        /** Pedido de ligar/desligar: só grava depois da confirmação da credencial. */
        data class OnToggled(
            val enabled: Boolean,
        ) : AppLockEvent

        data class OnTimeoutSelected(
            val timeout: AppLockTimeout,
        ) : AppLockEvent

        /** Resultado da confirmação pedida por [OnToggled]. */
        data class OnAuthResult(
            val result: AuthResult,
        ) : AppLockEvent

        data object OnOpenSecuritySettingsClicked : AppLockEvent

        /** A tela voltou ao primeiro plano: relê se o aparelho tem bloqueio de tela. */
        data object OnScreenResumed : AppLockEvent
    }
}
