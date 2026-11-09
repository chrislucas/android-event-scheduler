package com.br.androideventscheduler.feature.backup.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
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
import com.br.androideventscheduler.feature.backup.domain.model.ImportPreview
import com.br.androideventscheduler.feature.backup.domain.model.ImportStrategy

/**
 * Resumo do arquivo já validado (total, novos, existentes, ignorados), a escolha para os que já existem e os
 * botões Importar/Cancelar. A escolha só aparece quando há itens que já existem.
 */
@Composable
internal fun ImportPreviewCard(
    preview: ImportPreview,
    strategy: ImportStrategy,
    isImporting: Boolean,
    onStrategySelect: (ImportStrategy) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val confirmDescription = stringResource(R.string.backup_import_confirm_description)
    val importingLabel = stringResource(R.string.backup_importing)
    Card(modifier = modifier.fillMaxWidth().testTag(SemanticsTags.BACKUP_IMPORT_PREVIEW)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.backup_preview_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = pluralStringResource(R.plurals.backup_preview_total, preview.totalInFile, preview.totalInFile),
                modifier = Modifier.testTag(SemanticsTags.BACKUP_IMPORT_PREVIEW_TOTAL),
            )
            Text(
                text = pluralStringResource(R.plurals.backup_preview_new, preview.newCount, preview.newCount),
                modifier = Modifier.testTag(SemanticsTags.BACKUP_IMPORT_PREVIEW_NEW),
            )
            Text(
                text = pluralStringResource(R.plurals.backup_preview_existing, preview.existingCount, preview.existingCount),
                modifier = Modifier.testTag(SemanticsTags.BACKUP_IMPORT_PREVIEW_EXISTING),
            )
            if (preview.invalidCount > 0) {
                Text(
                    text = pluralStringResource(R.plurals.backup_preview_invalid, preview.invalidCount, preview.invalidCount),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag(SemanticsTags.BACKUP_IMPORT_PREVIEW_INVALID),
                )
            }
            if (preview.existingCount > 0) {
                ImportStrategySelector(selected = strategy, enabled = !isImporting, onSelect = onStrategySelect)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isImporting,
                    modifier = Modifier.testTag(SemanticsTags.BACKUP_IMPORT_CANCEL_BUTTON),
                ) {
                    Text(stringResource(R.string.backup_import_cancel_button))
                }
                Button(
                    onClick = onConfirm,
                    enabled = !isImporting,
                    modifier =
                        Modifier
                            .testTag(SemanticsTags.BACKUP_IMPORT_CONFIRM_BUTTON)
                            .semantics { contentDescription = if (isImporting) importingLabel else confirmDescription },
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).semantics { liveRegion = LiveRegionMode.Polite },
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.backup_import_confirm_button))
                    }
                }
            }
        }
    }
}

@Preview(name = "Com repetidos e inválidos", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun ImportPreviewCardPreview() {
    AndroidEventSchedulerTheme {
        ImportPreviewCard(
            preview = previewImport(),
            strategy = ImportStrategy.REPLACE_EXISTING,
            isImporting = false,
            onStrategySelect = {},
            onConfirm = {},
            onCancel = {},
        )
    }
}

@Preview(name = "Só novos", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun ImportPreviewCardOnlyNewPreview() {
    AndroidEventSchedulerTheme {
        ImportPreviewCard(
            preview = previewImport(newCount = 1, existingCount = 0, invalidCount = 0),
            strategy = ImportStrategy.REPLACE_EXISTING,
            isImporting = false,
            onStrategySelect = {},
            onConfirm = {},
            onCancel = {},
        )
    }
}

@Preview(name = "Importando", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun ImportPreviewCardImportingPreview() {
    AndroidEventSchedulerTheme {
        ImportPreviewCard(
            preview = previewImport(),
            strategy = ImportStrategy.IMPORT_AS_COPIES,
            isImporting = true,
            onStrategySelect = {},
            onConfirm = {},
            onCancel = {},
        )
    }
}
