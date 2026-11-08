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

    /** Snackbar "Cópia criada" com a ação "Abrir", que dispara `OnOpenCopyClicked(copyId)`. */
    data class ShowCopyCreated(
        val copyId: String,
    ) : ScheduleDetailUiEffect

    /** Abre o seletor de compartilhamento do sistema; a tela dispara o Intent (precisa da `Activity`). */
    data class ShareText(
        val subject: String,
        val body: String,
    ) : ScheduleDetailUiEffect

    /** Substitui o detalhe atual pelo da cópia. */
    data class NavigateToCopy(
        val copyId: String,
    ) : ScheduleDetailUiEffect
}
