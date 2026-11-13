package com.br.androideventscheduler.feature.upsert.presentation.effect

import androidx.annotation.StringRes

internal sealed interface UpsertScheduleUiEffect {
    data object NavigateBack : UpsertScheduleUiEffect

    data class ShowMessage(
        @param:StringRes val messageRes: Int,
    ) : UpsertScheduleUiEffect
}
