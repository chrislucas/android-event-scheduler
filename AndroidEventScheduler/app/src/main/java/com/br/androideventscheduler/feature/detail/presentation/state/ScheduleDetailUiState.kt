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
    /** Itens recorrentes mostram, no diálogo de exclusão, o aviso de que todas as ocorrências serão removidas. */
    val isRecurring: Boolean = false,
)

internal sealed class ScheduleDetailUiState {
    data object Loading : ScheduleDetailUiState()

    data class Content(
        val schedule: ScheduleDetailUi,
        /** Restaurado após rotação/morte do processo (`SavedStateHandle`). */
        val isDeleteDialogVisible: Boolean = false,
        /** Duplicar/excluir em andamento: os botões da barra ficam desabilitados e toques repetidos são ignorados. */
        val isProcessingAction: Boolean = false,
    ) : ScheduleDetailUiState()

    data object NotFound : ScheduleDetailUiState()
}
