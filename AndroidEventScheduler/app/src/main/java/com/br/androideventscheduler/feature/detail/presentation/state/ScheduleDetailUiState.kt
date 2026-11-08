package com.br.androideventscheduler.feature.detail.presentation.state

/** Modelo de UI já formatado (textos prontos): evita formatar no Composable e facilita testar o mapper. */
internal data class ScheduleDetailUi(
    val id: String,
    val title: String,
    val stickerEmoji: String?,
    val colorHex: Long,
    val dateRangeText: String,
    val calendarLabel: String,
    val calendarColorHex: Long,
    val reminderText: String,
    val recurrenceText: String,
    val location: String?,
    val notes: String?,
)

internal sealed class ScheduleDetailUiState {
    data object Loading : ScheduleDetailUiState()

    data class Content(
        val schedule: ScheduleDetailUi,
    ) : ScheduleDetailUiState()

    data object NotFound : ScheduleDetailUiState()
}
