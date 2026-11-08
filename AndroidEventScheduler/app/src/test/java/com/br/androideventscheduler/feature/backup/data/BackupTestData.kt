package com.br.androideventscheduler.feature.backup.data

import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Itens que exercitam todos os campos e padrões de recorrência, para testes de round-trip. */
internal fun fullyPopulatedItems(): List<ScheduleItem> =
    listOf(
        ScheduleItem(
            id = "event-weekly",
            title = "Treino de corrida",
            type = ScheduleType.EVENT,
            startDate = LocalDate.of(2026, 11, 2),
            endDate = LocalDate.of(2026, 11, 3),
            startTime = LocalTime.of(6, 30),
            endTime = LocalTime.of(7, 45),
            isAllDay = false,
            location = "Parque Ibirapuera",
            notes = "Levar água\ne tênis novo — \"importante\"",
            colorHex = 0xFF7E57C2,
            calendarSource = CalendarSource.FAMILY,
            reminderOffset = ReminderOffset.THIRTY_MINUTES,
            recurrencePattern = RecurrencePattern.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), occurrenceLimit = 8),
            stickerEmoji = "🏃",
        ),
        ScheduleItem(
            id = "reminder-daily",
            title = "Tomar remédio",
            type = ScheduleType.REMINDER,
            startDate = LocalDate.of(2026, 11, 1),
            startTime = LocalTime.of(8, 0),
            calendarSource = CalendarSource.MY_REMINDERS,
            reminderOffset = ReminderOffset.NONE,
            recurrencePattern = RecurrencePattern.Daily(occurrenceLimit = null),
        ),
        ScheduleItem(
            id = "all-day-plain",
            title = "Feriado",
            type = ScheduleType.EVENT,
            startDate = LocalDate.of(2026, 11, 15),
            startTime = LocalTime.MIDNIGHT,
            isAllDay = true,
            calendarSource = CalendarSource.PERSONAL_ACCOUNT,
            reminderOffset = ReminderOffset.ONE_HOUR,
            recurrencePattern = RecurrencePattern.Daily(occurrenceLimit = 3),
        ),
        ScheduleItem(
            id = "plain",
            title = "Dentista",
            type = ScheduleType.EVENT,
            startDate = LocalDate.of(2026, 12, 1),
            startTime = LocalTime.of(14, 0),
            reminderOffset = ReminderOffset.TEN_MINUTES,
        ),
    )

/** JSON mínimo válido de um item, com campos sobrescritíveis para montar casos inválidos. */
internal fun itemJson(
    id: String = "i1",
    title: String = "\"Reunião\"",
    type: String = "\"EVENT\"",
    startDate: String = "\"2026-11-05\"",
    endDate: String = "\"2026-11-05\"",
    startTime: String = "\"10:00\"",
    extra: String = "",
): String = """{"id":"$id","title":$title,"type":$type,"startDate":$startDate,"endDate":$endDate,"startTime":$startTime$extra}"""

internal fun fileJson(
    items: String,
    format: String = "\"rememberme-agenda\"",
    version: String = "1",
): String = """{"format":$format,"version":$version,"exportedAt":"2026-11-08T12:00:00Z","appVersion":"1.0","items":[$items]}"""
