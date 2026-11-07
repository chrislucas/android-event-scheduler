package com.br.androideventscheduler.core.testing

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class FakeClockTest {
    @Test
    fun `given fake clock when advancing then instant moves by the duration`() {
        val clock = FakeClock(initialInstant = Instant.parse("2026-11-07T12:00:00Z"))

        clock.advanceBy(Duration.ofMinutes(90))

        assertEquals(Instant.parse("2026-11-07T13:30:00Z"), clock.instant())
        assertEquals(clock.instant().toEpochMilli(), clock.millis())
    }

    @Test
    fun `given fake clock when setting instant then it returns the new instant`() {
        val clock = FakeClock()

        clock.setInstant(Instant.parse("2027-01-01T00:00:00Z"))

        assertEquals(Instant.parse("2027-01-01T00:00:00Z"), clock.instant())
    }

    @Test
    fun `given fake clock when changing zone then instant is preserved and zone changes`() {
        val clock = FakeClock(initialInstant = Instant.parse("2026-11-07T12:00:00Z"))

        val utcClock = clock.withZone(ZoneId.of("UTC"))

        assertEquals(ZoneId.of("UTC"), utcClock.zone)
        assertEquals(clock.instant(), utcClock.instant())
    }
}
