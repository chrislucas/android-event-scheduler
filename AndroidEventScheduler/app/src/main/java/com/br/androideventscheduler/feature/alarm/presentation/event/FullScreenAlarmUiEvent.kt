package com.br.androideventscheduler.feature.alarm.presentation.event

internal sealed interface FullScreenAlarmUiEvent {
    data class OnSnoozeDurationSelected(
        val minutes: Int,
    ) : FullScreenAlarmUiEvent

    data object OnSnoozeClicked : FullScreenAlarmUiEvent

    data object OnDismissClicked : FullScreenAlarmUiEvent

    data object OnBackPressed : FullScreenAlarmUiEvent

    /** Um novo alarme chegou com a tela já aberta (`onNewIntent`): recarrega o item. */
    data object OnReloadRequested : FullScreenAlarmUiEvent
}
