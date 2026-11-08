package com.br.androideventscheduler.feature.upsert.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.ui.format.toDisplayLabel
import com.br.androideventscheduler.core.ui.format.toShortDateLabel
import com.br.androideventscheduler.core.ui.format.toTimeLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

private val ROW_MIN_HEIGHT = 48.dp
private val ROW_ICON_SIZE = 24.dp

/**
 * Linha do formulário: ícone, rótulo e valor atual. Com [onClick] vira um botão (`Role.Button`); [isExpanded]
 * não nulo acrescenta o estado "Expandido/Recolhido". A descrição de acessibilidade é "rótulo: valor".
 */
@Composable
internal fun SettingRow(
    @DrawableRes iconRes: Int,
    label: String,
    value: String,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isExpanded: Boolean? = null,
) {
    val description = stringResource(R.string.upsert_row_description, label, value)
    val expansionState =
        isExpanded?.let { expanded ->
            stringResource(if (expanded) R.string.upsert_state_expanded else R.string.upsert_state_collapsed)
        }
    val clickModifier = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT)
                .then(clickModifier)
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    expansionState?.let { stateDescription = it }
                }.testTag(tag)
                .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ROW_ICON_SIZE),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Preview(name = "Recolhida", showBackground = true)
@Composable
private fun SettingRowCollapsedPreview() {
    AndroidEventSchedulerTheme {
        SettingRow(
            iconRes = R.drawable.ic_calendar_today,
            label = stringResource(R.string.upsert_label_start_date),
            value = LocalDate.of(2026, 11, 5).toShortDateLabel(),
            tag = "preview",
            onClick = {},
            isExpanded = false,
        )
    }
}

@Preview(name = "Expandida", showBackground = true)
@Composable
private fun SettingRowExpandedPreview() {
    AndroidEventSchedulerTheme {
        SettingRow(
            iconRes = R.drawable.ic_schedule,
            label = stringResource(R.string.upsert_label_start_time),
            value = LocalTime.of(22, 0).toTimeLabel(),
            tag = "preview",
            onClick = {},
            isExpanded = true,
        )
    }
}

@Preview(name = "Sem clique", showBackground = true)
@Composable
private fun SettingRowReadOnlyPreview() {
    AndroidEventSchedulerTheme {
        SettingRow(
            iconRes = R.drawable.ic_repeat,
            label = stringResource(R.string.upsert_recurrence_label),
            value = RecurrencePattern.None.toDisplayLabel(LocalContext.current.resources),
            tag = "preview",
        )
    }
}

@Preview(name = "Valor longo", showBackground = true)
@Composable
private fun SettingRowLongValuePreview() {
    AndroidEventSchedulerTheme {
        val weeklyEveryDay = RecurrencePattern.Weekly(daysOfWeek = DayOfWeek.entries.toSet(), occurrenceLimit = 5000)
        SettingRow(
            iconRes = R.drawable.ic_repeat,
            label = stringResource(R.string.upsert_recurrence_label),
            value = weeklyEveryDay.toDisplayLabel(LocalContext.current.resources),
            tag = "preview",
        )
    }
}
