package com.br.androideventscheduler.feature.upsert.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.common.StringProvider
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.core.ui.format.ResourcesStringProvider
import com.br.androideventscheduler.core.ui.format.label
import com.br.androideventscheduler.core.ui.format.toDisplayLabel

/** Alerta: a linha mostra a antecedência atual; tocar expande a lista de opções (grupo de rádio). */
@Composable
internal fun ReminderOffsetRow(
    selected: ReminderOffset,
    onSelected: (ReminderOffset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = rememberStringProvider()
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        SettingRow(
            iconRes = R.drawable.ic_bell,
            label = stringResource(R.string.upsert_reminder_label),
            value = selected.toDisplayLabel(strings),
            tag = SemanticsTags.UPSERT_REMINDER_ROW,
            onClick = { isExpanded = !isExpanded },
            isExpanded = isExpanded,
        )
        if (isExpanded) {
            OptionGroup(
                options = ReminderOffset.entries,
                selected = selected,
                labelOf = { it.toDisplayLabel(strings) },
                tagOf = { SemanticsTags.UPSERT_REMINDER_OPTION_PREFIX + it.name },
                onSelected = {
                    onSelected(it)
                    isExpanded = false
                },
            )
        }
    }
}

/** Calendário de um Evento: conta pessoal (e-mail ativo) ou Family. Lembretes usam "Meus lembretes" fixo. */
@Composable
internal fun CalendarSourceRow(
    selected: CalendarSource,
    accountEmail: String,
    onSelected: (CalendarSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = rememberStringProvider()
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        SettingRow(
            iconRes = R.drawable.ic_calendar_today,
            label = stringResource(R.string.upsert_calendar_label),
            value = selected.label(accountEmail, strings),
            tag = SemanticsTags.UPSERT_CALENDAR_SOURCE_ROW,
            onClick = { isExpanded = !isExpanded },
            isExpanded = isExpanded,
        )
        if (isExpanded) {
            OptionGroup(
                options = listOf(CalendarSource.PERSONAL_ACCOUNT, CalendarSource.FAMILY),
                selected = selected,
                labelOf = { it.label(accountEmail, strings) },
                tagOf = { SemanticsTags.UPSERT_CALENDAR_SOURCE_OPTION_PREFIX + it.name },
                onSelected = {
                    onSelected(it)
                    isExpanded = false
                },
            )
        }
    }
}

@Composable
private fun rememberStringProvider(): StringProvider = ResourcesStringProvider(LocalContext.current.resources)

@Composable
private fun <T> OptionGroup(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    tagOf: (T) -> String,
    onSelected: (T) -> Unit,
) {
    val selectedState = stringResource(R.string.upsert_state_selected)
    val notSelectedState = stringResource(R.string.upsert_state_not_selected)
    Column(modifier = Modifier.selectableGroup().padding(start = 40.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelected(option) })
                        .semantics { stateDescription = if (isSelected) selectedState else notSelectedState }
                        .testTag(tagOf(option)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = isSelected, onClick = null)
                Text(text = labelOf(option), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

@Preview(name = "Alerta", showBackground = true)
@Composable
private fun ReminderOffsetRowPreview() {
    AndroidEventSchedulerTheme { ReminderOffsetRow(selected = ReminderOffset.FIFTEEN_MINUTES, onSelected = {}) }
}

@Preview(name = "Calendário pessoal", showBackground = true)
@Composable
private fun CalendarSourceRowPersonalPreview() {
    AndroidEventSchedulerTheme {
        CalendarSourceRow(selected = CalendarSource.PERSONAL_ACCOUNT, accountEmail = "ana@exemplo.com", onSelected = {})
    }
}

@Preview(name = "Calendário Family", showBackground = true)
@Composable
private fun CalendarSourceRowFamilyPreview() {
    AndroidEventSchedulerTheme {
        CalendarSourceRow(selected = CalendarSource.FAMILY, accountEmail = "ana@exemplo.com", onSelected = {})
    }
}
