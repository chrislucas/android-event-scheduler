package com.br.androideventscheduler.core.schedule.data.mapper

import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.EventColorOption
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ScheduleMapperTest {
    private val fullItem =
        ScheduleItem(
            id = "abc",
            title = "Reunião de Equipe",
            type = ScheduleType.EVENT,
            startDate = LocalDate.of(2026, 11, 5),
            endDate = LocalDate.of(2026, 11, 6),
            startTime = LocalTime.of(22, 0),
            endTime = LocalTime.of(23, 30),
            isAllDay = false,
            location = "Sala Azul",
            notes = "Levar notebook",
            colorHex = EventColorOption.CORAL.hex,
            calendarSource = CalendarSource.FAMILY,
            reminderOffset = ReminderOffset.THIRTY_MINUTES,
            recurrencePattern = RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), occurrenceLimit = 5000),
            stickerEmoji = "🚀",
        )

    @Test
    fun `given item with every field when mapping to entity and back then nothing is lost`() {
        assertEquals(fullItem, fullItem.toEntity().toDomain())
    }

    @Test
    fun `given each recurrence pattern when round tripping then pattern is preserved`() {
        val patterns =
            listOf(
                RecurrencePattern.None,
                RecurrencePattern.Daily(),
                RecurrencePattern.Daily(occurrenceLimit = 3),
                RecurrencePattern.Weekly(DayOfWeek.entries.toSet()),
                RecurrencePattern.Weekly(setOf(DayOfWeek.SUNDAY)),
            )

        patterns.forEach { pattern ->
            val item = fullItem.copy(recurrencePattern = pattern)
            assertEquals(pattern, item.toEntity().toDomain().recurrencePattern)
        }
    }

    @Test
    fun `given days of week when building mask then monday is bit zero and sunday is bit six`() {
        assertEquals(0b0000001, setOf(DayOfWeek.MONDAY).toDaysMask())
        assertEquals(0b1000000, setOf(DayOfWeek.SUNDAY).toDaysMask())
        assertEquals(0b0001010, setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY).toDaysMask())
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), 0b0001010.toDaysOfWeek())
    }

    @Test
    fun `given reminder without alert when round tripping then offset stays none`() {
        val item = fullItem.copy(reminderOffset = ReminderOffset.NONE, endTime = null, stickerEmoji = null)

        assertEquals(item, item.toEntity().toDomain())
    }

    @Test
    fun `given five minute reminder when mapping then the entity stores five minutes and it round trips`() {
        val item = fullItem.copy(reminderOffset = ReminderOffset.FIVE_MINUTES)

        val entity = item.toEntity()

        assertEquals(5, entity.reminderMinutes)
        assertEquals(ReminderOffset.FIVE_MINUTES, entity.toDomain().reminderOffset)
        assertEquals(item, entity.toDomain())
    }

    @Test
    fun `given item when mapping to entity then search text is normalized title notes and location`() {
        assertEquals("reuniao de equipe levar notebook sala azul", fullItem.toEntity().searchText)
    }
}
