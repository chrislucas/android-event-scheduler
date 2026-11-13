package com.br.androideventscheduler.feature.backup.data.mapper

import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.feature.backup.data.dto.RecurrenceDto
import com.br.androideventscheduler.feature.backup.data.dto.ScheduleItemDto
import com.br.androideventscheduler.feature.backup.data.fullyPopulatedItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class AgendaDtoMapperTest {
    private fun dto(
        id: String = "i1",
        title: String = "Reunião",
        type: String = "EVENT",
        startDate: String = "2026-11-05",
        endDate: String? = "2026-11-05",
        startTime: String = "10:00",
        endTime: String? = null,
        location: String = "",
        notes: String = "",
        colorHex: String? = null,
        calendarSource: String? = null,
        reminderOffset: String? = null,
        recurrence: RecurrenceDto = RecurrenceDto(),
        stickerEmoji: String? = null,
    ) = ScheduleItemDto(
        id = id,
        title = title,
        type = type,
        startDate = startDate,
        endDate = endDate,
        startTime = startTime,
        endTime = endTime,
        location = location,
        notes = notes,
        colorHex = colorHex,
        calendarSource = calendarSource,
        reminderOffset = reminderOffset,
        recurrence = recurrence,
        stickerEmoji = stickerEmoji,
    )

    private fun map(dto: ScheduleItemDto): ScheduleItem? = AgendaDtoMapper.toDomainOrNull(dto)

    @Test
    fun `given every kind of item when mapping to dto and back then the domain item is identical`() {
        fullyPopulatedItems().forEach { item ->
            assertEquals(item, map(AgendaDtoMapper.toDto(item)))
        }
    }

    @Test
    fun `given a valid minimal dto when mapping then defaults fill the optional fields`() {
        val item = map(dto(endDate = null, type = "REMINDER"))

        assertNotNull(item)
        assertEquals(LocalDate.of(2026, 11, 5), item!!.endDate)
        assertEquals(CalendarSource.MY_REMINDERS, item.calendarSource)
        assertEquals(ReminderOffset.DEFAULT, item.reminderOffset)
        assertEquals(RecurrencePattern.None, item.recurrencePattern)
    }

    @Test
    fun `given invalid dates when mapping then the item is null`() {
        assertNull(map(dto(startDate = "2026-02-30")))
        assertNull(map(dto(startDate = "05/11/2026")))
        assertNull(map(dto(endDate = "ontem")))
        assertNull(map(dto(startDate = "9999-01-01", endDate = "9999-01-02")))
    }

    @Test
    fun `given end date before start date when mapping then the item is null`() {
        assertNull(map(dto(startDate = "2026-11-05", endDate = "2026-11-04")))
    }

    @Test
    fun `given invalid times when mapping then the item is null`() {
        assertNull(map(dto(startTime = "25:00")))
        assertNull(map(dto(startTime = "10h")))
        assertNull(map(dto(startTime = "10:00:30")))
        assertNull(map(dto(endTime = "99:99")))
    }

    @Test
    fun `given five minute reminder name when mapping then the item keeps the five minute offset`() {
        assertEquals(ReminderOffset.FIVE_MINUTES, map(dto(reminderOffset = "FIVE_MINUTES"))?.reminderOffset)
    }

    @Test
    fun `given item with five minute reminder when mapping to dto and back then the name is stable and it round trips`() {
        val item = map(dto())!!.copy(reminderOffset = ReminderOffset.FIVE_MINUTES)

        val exported = AgendaDtoMapper.toDto(item)

        assertEquals("FIVE_MINUTES", exported.reminderOffset)
        assertEquals(item, map(exported))
    }

    @Test
    fun `given unknown enum names when mapping then the item is null`() {
        assertNull(map(dto(type = "MEETING")))
        assertNull(map(dto(type = "event")))
        assertNull(map(dto(calendarSource = "work_calendar")))
        assertNull(map(dto(reminderOffset = "TWO_MINUTES")))
        assertNull(map(dto(recurrence = RecurrenceDto(type = "MONTHLY"))))
        assertNull(map(dto(recurrence = RecurrenceDto(type = "WEEKLY", daysOfWeek = listOf("FUNDAY")))))
    }

    @Test
    fun `given weekly recurrence without days when mapping then the item is null`() {
        assertNull(map(dto(recurrence = RecurrenceDto(type = "WEEKLY", daysOfWeek = emptyList()))))
    }

    @Test
    fun `given occurrence limit below one when mapping then the item is null`() {
        assertNull(map(dto(recurrence = RecurrenceDto(type = "DAILY", occurrenceLimit = 0))))
        assertNull(map(dto(recurrence = RecurrenceDto(type = "WEEKLY", daysOfWeek = listOf("MONDAY"), occurrenceLimit = -2))))
    }

    @Test
    fun `given weekly recurrence with days when mapping then the pattern keeps days and limit`() {
        val item = map(dto(recurrence = RecurrenceDto("WEEKLY", listOf("TUESDAY", "THURSDAY"), occurrenceLimit = 4)))

        assertEquals(RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), 4), item!!.recurrencePattern)
    }

    @Test
    fun `given blank or oversized id or blank title when mapping then the item is null`() {
        assertNull(map(dto(id = "")))
        assertNull(map(dto(id = "   ")))
        assertNull(map(dto(id = "x".repeat(101))))
        assertNull(map(dto(title = "   ")))
    }

    @Test
    fun `given oversized strings when mapping then they are truncated to the field limits`() {
        val item = map(dto(title = "t".repeat(500), location = "l".repeat(500), notes = "n".repeat(9000)))

        assertEquals(200, item!!.title.length)
        assertEquals(200, item.location.length)
        assertEquals(4000, item.notes.length)
    }

    @Test
    fun `given a title cut inside an emoji when truncating then the surrogate pair is not split`() {
        val item = map(dto(title = "a".repeat(199) + "😀😀"))

        assertEquals("a".repeat(199), item!!.title)
    }

    @Test
    fun `given sticker that is blank or too long when mapping then it is dropped`() {
        assertNull(map(dto(stickerEmoji = "  "))!!.stickerEmoji)
        assertNull(map(dto(stickerEmoji = "x".repeat(40)))!!.stickerEmoji)
        assertEquals("🎉", map(dto(stickerEmoji = "🎉"))!!.stickerEmoji)
    }

    @Test
    fun `given color in accepted forms when mapping then it becomes an opaque or explicit argb`() {
        assertEquals(0xFF29B6F6, map(dto(colorHex = "#FF29B6F6"))!!.colorHex)
        assertEquals(0xFF29B6F6, map(dto(colorHex = "#29b6f6"))!!.colorHex)
        assertEquals(0x8029B6F6, map(dto(colorHex = "#8029B6F6"))!!.colorHex)
    }

    @Test
    fun `given malformed color when mapping then the item is null`() {
        assertNull(map(dto(colorHex = "azul")))
        assertNull(map(dto(colorHex = "#GGGGGG")))
        assertNull(map(dto(colorHex = "#FFF")))
        assertNull(map(dto(colorHex = "#-1000000")))
    }

    @Test
    fun `given an item when mapping to dto then times and dates use the documented formats`() {
        val item =
            ScheduleItem(
                id = "a",
                title = "t",
                type = ScheduleType.EVENT,
                startDate = LocalDate.of(2026, 1, 2),
                startTime = LocalTime.of(7, 5),
                endTime = LocalTime.of(23, 59),
            )

        val result = AgendaDtoMapper.toDto(item)

        assertEquals("2026-01-02", result.startDate)
        assertEquals("07:05", result.startTime)
        assertEquals("23:59", result.endTime)
        assertEquals("#FF29B6F6", result.colorHex)
    }
}
