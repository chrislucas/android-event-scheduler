package com.br.androideventscheduler.feature.upsert.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleDraftTimeRulesTest {
    private fun draft(
        start: LocalDateTime,
        end: LocalDateTime,
        isAllDay: Boolean = false,
    ) = ScheduleDraftTimes(
        startDate = start.toLocalDate(),
        startTime = start.toLocalTime(),
        endDate = end.toLocalDate(),
        endTime = end.toLocalTime(),
        isAllDay = isAllDay,
    )

    private fun at(
        day: Int,
        hour: Int,
        minute: Int = 0,
    ) = LocalDateTime.of(2026, 11, day, hour, minute)

    @Test
    fun `given one hour event when moving the start to another day then the end keeps the one hour duration`() {
        val original = draft(start = at(5, 10), end = at(5, 11))

        val result = ScheduleDraftTimeRules.withNewStart(original, newStart = at(8, 14, 30))

        assertEquals(draft(start = at(8, 14, 30), end = at(8, 15, 30)), result)
    }

    @Test
    fun `given multi day event when moving the start then the end keeps the duration across days`() {
        val original = draft(start = at(5, 22), end = at(6, 23, 30))

        val result = ScheduleDraftTimeRules.withNewStart(original, newStart = at(10, 8))

        assertEquals(draft(start = at(10, 8), end = at(11, 9, 30)), result)
    }

    @Test
    fun `given new start close to midnight when keeping duration then the end rolls over to the next day`() {
        val original = draft(start = at(5, 10), end = at(5, 11))

        val result = ScheduleDraftTimeRules.withNewStart(original, newStart = at(5, 23, 30))

        assertEquals(draft(start = at(5, 23, 30), end = at(6, 0, 30)), result)
    }

    @Test
    fun `given end after start when changing the end then the new end is kept`() {
        val original = draft(start = at(5, 10), end = at(5, 11))

        val result = ScheduleDraftTimeRules.withNewEnd(original, newEnd = at(7, 18))

        assertEquals(draft(start = at(5, 10), end = at(7, 18)), result)
    }

    @Test
    fun `given new end before start when changing the end then the end is clamped to the start`() {
        val original = draft(start = at(5, 10), end = at(5, 11))

        val result = ScheduleDraftTimeRules.withNewEnd(original, newEnd = at(5, 9))

        assertEquals(draft(start = at(5, 10), end = at(5, 10)), result)
    }

    @Test
    fun `given new end date before start date when changing the end then the end is clamped to the start`() {
        val original = draft(start = at(5, 10), end = at(5, 11))

        val result = ScheduleDraftTimeRules.withNewEnd(original, newEnd = at(3, 15))

        assertEquals(draft(start = at(5, 10), end = at(5, 10)), result)
    }

    @Test
    fun `given all day draft when the end date equals the start date then clock times are ignored and nothing is clamped`() {
        val original = draft(start = at(5, 10), end = at(6, 11), isAllDay = true)

        val result = ScheduleDraftTimeRules.withNewEnd(original, newEnd = at(5, 8))

        assertEquals(draft(start = at(5, 10), end = at(5, 8), isAllDay = true), result)
    }

    @Test
    fun `given all day draft when the end date is before the start date then the end date is clamped to the start date`() {
        val original = draft(start = at(5, 10), end = at(6, 11), isAllDay = true)

        val result = ScheduleDraftTimeRules.withNewEnd(original, newEnd = at(4, 8))

        assertEquals(LocalDate.of(2026, 11, 5), result.endDate)
    }

    @Test
    fun `given today when asking for quick date offsets then it adds the days`() {
        val today = LocalDate.of(2026, 11, 28)

        assertEquals(today, ScheduleDraftTimeRules.quickDate(daysOffset = 0, today = today))
        assertEquals(LocalDate.of(2026, 11, 29), ScheduleDraftTimeRules.quickDate(daysOffset = 1, today = today))
        assertEquals(LocalDate.of(2026, 12, 1), ScheduleDraftTimeRules.quickDate(daysOffset = 3, today = today))
        assertEquals(LocalDate.of(2026, 12, 5), ScheduleDraftTimeRules.quickDate(daysOffset = 7, today = today))
    }

    @Test
    fun `given now when asking for in one hour then it adds an hour and drops seconds`() {
        val now = LocalDateTime.of(2026, 11, 5, 10, 20, 45)

        assertEquals(at(5, 11, 20), ScheduleDraftTimeRules.inOneHour(now))
    }

    @Test
    fun `given now close to midnight when asking for in one hour then the date moves to the next day`() {
        val now = LocalDateTime.of(2026, 11, 30, 23, 30)

        assertEquals(LocalDateTime.of(2026, 12, 1, 0, 30), ScheduleDraftTimeRules.inOneHour(now))
    }

    @Test
    fun `given now in the middle of an hour when asking for the default start then it is the next full hour`() {
        assertEquals(at(5, 11), ScheduleDraftTimeRules.nextFullHour(at(5, 10, 20)))
        assertEquals(at(5, 11), ScheduleDraftTimeRules.nextFullHour(at(5, 10)))
    }

    @Test
    fun `given now after 23h when asking for the default start then it rolls over to midnight of the next day`() {
        assertEquals(LocalDateTime.of(2026, 12, 1, 0, 0), ScheduleDraftTimeRules.nextFullHour(LocalDateTime.of(2026, 11, 30, 23, 10)))
    }

    @Test
    fun `given reminder draft when switching to event then the end is one hour after the start`() {
        val reminder = draft(start = at(5, 23, 30), end = at(5, 23, 30))

        val result = ScheduleDraftTimeRules.switchToEvent(reminder)

        assertEquals(draft(start = at(5, 23, 30), end = at(6, 0, 30)), result)
    }

    @Test
    fun `given event draft when switching to reminder then the end collapses into the start and all day is dropped`() {
        val event = draft(start = at(5, 10), end = at(6, 12), isAllDay = true)

        val result = ScheduleDraftTimeRules.switchToReminder(event)

        assertEquals(draft(start = at(5, 10), end = at(5, 10), isAllDay = false), result)
    }

    @Test
    fun `given any draft when switching modes then the start date and time are preserved`() {
        val original = draft(start = at(5, 7, 45), end = at(5, 9))

        val reminder = ScheduleDraftTimeRules.switchToReminder(original)
        val event = ScheduleDraftTimeRules.switchToEvent(reminder)

        assertEquals(LocalTime.of(7, 45), event.startTime)
        assertEquals(LocalDate.of(2026, 11, 5), event.startDate)
    }
}
