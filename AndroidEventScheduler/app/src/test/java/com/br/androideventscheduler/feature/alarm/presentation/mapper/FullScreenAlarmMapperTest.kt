package com.br.androideventscheduler.feature.alarm.presentation.mapper

import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.common.testing.FakeStringProvider
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.recurrence.RecurrenceEngine
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class FullScreenAlarmMapperTest {
    // 2026-11-07 12:00 em São Paulo.
    private val clock = FakeClock(Instant.parse("2026-11-07T15:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val mapper = FullScreenAlarmMapper(FakeStringProvider(), RecurrenceEngine(), clock)
    private val today = LocalDate.of(2026, 11, 7)

    private fun minutesUntil(
        startTime: LocalTime,
        startDate: LocalDate = today,
    ): Int = mapper.map(scheduleItem(startDate = startDate, startTime = startTime), 10).minutesUntilStart

    @Test
    fun `given event starting in fifteen minutes when mapping then minutes until start is fifteen`() {
        assertEquals(15, minutesUntil(LocalTime.of(12, 15)))
    }

    @Test
    fun `given alarm that arrived a few seconds late when mapping then minutes are rounded to the nearest`() {
        clock.setInstant(Instant.parse("2026-11-07T15:00:05Z"))

        assertEquals(15, minutesUntil(LocalTime.of(12, 15)))
    }

    @Test
    fun `given event that already started when mapping then minutes until start is zero`() {
        assertEquals(0, minutesUntil(LocalTime.of(11, 30)))
    }

    @Test
    fun `given event on a past date when mapping then minutes until start is zero`() {
        assertEquals(0, minutesUntil(LocalTime.of(12, 15), startDate = today.minusDays(3)))
    }

    @Test
    fun `given daily item whose time passed more than an hour ago when mapping then it counts to tomorrow`() {
        val item =
            scheduleItem(startDate = today.minusDays(10), startTime = LocalTime.of(9, 0), recurrencePattern = RecurrencePattern.Daily())

        assertEquals(21 * 60, mapper.map(item, 10).minutesUntilStart)
    }

    @Test
    fun `given daily item that started minutes ago when mapping then it counts as now`() {
        val item =
            scheduleItem(startDate = today.minusDays(10), startTime = LocalTime.of(11, 50), recurrencePattern = RecurrencePattern.Daily())

        assertEquals(0, mapper.map(item, 10).minutesUntilStart)
    }

    @Test
    fun `given all day item when mapping then it counts to nine in the morning`() {
        val item = scheduleItem(startDate = today.plusDays(1), startTime = LocalTime.MIDNIGHT, isAllDay = true)

        assertEquals(21 * 60, mapper.map(item, 10).minutesUntilStart)
    }

    @Test
    fun `given item with end time when mapping then time text is the range`() {
        val item = scheduleItem(startTime = LocalTime.of(22, 0), endTime = LocalTime.of(23, 30))

        val timeText = mapper.map(item, 10).timeText

        assertEquals("string(${R.string.alarm_time_range})[22:00, 23:30]", timeText)
    }

    @Test
    fun `given item without end time when mapping then time text is only the start`() {
        assertEquals("22:00", mapper.map(scheduleItem(startTime = LocalTime.of(22, 0)), 10).timeText)
    }

    @Test
    fun `given all day item when mapping then time text is the all day label`() {
        val item = scheduleItem(isAllDay = true)

        assertEquals("string(${R.string.alarm_time_all_day})", mapper.map(item, 10).timeText)
    }

    @Test
    fun `given blank location and notes when mapping then they are null`() {
        val content = mapper.map(scheduleItem(location = " ", notes = ""), 10)

        assertNull(content.location)
        assertNull(content.notes)
    }

    @Test
    fun `given location and notes when mapping then they are kept`() {
        val content = mapper.map(scheduleItem(location = "Sala 3", notes = "Levar o notebook"), 10)

        assertEquals("Sala 3", content.location)
        assertEquals("Levar o notebook", content.notes)
    }

    @Test
    fun `given unsupported default snooze when mapping then selected falls back to ten`() {
        assertEquals(10, mapper.map(scheduleItem(), 42).selectedSnoozeMinutes)
        assertEquals(15, mapper.map(scheduleItem(), 15).selectedSnoozeMinutes)
    }
}
