package com.br.androideventscheduler.feature.backup.presentation.event

import com.br.androideventscheduler.feature.backup.domain.model.ImportStrategy

internal sealed interface BackupUiEvent {
    data object OnShareExportClicked : BackupUiEvent

    data object OnSaveExportClicked : BackupUiEvent

    /** Retorno do `CreateDocument`; `null` quando o usuário cancelou o seletor. */
    data class OnSaveDocumentCreated(
        val uri: String?,
    ) : BackupUiEvent

    data object OnPickImportClicked : BackupUiEvent

    /** Retorno do `OpenDocument`; `null` quando o usuário cancelou o seletor. */
    data class OnImportFileChosen(
        val uri: String?,
    ) : BackupUiEvent

    data class OnStrategySelected(
        val strategy: ImportStrategy,
    ) : BackupUiEvent

    data object OnConfirmImportClicked : BackupUiEvent

    data object OnCancelImportClicked : BackupUiEvent

    data object OnBackClicked : BackupUiEvent
}
