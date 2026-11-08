package com.br.androideventscheduler.feature.home.domain.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class MonthGridBuilderTest {
    private val builder = MonthGridBuilder()

    @Test
    fun `given any month when building then the grid has 42 consecutive dates`() {
        val months = listOf(YearMonth.of(2026, 2), YearMonth.of(2026, 11), YearMonth.of(2027, 8), YearMonth.of(2024, 2))

        months.forEach { month ->
            val grid = builder.build(month)

            assertEquals(42, grid.size)
            grid.zipWithNext().forEach { (current, next) -> assertEquals(current.plusDays(1), next) }
        }
    }

    @Test
    fun `given november 2026 which starts on a sunday when building then the grid starts on the first day`() {
        val grid = builder.build(YearMonth.of(2026, 11))

        assertEquals(LocalDate.of(2026, 11, 1), grid.first())
        assertEquals(LocalDate.of(2026, 12, 12), grid.last())
    }

    @Test
    fun `given a month starting on a saturday when building then six days of the previous month lead the grid`() {
        val grid = builder.build(YearMonth.of(2026, 8))

        assertEquals(LocalDate.of(2026, 8, 1).dayOfWeek, DayOfWeek.SATURDAY)
        assertEquals(LocalDate.of(2026, 7, 26), grid.first())
        assertEquals(LocalDate.of(2026, 7, 26).dayOfWeek, DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 8, 1), grid[6])
    }

    @Test
    fun `given a leap february when building then it contains the 29th and keeps 42 dates`() {
        val grid = builder.build(YearMonth.of(2028, 2))

        assertEquals(42, grid.size)
        assertEquals(true, LocalDate.of(2028, 2, 29) in grid)
        assertEquals(29, grid.count { it.monthValue == 2 })
    }

    @Test
    fun `given a february starting on the first column when building then trailing days come from march`() {
        val grid = builder.build(YearMonth.of(2026, 2))

        assertEquals(LocalDate.of(2026, 2, 1), grid.first())
        assertEquals(LocalDate.of(2026, 3, 14), grid.last())
    }

    @Test
    fun `given monday as first day of week when building then the grid starts on a monday`() {
        val grid = MonthGridBuilder(firstDayOfWeek = DayOfWeek.MONDAY).build(YearMonth.of(2026, 11))

        assertEquals(LocalDate.of(2026, 10, 26), grid.first())
        assertEquals(DayOfWeek.MONDAY, grid.first().dayOfWeek)
        assertEquals(42, grid.size)
    }
}
