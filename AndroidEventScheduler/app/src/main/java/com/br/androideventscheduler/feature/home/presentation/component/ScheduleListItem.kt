package com.br.androideventscheduler.feature.home.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleOccurrence
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.core.ui.format.ResourcesStringProvider
import com.br.androideventscheduler.core.ui.format.label
import com.br.androideventscheduler.core.ui.format.shortDateTimeLabel
import com.br.androideventscheduler.core.ui.format.toTimeLabel
import java.time.LocalDate
import java.time.LocalTime

private val MIN_ROW_HEIGHT = 64.dp
private val COLOR_BAR_WIDTH = 4.dp

/** Linha da agenda do dia: barra na cor do item, adesivo, título, intervalo e calendário de origem. */
@Composable
internal fun ScheduleListItem(
    occurrence: ScheduleOccurrence,
    accountEmail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = occurrence.item
    val resources = LocalContext.current.resources
    val calendarLabel = item.calendarSource.label(personalAccountLabel = accountEmail, strings = ResourcesStringProvider(resources))
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MIN_ROW_HEIGHT)
                .height(IntrinsicSize.Min)
                .testTag(SemanticsTags.HOME_SCHEDULE_ITEM_PREFIX + item.id)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .width(COLOR_BAR_WIDTH)
                    .fillMaxHeight()
                    .background(Color(item.colorHex), RoundedCornerShape(COLOR_BAR_WIDTH)),
        )
        item.stickerEmoji?.let { emoji -> Text(text = emoji, style = MaterialTheme.typography.titleLarge) }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.home_item_interval_and_calendar, occurrence.intervalText(), calendarLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** `Todo o dia`, `22:00 – 23:30`, `22:00` (sem fim, lembretes) ou `qui., 5 de nov., 22:00 → sex., 6 de nov., 01:00`. */
@Composable
private fun ScheduleOccurrence.intervalText(): String {
    val resources = LocalContext.current.resources
    val endTime = item.endTime
    return when {
        item.isAllDay -> {
            stringResource(R.string.home_all_day)
        }

        item.type == ScheduleType.REMINDER || endTime == null -> {
            item.startTime.toTimeLabel()
        }

        startDate == endDate -> {
            stringResource(R.string.home_interval_range, item.startTime.toTimeLabel(), endTime.toTimeLabel())
        }

        else -> {
            stringResource(
                R.string.home_interval_multi_day,
                shortDateTimeLabel(startDate, item.startTime, resources),
                shortDateTimeLabel(endDate, endTime, resources),
            )
        }
    }
}

@Preview(showBackground = true, name = "Evento com intervalo")
@Composable
private fun ScheduleListItemPreview() {
    AndroidEventSchedulerTheme {
        ScheduleListItem(
            occurrence = previewOccurrence(endTime = LocalTime.of(23, 30)),
            accountEmail = "bia@exemplo.com",
            onClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Com adesivo")
@Composable
private fun ScheduleListItemStickerPreview() {
    AndroidEventSchedulerTheme {
        ScheduleListItem(
            occurrence = previewOccurrence(sticker = "🚀", endTime = LocalTime.of(11, 0)),
            accountEmail = "bia@exemplo.com",
            onClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Dia inteiro")
@Composable
private fun ScheduleListItemAllDayPreview() {
    AndroidEventSchedulerTheme {
        ScheduleListItem(
            occurrence = previewOccurrence(allDay = true, source = CalendarSource.FAMILY),
            accountEmail = "bia@exemplo.com",
            onClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Multi-dia, título longo")
@Composable
private fun ScheduleListItemMultiDayPreview() {
    AndroidEventSchedulerTheme {
        ScheduleListItem(
            occurrence =
                previewOccurrence(
                    title = "Viagem de fim de ano com toda a família para a casa de praia",
                    endTime = LocalTime.of(8, 0),
                    endDate = LocalDate.of(2026, 11, 8),
                ),
            accountEmail = "bia@exemplo.com",
            onClick = {},
        )
    }
}
