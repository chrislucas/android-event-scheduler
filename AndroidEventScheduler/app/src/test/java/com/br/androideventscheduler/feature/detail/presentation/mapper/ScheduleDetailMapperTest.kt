package com.br.androideventscheduler.feature.detail.presentation.mapper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.common.AndroidStringProvider
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR")
class ScheduleDetailMapperTest {
    private val mapper = ScheduleDetailMapper(AndroidStringProvider(ApplicationProvider.getApplicationContext<Context>()))
    private val accountEmail = "ana@exemplo.com"
    private val thursday = LocalDate.of(2026, 11, 5)

    private fun dateRangeOf(item: ScheduleItem): String = mapper.map(item, accountEmail).dateRangeText

    @Test
    fun `given event spanning two days with times when mapping then range uses an arrow between both ends`() {
        val item =
            scheduleItem(
                startDate = thursday,
                endDate = thursday.plusDays(1),
                startTime = LocalTime.of(22, 0),
                endTime = LocalTime.of(23, 30),
            )

        assertEquals("qui., 5 de nov., 22:00 → sex., 6 de nov., 23:30", dateRangeOf(item))
    }

    @Test
    fun `given event within one day when mapping then range shows only the end time after a dash`() {
        val item = scheduleItem(startDate = thursday, startTime = LocalTime.of(22, 0), endTime = LocalTime.of(23, 30))

        assertEquals("qui., 5 de nov., 22:00 – 23:30", dateRangeOf(item))
    }

    @Test
    fun `given single all day event when mapping then range shows the date and the all day suffix`() {
        val item = scheduleItem(startDate = thursday, isAllDay = true)

        assertEquals("qui., 5 de nov. · Todo o dia", dateRangeOf(item))
    }

    @Test
    fun `given multi day all day event when mapping then range shows both dates and the all day suffix`() {
        val item = scheduleItem(startDate = thursday, endDate = thursday.plusDays(2), isAllDay = true)

        assertEquals("qui., 5 de nov. → sáb., 7 de nov. · Todo o dia", dateRangeOf(item))
    }

    @Test
    fun `given reminder when mapping then range is the start date and time only`() {
        val item = scheduleItem(type = ScheduleType.REMINDER, startDate = thursday, startTime = LocalTime.of(22, 0))

        assertEquals("qui., 5 de nov., 22:00", dateRangeOf(item))
    }

    @Test
    fun `given event without end time when mapping then range is the start date and time only`() {
        val item = scheduleItem(startDate = thursday, startTime = LocalTime.of(9, 5), endTime = null)

        assertEquals("qui., 5 de nov., 09:05", dateRangeOf(item))
    }

    @Test
    fun `given recurring item when mapping then range shows the first occurrence of the series`() {
        val item =
            scheduleItem(
                startDate = thursday,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(9, 0),
                recurrencePattern = RecurrencePattern.Daily(),
            )

        assertEquals("qui., 5 de nov., 08:00 – 09:00", dateRangeOf(item))
    }

    @Test
    fun `given personal account calendar when mapping then label is the active account email`() {
        val item = scheduleItem().copy(calendarSource = CalendarSource.PERSONAL_ACCOUNT)

        val ui = mapper.map(item, accountEmail)

        assertEquals(accountEmail, ui.calendarLabel)
        assertEquals(CalendarSource.PERSONAL_ACCOUNT.colorHex, ui.calendarColorHex)
    }

    @Test
    fun `given family calendar when mapping then label is Family`() {
        val item = scheduleItem().copy(calendarSource = CalendarSource.FAMILY)

        assertEquals("Family", mapper.map(item, accountEmail).calendarLabel)
    }

    @Test
    fun `given my reminders calendar when mapping then label is Meus lembretes`() {
        val item = scheduleItem().copy(calendarSource = CalendarSource.MY_REMINDERS)

        assertEquals("Meus lembretes", mapper.map(item, accountEmail).calendarLabel)
    }

    @Test
    fun `given blank location and notes when mapping then both are omitted`() {
        val ui = mapper.map(scheduleItem(location = "", notes = "   "), accountEmail)

        assertNull(ui.location)
        assertNull(ui.notes)
    }

    @Test
    fun `given filled location and notes when mapping then both are kept`() {
        val ui = mapper.map(scheduleItem(location = "Sala 3", notes = "Levar o notebook"), accountEmail)

        assertEquals("Sala 3", ui.location)
        assertEquals("Levar o notebook", ui.notes)
    }

    @Test
    fun `given reminder offsets when mapping then labels follow the display format`() {
        val labels =
            listOf(ReminderOffset.NONE, ReminderOffset.TEN_MINUTES, ReminderOffset.ONE_HOUR).map { offset ->
                mapper.map(scheduleItem().copy(reminderOffset = offset), accountEmail).reminderText
            }

        assertEquals(listOf("Sem alerta", "10 minutos antes", "1 hora antes"), labels)
    }

    @Test
    fun `given five minute reminder when mapping then the label reads five minutes before`() {
        val ui = mapper.map(scheduleItem().copy(reminderOffset = ReminderOffset.FIVE_MINUTES), accountEmail)

        assertEquals("5 minutos antes", ui.reminderText)
    }

    @Test
    fun `given recurrence patterns when mapping then labels follow the display format`() {
        val patterns =
            listOf(
                RecurrencePattern.None,
                RecurrencePattern.Daily(),
                RecurrencePattern.Weekly(setOf(DayOfWeek.THURSDAY, DayOfWeek.TUESDAY), occurrenceLimit = 5000),
            )

        val labels = patterns.map { mapper.map(scheduleItem(recurrencePattern = it), accountEmail).recurrenceText }

        assertEquals(listOf("Não repetir", "Diariamente", "Toda semana em ter., qui. (5000 vezes)"), labels)
    }

    @Test
    fun `given sticker color and id when mapping then they are copied to the ui model`() {
        val item = scheduleItem(id = "abc").copy(stickerEmoji = "🚀", colorHex = 0xFFEF5350)

        val ui = mapper.map(item, accountEmail)

        assertEquals("abc", ui.id)
        assertEquals("🚀", ui.stickerEmoji)
        assertEquals(0xFFEF5350, ui.colorHex)
    }

    @Test
    fun `given recurring and non recurring items when mapping then only the recurring one is flagged`() {
        val recurring = mapper.map(scheduleItem(recurrencePattern = RecurrencePattern.Daily(occurrenceLimit = 3)), accountEmail)
        val single = mapper.map(scheduleItem(recurrencePattern = RecurrencePattern.None), accountEmail)

        assertEquals(true, recurring.isRecurring)
        assertEquals(false, single.isRecurring)
    }
}
