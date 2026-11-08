package com.br.androideventscheduler.feature.upsert.presentation.component

import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RecurrenceDraftTest {
    // 2027-01-21 é uma quinta-feira.
    private val thursdayStart: LocalDate = LocalDate.of(2027, 1, 21)

    private fun draft(
        kind: RecurrenceKind = RecurrenceKind.NONE,
        days: Set<DayOfWeek> = setOf(DayOfWeek.THURSDAY),
        countText: String = "",
    ) = RecurrenceDraft(kind = kind, selectedDays = days, countText = countText)

    // region from

    @Test
    fun `given no recurrence when creating the draft then it is NONE with the start date weekday and no count`() {
        val result = RecurrenceDraft.from(RecurrencePattern.None, thursdayStart)

        assertEquals(RecurrenceKind.NONE, result.kind)
        assertEquals(setOf(DayOfWeek.THURSDAY), result.selectedDays)
        assertEquals("", result.countText)
    }

    @Test
    fun `given a daily pattern with limit when creating the draft then the count is filled as text`() {
        val result = RecurrenceDraft.from(RecurrencePattern.Daily(occurrenceLimit = 12), thursdayStart)

        assertEquals(RecurrenceKind.DAILY, result.kind)
        assertEquals("12", result.countText)
        assertEquals(setOf(DayOfWeek.THURSDAY), result.selectedDays)
    }

    @Test
    fun `given a daily pattern without limit when creating the draft then the count is empty`() {
        val result = RecurrenceDraft.from(RecurrencePattern.Daily(), thursdayStart)

        assertEquals(RecurrenceKind.DAILY, result.kind)
        assertEquals("", result.countText)
    }

    @Test
    fun `given a weekly pattern when creating the draft then days and limit come from the pattern`() {
        val pattern = RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), occurrenceLimit = 5000)

        val result = RecurrenceDraft.from(pattern, thursdayStart)

        assertEquals(RecurrenceKind.WEEKLY, result.kind)
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), result.selectedDays)
        assertEquals("5000", result.countText)
    }

    // endregion

    // region withKind

    @Test
    fun `given none draft when switching to weekly for the first time then the start date weekday is selected`() {
        val result = RecurrenceDraft.from(RecurrencePattern.None, thursdayStart).withKind(RecurrenceKind.WEEKLY)

        assertEquals(RecurrenceKind.WEEKLY, result.kind)
        assertEquals(setOf(DayOfWeek.THURSDAY), result.selectedDays)
    }

    @Test
    fun `given selected days when switching kind back and forth then the days are kept`() {
        val result =
            draft(kind = RecurrenceKind.WEEKLY, days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
                .withKind(RecurrenceKind.DAILY)
                .withKind(RecurrenceKind.WEEKLY)

        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), result.selectedDays)
    }

    // endregion

    // region toggleDay

    @Test
    fun `given a selected day when toggling another unselected day then both are selected`() {
        val result = draft(kind = RecurrenceKind.WEEKLY).toggleDay(DayOfWeek.TUESDAY)

        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), result.selectedDays)
    }

    @Test
    fun `given two selected days when toggling one of them then it is removed`() {
        val result =
            draft(kind = RecurrenceKind.WEEKLY, days = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY))
                .toggleDay(DayOfWeek.TUESDAY)

        assertEquals(setOf(DayOfWeek.THURSDAY), result.selectedDays)
    }

    @Test
    fun `given a single selected day when toggling it then the last day is not removed`() {
        val result = draft(kind = RecurrenceKind.WEEKLY).toggleDay(DayOfWeek.THURSDAY)

        assertEquals(setOf(DayOfWeek.THURSDAY), result.selectedDays)
    }

    // endregion

    // region withCountText

    @Test
    fun `given raw text with non digits when setting the count then only digits are kept`() {
        val result = draft().withCountText("1a2-b3 .")

        assertEquals("123", result.countText)
    }

    @Test
    fun `given more than four digits when setting the count then it is truncated to four`() {
        val result = draft().withCountText("123456")

        assertEquals("1234", result.countText)
    }

    @Test
    fun `given empty text when setting the count then it stays empty`() {
        val result = draft(countText = "7").withCountText("")

        assertEquals("", result.countText)
    }

    // endregion

    // region toPattern

    @Test
    fun `given kind none when converting then it is None even with a count and days`() {
        val result = draft(kind = RecurrenceKind.NONE, countText = "9").toPattern()

        assertEquals(RecurrencePattern.None, result)
    }

    @Test
    fun `given daily with empty count when converting then the limit is null`() {
        val result = draft(kind = RecurrenceKind.DAILY, countText = "").toPattern()

        assertEquals(RecurrencePattern.Daily(occurrenceLimit = null), result)
    }

    @Test
    fun `given daily with zero count when converting then the limit is null`() {
        val result = draft(kind = RecurrenceKind.DAILY, countText = "0").toPattern()

        assertEquals(RecurrencePattern.Daily(occurrenceLimit = null), result)
    }

    @Test
    fun `given daily with leading zeros when converting then the number is parsed`() {
        val result = draft(kind = RecurrenceKind.DAILY, countText = "007").toPattern()

        assertEquals(RecurrencePattern.Daily(occurrenceLimit = 7), result)
    }

    @Test
    fun `given weekly with two days and 5000 when converting then it is Weekly with limit 5000`() {
        val result =
            draft(kind = RecurrenceKind.WEEKLY, days = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), countText = "5000")
                .toPattern()

        assertEquals(RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), occurrenceLimit = 5000), result)
    }

    @Test
    fun `given weekly with empty count when converting then the limit is null`() {
        val result = draft(kind = RecurrenceKind.WEEKLY).toPattern()

        assertEquals(RecurrencePattern.Weekly(setOf(DayOfWeek.THURSDAY), occurrenceLimit = null), result)
    }

    // endregion
}
