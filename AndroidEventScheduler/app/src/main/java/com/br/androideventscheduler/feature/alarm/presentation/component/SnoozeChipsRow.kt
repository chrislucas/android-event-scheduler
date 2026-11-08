package com.br.androideventscheduler.feature.alarm.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags

private val CHIP_MIN_HEIGHT = 48.dp
private val CHECK_ICON_SIZE = 18.dp

/**
 * Opções de adiamento como grupo de seleção única (`selectableGroup` + `Role.RadioButton`). A opção selecionada
 * também mostra um "check", para que a seleção não dependa só da cor.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SnoozeChipsRow(
    options: List<Int>,
    selectedMinutes: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupDescription = stringResource(R.string.alarm_snooze_chips_description)
    FlowRow(
        modifier =
            modifier
                .testTag(SemanticsTags.ALARM_SNOOZE_CHIPS)
                .selectableGroup()
                .semantics { contentDescription = groupDescription },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { minutes ->
            val selected = minutes == selectedMinutes
            val chipDescription = pluralStringResource(R.plurals.alarm_snooze_chip_description, minutes, minutes)
            FilterChip(
                selected = selected,
                onClick = { onSelect(minutes) },
                enabled = enabled,
                label = { Text(stringResource(R.string.alarm_snooze_chip_label, minutes)) },
                leadingIcon =
                    if (selected) {
                        {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.size(CHECK_ICON_SIZE),
                            )
                        }
                    } else {
                        null
                    },
                modifier =
                    Modifier
                        .defaultMinSize(minHeight = CHIP_MIN_HEIGHT)
                        .testTag(SemanticsTags.ALARM_SNOOZE_CHIP_PREFIX + minutes)
                        .semantics {
                            role = Role.RadioButton
                            contentDescription = chipDescription
                        },
            )
        }
    }
}

@Preview(name = "Padrão 10 min", showBackground = true)
@Composable
private fun SnoozeChipsRowPreview() {
    AndroidEventSchedulerTheme {
        SnoozeChipsRow(options = listOf(5, 10, 15), selectedMinutes = 10, enabled = true, onSelect = {})
    }
}

@Preview(name = "Desabilitado", showBackground = true)
@Composable
private fun SnoozeChipsRowDisabledPreview() {
    AndroidEventSchedulerTheme {
        SnoozeChipsRow(options = listOf(5, 10, 15), selectedMinutes = 5, enabled = false, onSelect = {})
    }
}
