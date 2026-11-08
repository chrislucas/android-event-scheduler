package com.br.androideventscheduler.feature.alarm.presentation.mapper

import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.alarm.domain.usecase.ScheduleNextAlarmUseCase
import com.br.androideventscheduler.core.common.StringProvider
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.recurrence.RecurrenceEngine
import com.br.androideventscheduler.core.ui.format.toTimeLabel
import com.br.androideventscheduler.feature.alarm.presentation.state.FullScreenAlarmUiState
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

/** `ScheduleItem` → [FullScreenAlarmUiState.Content]: horário formatado e minutos até o início da ocorrência. */
internal class FullScreenAlarmMapper(
    private val strings: StringProvider,
    private val recurrenceEngine: RecurrenceEngine,
    private val clock: Clock,
) {
    fun map(
        item: ScheduleItem,
        defaultSnoozeMinutes: Int,
    ): FullScreenAlarmUiState.Content {
        val snoozeOptions = UserSettings.ALLOWED_SNOOZE_MINUTES
        return FullScreenAlarmUiState.Content(
            title = item.title,
            timeText = timeText(item),
            location = item.location.takeIf { it.isNotBlank() },
            notes = item.notes.takeIf { it.isNotBlank() },
            colorHex = item.colorHex,
            minutesUntilStart = minutesUntilStart(item),
            snoozeOptions = snoozeOptions,
            selectedSnoozeMinutes =
                defaultSnoozeMinutes.takeIf { it in snoozeOptions } ?: UserSettings.DEFAULT_SNOOZE_MINUTES,
        )
    }

    private fun timeText(item: ScheduleItem): String {
        val endTime = item.endTime
        return when {
            item.isAllDay -> strings.getString(R.string.alarm_time_all_day)
            endTime == null -> item.startTime.toTimeLabel()
            else -> strings.getString(R.string.alarm_time_range, item.startTime.toTimeLabel(), endTime.toTimeLabel())
        }
    }

    /**
     * Minutos (arredondados ao mais próximo, pois o alarme chega com alguns segundos de atraso) até o início da
     * ocorrência mais próxima: a primeira cujo início + 1 h ainda não passou. Nunca negativo.
     */
    private fun minutesUntilStart(item: ScheduleItem): Int {
        val now = ZonedDateTime.now(clock)
        val searchStartDate = now.toLocalDate().minusDays(1)
        val occurrenceStart =
            recurrenceEngine
                .occurrenceDates(item, onOrAfter = searchStartDate)
                .map { occurrenceDate -> startOf(item, occurrenceDate) }
                .firstOrNull { start -> !start.plusHours(RECENT_OCCURRENCE_GRACE_HOURS).isBefore(now) }
                ?: startOf(item, item.startDate)
        val secondsUntilStart = Duration.between(now, occurrenceStart).seconds
        return Math.floorDiv(secondsUntilStart + SECONDS_ROUNDING_OFFSET, SECONDS_PER_MINUTE).toInt().coerceAtLeast(0)
    }

    private fun startOf(
        item: ScheduleItem,
        occurrenceDate: LocalDate,
    ): ZonedDateTime {
        val startTime = if (item.isAllDay) ScheduleNextAlarmUseCase.ALL_DAY_ALERT_TIME else item.startTime
        return LocalDateTime.of(occurrenceDate, startTime).atZone(clock.zone)
    }

    private companion object {
        const val RECENT_OCCURRENCE_GRACE_HOURS = 1L
        const val SECONDS_PER_MINUTE = 60L
        const val SECONDS_ROUNDING_OFFSET = 30L
    }
}
