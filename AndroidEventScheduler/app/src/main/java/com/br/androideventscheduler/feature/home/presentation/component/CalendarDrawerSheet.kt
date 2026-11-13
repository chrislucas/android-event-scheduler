package com.br.androideventscheduler.feature.home.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.core.ui.format.label
import com.br.androideventscheduler.core.ui.format.rememberResourcesStringProvider

/**
 * Conteúdo da gaveta lateral da Home: um filtro (caixa de seleção) por calendário e o atalho para as
 * Configurações. Stateless: quem chama guarda o conjunto de [enabledSources].
 */
@Composable
internal fun CalendarDrawerSheet(
    enabledSources: Set<CalendarSource>,
    personalAccountLabel: String,
    onToggle: (CalendarSource) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val drawerDescription = stringResource(R.string.home_drawer_description)
    val strings = rememberResourcesStringProvider()
    ModalDrawerSheet(modifier = modifier.testTag(SemanticsTags.HOME_DRAWER_SHEET).semantics { contentDescription = drawerDescription }) {
        Text(
            text = stringResource(R.string.home_drawer_calendars_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
        )
        CalendarSource.entries.forEach { source ->
            CalendarFilterRow(
                source = source,
                label = source.label(personalAccountLabel, strings),
                checked = source in enabledSources,
                onCheckedChange = { onToggle(source) },
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        val settingsDescription = stringResource(R.string.home_drawer_settings_description)
        NavigationDrawerItem(
            label = { Text(text = stringResource(R.string.home_drawer_settings)) },
            selected = false,
            onClick = onSettingsClick,
            modifier =
                Modifier
                    .padding(horizontal = 12.dp)
                    .testTag(SemanticsTags.HOME_DRAWER_SETTINGS_BUTTON)
                    .semantics { contentDescription = settingsDescription },
        )
    }
}

@Composable
private fun CalendarFilterRow(
    source: CalendarSource,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val state =
        stringResource(if (checked) R.string.home_drawer_filter_visible else R.string.home_drawer_filter_hidden)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp)
                .testTag(SemanticsTags.HOME_DRAWER_FILTER_PREFIX + source.id)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
                .semantics { stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(modifier = Modifier.width(12.dp))
        Spacer(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(source.colorHex)))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Preview(showBackground = true, name = "Todos visíveis")
@Composable
private fun CalendarDrawerSheetAllVisiblePreview() {
    AndroidEventSchedulerTheme {
        CalendarDrawerSheet(CalendarSource.entries.toSet(), "bia@exemplo.com", onToggle = {}, onSettingsClick = {})
    }
}

@Preview(showBackground = true, name = "Um oculto")
@Composable
private fun CalendarDrawerSheetOneHiddenPreview() {
    AndroidEventSchedulerTheme {
        CalendarDrawerSheet(
            setOf(CalendarSource.PERSONAL_ACCOUNT, CalendarSource.MY_REMINDERS),
            "bia@exemplo.com",
            onToggle = {},
            onSettingsClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Todos ocultos")
@Composable
private fun CalendarDrawerSheetAllHiddenPreview() {
    AndroidEventSchedulerTheme {
        CalendarDrawerSheet(emptySet(), "bia@exemplo.com", onToggle = {}, onSettingsClick = {})
    }
}

@Preview(showBackground = true, name = "E-mail longo")
@Composable
private fun CalendarDrawerSheetLongEmailPreview() {
    AndroidEventSchedulerTheme {
        CalendarDrawerSheet(
            CalendarSource.entries.toSet(),
            "nome.muito.comprido.de.usuario@dominio-bastante-extenso-de-exemplo.com.br",
            onToggle = {},
            onSettingsClick = {},
        )
    }
}
