package com.br.androideventscheduler.feature.upsert.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.ui.SemanticsTags

private val PILL_PADDING = 4.dp
private val TAB_MIN_HEIGHT = 48.dp

/** Alternador em pílula **Evento | Lembrete**: cada aba é uma `Role.Tab` com `selected`. */
@Composable
internal fun ModeSegmentedPill(
    mode: ScheduleType,
    onModeSelected: (ScheduleType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupDescription = stringResource(R.string.upsert_mode_group_description)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(PILL_PADDING)
                .selectableGroup()
                .semantics { contentDescription = groupDescription },
    ) {
        ModeTab(
            label = stringResource(R.string.upsert_mode_event),
            description = stringResource(R.string.upsert_mode_event_description),
            selected = mode == ScheduleType.EVENT,
            tag = SemanticsTags.UPSERT_MODE_EVENT_TAB,
            onClick = { onModeSelected(ScheduleType.EVENT) },
        )
        ModeTab(
            label = stringResource(R.string.upsert_mode_reminder),
            description = stringResource(R.string.upsert_mode_reminder_description),
            selected = mode == ScheduleType.REMINDER,
            tag = SemanticsTags.UPSERT_MODE_REMINDER_TAB,
            onClick = { onModeSelected(ScheduleType.REMINDER) },
        )
    }
}

@Composable
private fun RowScope.ModeTab(
    label: String,
    description: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier =
            Modifier
                .weight(1f)
                .heightIn(min = TAB_MIN_HEIGHT)
                .clip(CircleShape)
                .background(containerColor)
                .selectable(selected = selected, role = Role.Tab, onClick = onClick)
                .semantics { contentDescription = description }
                .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}

@Preview(name = "Lembrete selecionado", showBackground = true)
@Composable
private fun ModeSegmentedPillReminderPreview() {
    AndroidEventSchedulerTheme { ModeSegmentedPill(mode = ScheduleType.REMINDER, onModeSelected = {}) }
}

@Preview(name = "Evento selecionado", showBackground = true)
@Composable
private fun ModeSegmentedPillEventPreview() {
    AndroidEventSchedulerTheme { ModeSegmentedPill(mode = ScheduleType.EVENT, onModeSelected = {}) }
}
