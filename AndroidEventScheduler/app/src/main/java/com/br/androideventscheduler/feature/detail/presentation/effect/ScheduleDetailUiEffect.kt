package com.br.androideventscheduler.feature.detail.presentation.effect

import androidx.annotation.StringRes

internal sealed interface ScheduleDetailUiEffect {
    data object NavigateBack : ScheduleDetailUiEffect

    data class NavigateToEdit(
        val scheduleId: String,
    ) : ScheduleDetailUiEffect

    data class ShowMessage(
        @StringRes val messageRes: Int,
    ) : ScheduleDetailUiEffect
}
