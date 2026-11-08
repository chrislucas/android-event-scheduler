package com.br.androideventscheduler.feature.detail.presentation.event

internal sealed interface ScheduleDetailUiEvent {
    data object OnBackClicked : ScheduleDetailUiEvent

    data object OnEditClicked : ScheduleDetailUiEvent

    data object OnDuplicateClicked : ScheduleDetailUiEvent

    data object OnDeleteClicked : ScheduleDetailUiEvent

    data object OnConfirmDeleteClicked : ScheduleDetailUiEvent

    data object OnDismissDeleteDialog : ScheduleDetailUiEvent

    data object OnShareClicked : ScheduleDetailUiEvent

    /** Toque na ação "Abrir" do Snackbar de cópia criada. */
    data class OnOpenCopyClicked(
        val copyId: String,
    ) : ScheduleDetailUiEvent
}
