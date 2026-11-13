package com.br.androideventscheduler.feature.backup.presentation.state

import androidx.annotation.StringRes
import com.br.androideventscheduler.feature.backup.domain.model.ImportPreview
import com.br.androideventscheduler.feature.backup.domain.model.ImportStrategy

internal sealed class BackupUiState {
    data object Loading : BackupUiState()

    /**
     * [itemCount] zero desabilita a exportação. [isExporting], [isReadingImport] e [isImporting] são operações em
     * andamento (impedem toques repetidos). [preview] é o arquivo já validado, aguardando confirmação;
     * [importErrorRes] é o motivo (texto de resource) de o último arquivo escolhido ter sido recusado.
     */
    data class Content(
        val itemCount: Int,
        val isExporting: Boolean = false,
        val isReadingImport: Boolean = false,
        val preview: ImportPreview? = null,
        val strategy: ImportStrategy = ImportStrategy.REPLACE_EXISTING,
        val isImporting: Boolean = false,
        @param:StringRes val importErrorRes: Int? = null,
    ) : BackupUiState() {
        val isBusy: Boolean get() = isExporting || isReadingImport || isImporting

        /** Exportar exige ao menos um agendamento e nenhuma outra operação em andamento. */
        val canExport: Boolean get() = itemCount > 0 && !isBusy
    }
}
