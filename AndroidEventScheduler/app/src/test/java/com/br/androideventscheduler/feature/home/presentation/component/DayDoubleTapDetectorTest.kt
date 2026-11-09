package com.br.androideventscheduler.feature.home.presentation.component

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DayDoubleTapDetectorTest {
    private val day = LocalDate.of(2026, 11, 5)
    private val otherDay = LocalDate.of(2026, 11, 6)
    private val timeout = 300L

    private fun detector() = DayDoubleTapDetector<LocalDate>(timeout)

    @Test
    fun `given two taps on same day within timeout when onTap then second is a double tap`() {
        val detector = detector()

        val results = listOf(detector.onTap(day, 1_000), detector.onTap(day, 1_200))

        assertEquals(listOf(false, true), results)
    }

    @Test
    fun `given two taps on same day after timeout when onTap then no double tap`() {
        val detector = detector()

        val results = listOf(detector.onTap(day, 1_000), detector.onTap(day, 1_301))

        assertEquals(listOf(false, false), results)
    }

    @Test
    fun `given second tap exactly at timeout when onTap then it is a double tap`() {
        val detector = detector()

        val results = listOf(detector.onTap(day, 1_000), detector.onTap(day, 1_300))

        assertEquals(listOf(false, true), results)
    }

    @Test
    fun `given quick taps on different days when onTap then no double tap`() {
        val detector = detector()

        val results = listOf(detector.onTap(day, 1_000), detector.onTap(otherDay, 1_100))

        assertEquals(listOf(false, false), results)
    }

    @Test
    fun `given three quick taps on same day when onTap then opens only once`() {
        val detector = detector()

        val results = listOf(detector.onTap(day, 1_000), detector.onTap(day, 1_100), detector.onTap(day, 1_200))

        assertEquals(listOf(false, true, false), results)
    }

    @Test
    fun `given four quick taps on same day when onTap then two double taps`() {
        val detector = detector()

        val results = (0..3).map { detector.onTap(day, 1_000L + it * 100) }

        assertEquals(listOf(false, true, false, true), results)
    }

    @Test
    fun `given time going backwards when onTap then no double tap`() {
        val detector = detector()

        val results = listOf(detector.onTap(day, 2_000), detector.onTap(day, 1_900))

        assertEquals(listOf(false, false), results)
    }

    @Test
    fun `given a tap on other day between two taps on same day when onTap then no double tap`() {
        val detector = detector()

        val results = listOf(detector.onTap(day, 1_000), detector.onTap(otherDay, 1_100), detector.onTap(day, 1_200))

        assertEquals(listOf(false, false, false), results)
    }

    @Test
    fun `given a tap and a reset when a second tap comes within timeout then no double tap`() {
        val detector = detector()

        detector.onTap(day, 1_000)
        detector.reset()

        assertEquals(false, detector.onTap(day, 1_100))
    }
}
