package com.br.androideventscheduler.feature.search.presentation.mapper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.common.AndroidStringProvider
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
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
class SearchResultMapperTest {
    private val mapper = SearchResultMapper(AndroidStringProvider(ApplicationProvider.getApplicationContext<Context>()))
    private val accountEmail = "bia@exemplo.com"

    private fun mapOne(item: ScheduleItem) = mapper.map(listOf(item), accountEmail).single()

    @Test
    fun `given a simple event when mapping then fields are formatted and there is no recurrence text`() {
        val item =
            scheduleItem(
                id = "42",
                title = "Reunião",
                startDate = LocalDate.of(2026, 11, 5),
                startTime = LocalTime.of(22, 0),
            ).copy(colorHex = 0xFF26A69A)

        val result = mapOne(item)

        assertEquals("42", result.id)
        assertEquals("Reunião", result.title)
        assertEquals(0xFF26A69A, result.colorHex)
        assertEquals("qui., 5 de nov., 22:00", result.dateTimeText)
        assertEquals("Evento", result.typeText)
        assertNull(result.recurrenceText)
        assertNull(result.stickerEmoji)
    }

    @Test
    fun `given a reminder when mapping then type text is Lembrete`() {
        val result = mapOne(scheduleItem(type = ScheduleType.REMINDER))

        assertEquals("Lembrete", result.typeText)
    }

    @Test
    fun `given a recurring item when mapping then the repetition label is exposed`() {
        val item = scheduleItem(recurrencePattern = RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)))

        val result = mapOne(item)

        assertEquals("Toda semana em ter., qui.", result.recurrenceText)
    }

    @Test
    fun `given a daily item with limit when mapping then the label includes the count`() {
        val result = mapOne(scheduleItem(recurrencePattern = RecurrencePattern.Daily(occurrenceLimit = 1)))

        assertEquals("Diariamente (1 vez)", result.recurrenceText)
    }

    @Test
    fun `given a sticker when mapping then it is carried to the result`() {
        val result = mapOne(scheduleItem().copy(stickerEmoji = "🚀"))

        assertEquals("🚀", result.stickerEmoji)
    }

    @Test
    fun `given an all day item when mapping then the date text says all day`() {
        val result = mapOne(scheduleItem(startDate = LocalDate.of(2026, 11, 5), isAllDay = true))

        assertEquals("qui., 5 de nov., dia inteiro", result.dateTimeText)
    }

    @Test
    fun `given each calendar source when mapping then labels use the account email or resources`() {
        val labels =
            CalendarSource.entries.map { source ->
                mapOne(scheduleItem().copy(calendarSource = source)).calendarLabel
            }

        assertEquals(listOf(accountEmail, "Family", "Meus lembretes"), labels)
    }

    @Test
    fun `given several items when mapping then order is preserved`() {
        val items = listOf(scheduleItem(id = "b"), scheduleItem(id = "a"))

        assertEquals(listOf("b", "a"), mapper.map(items, accountEmail).map { it.id })
    }
}
