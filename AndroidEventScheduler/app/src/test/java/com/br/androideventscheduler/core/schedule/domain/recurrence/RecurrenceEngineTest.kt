package com.br.androideventscheduler.core.schedule.domain.recurrence

import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleOccurrence
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class RecurrenceEngineTest {
    private val engine = RecurrenceEngine()

    // 2026-11-05 é uma quinta-feira.
    private val thursdayNov5 = LocalDate.of(2026, 11, 5)

    private fun item(
        pattern: RecurrencePattern,
        startDate: LocalDate = thursdayNov5,
        endDate: LocalDate = startDate,
        startTime: LocalTime = LocalTime.of(10, 0),
        id: String = "item",
    ) = ScheduleItem(
        id = id,
        title = "Item",
        type = ScheduleType.EVENT,
        startDate = startDate,
        endDate = endDate,
        startTime = startTime,
        recurrencePattern = pattern,
    )

    @Test
    fun `given non recurring item when listing occurrences then only the start date is returned`() {
        val dates = engine.occurrenceDates(item(RecurrencePattern.None)).toList()

        assertEquals(listOf(thursdayNov5), dates)
    }

    @Test
    fun `given daily without limit when taking five occurrences then they are consecutive days`() {
        val dates = engine.occurrenceDates(item(RecurrencePattern.Daily())).take(5).toList()

        assertEquals((0L..4L).map { thursdayNov5.plusDays(it) }, dates)
    }

    @Test
    fun `given daily with limit three when listing occurrences then the series has exactly three`() {
        val dates = engine.occurrenceDates(item(RecurrencePattern.Daily(occurrenceLimit = 3))).toList()

        assertEquals(listOf(thursdayNov5, thursdayNov5.plusDays(1), thursdayNov5.plusDays(2)), dates)
    }

    @Test
    fun `given weekly on tuesday and thursday when listing then only those weekdays appear`() {
        val pattern = RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY))

        val dates = engine.occurrenceDates(item(pattern)).take(4).toList()

        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 5),
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12),
                LocalDate.of(2026, 11, 17),
            ),
            dates,
        )
    }

    @Test
    fun `given weekly whose start date is not a marked day when listing then first occurrence moves forward`() {
        val pattern = RecurrencePattern.Weekly(setOf(DayOfWeek.MONDAY), occurrenceLimit = 2)

        val dates = engine.occurrenceDates(item(pattern)).toList()

        assertEquals(listOf(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 16)), dates)
    }

    @Test
    fun `given limit of five thousand when consuming the whole series then it ends at the limit`() {
        val pattern = RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), occurrenceLimit = 5000)

        val dates = engine.occurrenceDates(item(pattern)).toList()

        assertEquals(5000, dates.size)
        assertTrue(dates.zipWithNext().all { (previous, next) -> next > previous })
    }

    @Test
    fun `given on or after in the middle of a limited series when listing then counting still starts at the first`() {
        val dailyLimitedToFive = item(RecurrencePattern.Daily(occurrenceLimit = 5))

        val dates = engine.occurrenceDates(dailyLimitedToFive, onOrAfter = thursdayNov5.plusDays(3)).toList()

        assertEquals(listOf(thursdayNov5.plusDays(3), thursdayNov5.plusDays(4)), dates)
    }

    @Test
    fun `given multi day daily item when expanding then each occurrence keeps the duration`() {
        val twoDayItem = item(RecurrencePattern.Daily(occurrenceLimit = 2), endDate = thursdayNov5.plusDays(1))

        val occurrences = engine.expand(twoDayItem, from = thursdayNov5, to = thursdayNov5.plusDays(10))

        assertEquals(
            listOf(
                ScheduleOccurrence(twoDayItem, thursdayNov5, thursdayNov5.plusDays(1)),
                ScheduleOccurrence(twoDayItem, thursdayNov5.plusDays(1), thursdayNov5.plusDays(2)),
            ),
            occurrences,
        )
    }

    @Test
    fun `given month boundary when expanding a window then occurrences overlapping the window are included`() {
        val oct30to31 = item(RecurrencePattern.None, startDate = LocalDate.of(2026, 10, 30), endDate = LocalDate.of(2026, 11, 1))

        val novemberOccurrences = engine.expand(oct30to31, from = LocalDate.of(2026, 11, 1), to = LocalDate.of(2026, 11, 30))
        val decemberOccurrences = engine.expand(oct30to31, from = LocalDate.of(2026, 12, 1), to = LocalDate.of(2026, 12, 31))

        assertEquals(1, novemberOccurrences.size)
        assertTrue(decemberOccurrences.isEmpty())
    }

    @Test
    fun `given daily series started months ago when expanding november then only november days are returned`() {
        val startedInJanuary = item(RecurrencePattern.Daily(), startDate = LocalDate.of(2026, 1, 1))

        val occurrences = engine.expand(startedInJanuary, from = LocalDate.of(2026, 11, 1), to = LocalDate.of(2026, 11, 30))

        assertEquals(30, occurrences.size)
        assertEquals(LocalDate.of(2026, 11, 1), occurrences.first().startDate)
        assertEquals(LocalDate.of(2026, 11, 30), occurrences.last().startDate)
    }

    @Test
    fun `given non recurring item outside window when expanding then nothing is returned`() {
        val occurrences = engine.expand(item(RecurrencePattern.None), from = LocalDate.of(2026, 12, 1), to = LocalDate.of(2026, 12, 31))

        assertTrue(occurrences.isEmpty())
    }
}
