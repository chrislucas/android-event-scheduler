package com.br.androideventscheduler.core.schedule.data.mapper

import com.br.androideventscheduler.core.schedule.data.local.ScheduleDao
import com.br.androideventscheduler.core.schedule.data.local.ScheduleEntity
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.domain.search.SearchTextNormalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

private const val MINUTES_PER_HOUR = 60
private const val RECURRENCE_DAILY = "DAILY"
private const val RECURRENCE_WEEKLY = "WEEKLY"

internal fun ScheduleItem.toEntity(): ScheduleEntity =
    ScheduleEntity(
        id = id,
        title = title,
        type = type.name,
        startDateEpochDay = startDate.toEpochDay(),
        endDateEpochDay = endDate.toEpochDay(),
        startMinuteOfDay = startTime.toMinuteOfDay(),
        endMinuteOfDay = endTime?.toMinuteOfDay(),
        isAllDay = isAllDay,
        location = location,
        notes = notes,
        colorHex = colorHex,
        calendarSourceId = calendarSource.id,
        reminderMinutes = reminderOffset.minutes,
        recurrenceType = recurrencePattern.toRecurrenceType(),
        recurrenceDaysMask = (recurrencePattern as? RecurrencePattern.Weekly)?.daysOfWeek?.toDaysMask() ?: 0,
        occurrenceLimit = recurrencePattern.occurrenceLimit,
        stickerEmoji = stickerEmoji,
        searchText = SearchTextNormalizer.normalize("$title $notes $location"),
    )

internal fun ScheduleEntity.toDomain(): ScheduleItem {
    val scheduleType = ScheduleType.valueOf(type)
    return ScheduleItem(
        id = id,
        title = title,
        type = scheduleType,
        startDate = LocalDate.ofEpochDay(startDateEpochDay),
        endDate = LocalDate.ofEpochDay(endDateEpochDay),
        startTime = startMinuteOfDay.toLocalTime(),
        endTime = endMinuteOfDay?.toLocalTime(),
        isAllDay = isAllDay,
        location = location,
        notes = notes,
        colorHex = colorHex,
        calendarSource = CalendarSource.fromId(calendarSourceId) ?: CalendarSource.defaultFor(scheduleType),
        reminderOffset = ReminderOffset.fromMinutes(reminderMinutes) ?: ReminderOffset.DEFAULT,
        recurrencePattern = toRecurrencePattern(),
        stickerEmoji = stickerEmoji,
    )
}

private fun ScheduleEntity.toRecurrencePattern(): RecurrencePattern =
    when (recurrenceType) {
        RECURRENCE_DAILY -> RecurrencePattern.Daily(occurrenceLimit)
        RECURRENCE_WEEKLY -> RecurrencePattern.Weekly(recurrenceDaysMask.toDaysOfWeek(), occurrenceLimit)
        else -> RecurrencePattern.None
    }

private fun RecurrencePattern.toRecurrenceType(): String =
    when (this) {
        RecurrencePattern.None -> ScheduleDao.RECURRENCE_NONE
        is RecurrencePattern.Daily -> RECURRENCE_DAILY
        is RecurrencePattern.Weekly -> RECURRENCE_WEEKLY
    }

internal fun Set<DayOfWeek>.toDaysMask(): Int = fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

internal fun Int.toDaysOfWeek(): Set<DayOfWeek> = DayOfWeek.entries.filter { day -> this and (1 shl (day.value - 1)) != 0 }.toSet()

private fun LocalTime.toMinuteOfDay(): Int = hour * MINUTES_PER_HOUR + minute

private fun Int.toLocalTime(): LocalTime = LocalTime.of(this / MINUTES_PER_HOUR, this % MINUTES_PER_HOUR)
