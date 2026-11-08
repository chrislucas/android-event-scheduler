package com.br.androideventscheduler.feature.home.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleOccurrence
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.home.presentation.mapper.toDayHeader
import java.time.LocalDate

/**
 * Itens da agenda do dia dentro da lista rolável da Home: cabeçalho do dia selecionado (`5 QUI.`) seguido dos
 * compromissos ou do estado vazio. Tudo na mesma lista da grade para que telas baixas (paisagem, fonte grande)
 * continuem alcançando a agenda.
 */
internal fun LazyListScope.dayScheduleItems(
    selectedDate: LocalDate,
    schedules: List<ScheduleOccurrence>,
    accountEmail: String,
    onScheduleClick: (scheduleId: String) -> Unit,
) {
    item(key = "day-header") {
        Text(
            text = selectedDate.toDayHeader(),
            modifier =
                Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag(SemanticsTags.HOME_DAY_HEADER)
                    .semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (schedules.isEmpty()) {
        item(key = "day-empty") {
            Box(
                modifier = Modifier.fillMaxWidth().testTag(SemanticsTags.HOME_EMPTY_DAY),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = stringResource(R.string.home_empty_day),
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        items(schedules, key = { occurrence -> occurrence.item.id + occurrence.startDate.toEpochDay() }) { occurrence ->
            ScheduleListItem(
                occurrence = occurrence,
                accountEmail = accountEmail,
                onClick = { onScheduleClick(occurrence.item.id) },
            )
        }
    }
}

@Preview(showBackground = true, name = "Com compromissos")
@Composable
private fun DayScheduleListPreview() {
    AndroidEventSchedulerTheme {
        LazyColumn {
            dayScheduleItems(
                selectedDate = PreviewDate,
                schedules = previewDaySchedules(),
                accountEmail = "bia@exemplo.com",
                onScheduleClick = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Dia vazio")
@Composable
private fun DayScheduleListEmptyPreview() {
    AndroidEventSchedulerTheme {
        LazyColumn {
            dayScheduleItems(selectedDate = PreviewDate, schedules = emptyList(), accountEmail = "bia@exemplo.com", onScheduleClick = {})
        }
    }
}
