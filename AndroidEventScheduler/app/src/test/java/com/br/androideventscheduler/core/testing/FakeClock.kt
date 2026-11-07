package com.br.androideventscheduler.core.testing

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** [Clock] controlável para testes determinísticos (datas, alarmes, recorrência). */
class FakeClock(
    initialInstant: Instant = Instant.parse("2026-11-07T12:00:00Z"),
    private val zoneId: ZoneId = ZoneId.of("America/Sao_Paulo"),
) : Clock() {
    private var currentInstant: Instant = initialInstant

    override fun getZone(): ZoneId = zoneId

    override fun withZone(zone: ZoneId): Clock = FakeClock(currentInstant, zone)

    override fun instant(): Instant = currentInstant

    fun setInstant(instant: Instant) {
        currentInstant = instant
    }

    fun advanceBy(duration: Duration) {
        currentInstant = currentInstant.plus(duration)
    }
}
