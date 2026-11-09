package com.br.androideventscheduler.feature.backup.presentation.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.backup.domain.model.ImportStrategy

private val MIN_TOUCH_HEIGHT = 48.dp

@StringRes
private fun ImportStrategy.labelRes(): Int =
    when (this) {
        ImportStrategy.REPLACE_EXISTING -> R.string.backup_strategy_replace
        ImportStrategy.KEEP_EXISTING -> R.string.backup_strategy_keep
        ImportStrategy.IMPORT_AS_COPIES -> R.string.backup_strategy_copies
    }

/** O que fazer com os itens que já existem: grupo exclusivo (`selectableGroup`) de três `RadioButton`. */
@Composable
internal fun ImportStrategySelector(
    selected: ImportStrategy,
    enabled: Boolean,
    onSelect: (ImportStrategy) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.backup_strategy_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        Column(modifier = Modifier.selectableGroup().testTag(SemanticsTags.BACKUP_IMPORT_STRATEGY_GROUP)) {
            ImportStrategy.entries.forEach { strategy ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = MIN_TOUCH_HEIGHT)
                            .testTag(SemanticsTags.BACKUP_IMPORT_STRATEGY_OPTION_PREFIX + strategy.name)
                            .selectable(
                                selected = strategy == selected,
                                enabled = enabled,
                                role = Role.RadioButton,
                                onClick = { onSelect(strategy) },
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RadioButton(selected = strategy == selected, onClick = null, enabled = enabled)
                    Text(text = stringResource(strategy.labelRes()), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Preview(name = "Substituir selecionado", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun ImportStrategySelectorPreview() {
    AndroidEventSchedulerTheme {
        ImportStrategySelector(selected = ImportStrategy.REPLACE_EXISTING, enabled = true, onSelect = {})
    }
}

@Preview(name = "Cópias selecionado e desabilitado", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun ImportStrategySelectorDisabledPreview() {
    AndroidEventSchedulerTheme {
        ImportStrategySelector(selected = ImportStrategy.IMPORT_AS_COPIES, enabled = false, onSelect = {})
    }
}
