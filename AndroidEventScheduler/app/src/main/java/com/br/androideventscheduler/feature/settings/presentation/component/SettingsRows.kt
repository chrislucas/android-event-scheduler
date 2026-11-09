package com.br.androideventscheduler.feature.settings.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme

private val MIN_TOUCH_HEIGHT = 48.dp
private val ROW_SPACING = 12.dp

/** Título de seção anunciado como cabeçalho pelo TalkBack. */
@Composable
internal fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 16.dp, bottom = 4.dp).semantics { heading() },
    )
}

/** Opção de um grupo exclusivo. A linha inteira é o alvo de toque (`Role.RadioButton`). */
@Composable
internal fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MIN_TOUCH_HEIGHT)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Grupo de opções exclusivas (`selectableGroup` para o TalkBack anunciar "1 de N"). */
@Composable
internal fun <T> SettingsRadioGroup(
    options: List<T>,
    selectedOption: T,
    labelFor: @Composable (T) -> String,
    testTagFor: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.selectableGroup()) {
        options.forEach { option ->
            SettingsRadioRow(
                label = labelFor(option),
                selected = option == selectedOption,
                onSelect = { onSelect(option) },
                modifier = Modifier.testTag(testTagFor(option)),
            )
        }
    }
}

/** Linha com switch; a linha inteira alterna (`Role.Switch`). */
@Composable
internal fun SettingsSwitchRow(
    label: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MIN_TOUCH_HEIGHT)
                .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** Linha que abre outra tela (`Role.Button`), com texto de apoio e seta; a linha inteira é o alvo de toque. */
@Composable
internal fun SettingsNavigationRow(
    label: String,
    supportingText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MIN_TOUCH_HEIGHT)
                .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(painter = painterResource(R.drawable.ic_chevron_right), contentDescription = null)
    }
}

@Preview(name = "Título de seção", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun SettingsSectionTitlePreview() {
    AndroidEventSchedulerTheme { SettingsSectionTitle(text = stringResource(R.string.settings_account_section)) }
}

@Preview(name = "Opção selecionada e não selecionada", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun SettingsRadioRowPreview() {
    AndroidEventSchedulerTheme {
        Column {
            SettingsRadioRow(label = "15 minutos antes", selected = true, onSelect = {})
            SettingsRadioRow(label = "30 minutos antes", selected = false, onSelect = {})
        }
    }
}

@Preview(name = "Grupo de opções", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun SettingsRadioGroupPreview() {
    AndroidEventSchedulerTheme {
        SettingsRadioGroup(
            options = listOf(5, 10, 15),
            selectedOption = 10,
            labelFor = { "$it min" },
            testTagFor = { "preview_$it" },
            onSelect = {},
        )
    }
}

@Preview(name = "Switch ligado e desligado", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun SettingsSwitchRowPreview() {
    AndroidEventSchedulerTheme {
        Column {
            SettingsSwitchRow(label = "Alerta em tela cheia", supportingText = "Como um alarme", checked = true, onCheckedChange = {})
            SettingsSwitchRow(label = "Alerta em tela cheia", supportingText = "Como um alarme", checked = false, onCheckedChange = {})
        }
    }
}

@Preview(name = "Linha de navegação", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun SettingsNavigationRowPreview() {
    AndroidEventSchedulerTheme {
        SettingsNavigationRow(
            label = stringResource(R.string.settings_backup_item),
            supportingText = stringResource(R.string.settings_backup_item_supporting),
            onClick = {},
        )
    }
}
