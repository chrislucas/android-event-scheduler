package com.br.androideventscheduler.feature.upsert.presentation.effect

import androidx.annotation.StringRes

internal sealed interface UpsertScheduleUiEffect {
    data object NavigateBack : UpsertScheduleUiEffect

    data class ShowMessage(
        @StringRes val messageRes: Int,
    ) : UpsertScheduleUiEffect
}
