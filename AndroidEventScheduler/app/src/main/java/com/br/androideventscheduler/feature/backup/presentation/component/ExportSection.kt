package com.br.androideventscheduler.feature.backup.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

/**
 * Seção "Exportar": os dois botões ficam desabilitados com agenda vazia ou enquanto algo está em andamento
 * ([enabled]); [isExporting] mostra o progresso, anunciado pelo leitor de tela (`liveRegion`).
 */
@Composable
internal fun ExportSection(
    hasItems: Boolean,
    enabled: Boolean,
    isExporting: Boolean,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shareDescription = stringResource(R.string.backup_export_share_description)
    val saveDescription = stringResource(R.string.backup_export_save_description)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.backup_export_section),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = stringResource(R.string.backup_export_description), style = MaterialTheme.typography.bodyMedium)
        if (!hasItems) {
            Text(
                text = stringResource(R.string.backup_export_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(SemanticsTags.BACKUP_EXPORT_EMPTY_MESSAGE),
            )
        }
        Button(
            onClick = onShareClick,
            enabled = enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(SemanticsTags.BACKUP_EXPORT_SHARE_BUTTON)
                    .semantics { contentDescription = shareDescription },
        ) {
            Text(stringResource(R.string.backup_export_share_button))
        }
        OutlinedButton(
            onClick = onSaveClick,
            enabled = enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(SemanticsTags.BACKUP_EXPORT_SAVE_BUTTON)
                    .semantics { contentDescription = saveDescription },
        ) {
            Text(stringResource(R.string.backup_export_save_button))
        }
        if (isExporting) {
            ProgressRow(
                label = stringResource(R.string.backup_export_progress),
                modifier = Modifier.testTag(SemanticsTags.BACKUP_EXPORT_PROGRESS),
            )
        }
        Text(
            text = stringResource(R.string.backup_encryption_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(SemanticsTags.BACKUP_ENCRYPTION_WARNING),
        )
    }
}

/** Indicador de progresso com texto, anunciado como região viva (o leitor de tela fala o [label] ao aparecer). */
@Composable
internal fun ProgressRow(
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = label
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(name = "Com agendamentos", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun ExportSectionPreview() {
    AndroidEventSchedulerTheme {
        ExportSection(hasItems = true, enabled = true, isExporting = false, onShareClick = {}, onSaveClick = {})
    }
}

@Preview(name = "Agenda vazia", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun ExportSectionEmptyPreview() {
    AndroidEventSchedulerTheme {
        ExportSection(hasItems = false, enabled = false, isExporting = false, onShareClick = {}, onSaveClick = {})
    }
}

@Preview(name = "Exportando", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun ExportSectionExportingPreview() {
    AndroidEventSchedulerTheme {
        ExportSection(hasItems = true, enabled = false, isExporting = true, onShareClick = {}, onSaveClick = {})
    }
}

@Preview(name = "Progresso", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun ProgressRowPreview() {
    AndroidEventSchedulerTheme { ProgressRow(label = stringResource(R.string.backup_import_reading)) }
}
