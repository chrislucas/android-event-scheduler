package com.br.androideventscheduler.feature.backup.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.backup.domain.model.ImportStrategy
import com.br.androideventscheduler.feature.backup.presentation.state.BackupUiState

/**
 * Seção "Importar": botão do seletor, progresso da leitura, motivo da recusa do último arquivo (região viva) e,
 * com um arquivo válido, o [ImportPreviewCard].
 */
@Composable
internal fun ImportSection(
    content: BackupUiState.Content,
    onPickClick: () -> Unit,
    onStrategySelect: (ImportStrategy) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickDescription = stringResource(R.string.backup_import_pick_description)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.backup_import_section),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = stringResource(R.string.backup_import_description), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(
            onClick = onPickClick,
            enabled = !content.isBusy,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(SemanticsTags.BACKUP_IMPORT_PICK_BUTTON)
                    .semantics { contentDescription = pickDescription },
        ) {
            Text(stringResource(R.string.backup_import_pick_button))
        }
        if (content.isReadingImport) {
            ProgressRow(
                label = stringResource(R.string.backup_import_reading),
                modifier = Modifier.testTag(SemanticsTags.BACKUP_IMPORT_PROGRESS),
            )
        }
        content.importErrorRes?.let { errorRes ->
            Text(
                text = stringResource(errorRes),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier =
                    Modifier
                        .testTag(SemanticsTags.BACKUP_IMPORT_ERROR)
                        .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        content.preview?.let { preview ->
            ImportPreviewCard(
                preview = preview,
                strategy = content.strategy,
                isImporting = content.isImporting,
                onStrategySelect = onStrategySelect,
                onConfirm = onConfirm,
                onCancel = onCancel,
            )
        }
    }
}

@Preview(name = "Sem arquivo", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun ImportSectionPreview() {
    AndroidEventSchedulerTheme {
        ImportSection(
            content = BackupUiState.Content(itemCount = 3),
            onPickClick = {},
            onStrategySelect = {},
            onConfirm = {},
            onCancel = {},
        )
    }
}

@Preview(name = "Lendo o arquivo", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun ImportSectionReadingPreview() {
    AndroidEventSchedulerTheme {
        ImportSection(
            content = BackupUiState.Content(itemCount = 3, isReadingImport = true),
            onPickClick = {},
            onStrategySelect = {},
            onConfirm = {},
            onCancel = {},
        )
    }
}

@Preview(name = "Arquivo recusado", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun ImportSectionErrorPreview() {
    AndroidEventSchedulerTheme {
        ImportSection(
            content = BackupUiState.Content(itemCount = 3, importErrorRes = R.string.backup_error_not_backup),
            onPickClick = {},
            onStrategySelect = {},
            onConfirm = {},
            onCancel = {},
        )
    }
}

@Preview(name = "Com pré-visualização", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun ImportSectionPreviewPreview() {
    AndroidEventSchedulerTheme {
        ImportSection(
            content = BackupUiState.Content(itemCount = 3, preview = previewImport()),
            onPickClick = {},
            onStrategySelect = {},
            onConfirm = {},
            onCancel = {},
        )
    }
}
