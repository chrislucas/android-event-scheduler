package com.br.androideventscheduler.core.schedule.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ScheduleModelsTest {
    @Test
    fun `given weekly pattern without days when creating then it fails`() {
        assertThrows(IllegalArgumentException::class.java) { RecurrencePattern.Weekly(daysOfWeek = emptySet()) }
    }

    @Test
    fun `given occurrence limit below one when creating patterns then they fail`() {
        assertThrows(IllegalArgumentException::class.java) { RecurrencePattern.Daily(occurrenceLimit = 0) }
        assertThrows(IllegalArgumentException::class.java) {
            RecurrencePattern.Weekly(daysOfWeek = setOf(DayOfWeek.MONDAY), occurrenceLimit = -1)
        }
    }

    @Test
    fun `given unknown hex when resolving color option then falls back to light blue`() {
        assertEquals(EventColorOption.LIGHT_BLUE, EventColorOption.fromHex(0xFF123456))
        assertEquals(EventColorOption.CORAL, EventColorOption.fromHex(EventColorOption.CORAL.hex))
    }

    @Test
    fun `given schedule type when using default calendar source then reminder goes to my reminders`() {
        assertEquals(CalendarSource.MY_REMINDERS, CalendarSource.defaultFor(ScheduleType.REMINDER))
        assertEquals(CalendarSource.PERSONAL_ACCOUNT, CalendarSource.defaultFor(ScheduleType.EVENT))
    }

    @Test
    fun `given end date before start date when creating item then it fails`() {
        assertThrows(IllegalArgumentException::class.java) {
            ScheduleItem(
                id = "1",
                title = "Reunião",
                type = ScheduleType.EVENT,
                startDate = LocalDate.of(2026, 11, 5),
                endDate = LocalDate.of(2026, 11, 4),
                startTime = LocalTime.of(10, 0),
            )
        }
    }

    @Test
    fun `given catalogs when listing then they have the expected sizes and stable ids`() {
        assertEquals(6, EventColorOption.entries.size)
        assertEquals(8, StickerEmoji.entries.size)
        assertEquals(listOf("personal_account", "family_calendar", "my_reminders"), CalendarSource.entries.map { it.id })
        assertEquals(listOf(null, 5, 10, 15, 30, 60), ReminderOffset.entries.map { it.minutes })
    }

    @Test
    fun `given missing schedule id when creating not found exception then message carries the id`() {
        val exception = ScheduleNotFoundException("abc")

        assertEquals("abc", exception.scheduleId)
    }
}
