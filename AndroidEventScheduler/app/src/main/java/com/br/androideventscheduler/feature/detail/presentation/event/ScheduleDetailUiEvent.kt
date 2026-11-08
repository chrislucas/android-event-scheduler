package com.br.androideventscheduler.feature.detail.presentation.event

internal sealed interface ScheduleDetailUiEvent {
    data object OnBackClicked : ScheduleDetailUiEvent

    data object OnEditClicked : ScheduleDetailUiEvent
}
