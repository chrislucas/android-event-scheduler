package com.br.androideventscheduler.feature.upsert.presentation.mapper

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

class DatePickerMillisTest {
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun rememberTimeZone() {
        originalTimeZone = TimeZone.getDefault()
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `given a date when converting to picker millis then it is midnight UTC of that date`() {
        val millis = LocalDate.of(2026, 11, 5).toDatePickerMillis()

        assertEquals(Instant.parse("2026-11-05T00:00:00Z").toEpochMilli(), millis)
    }

    @Test
    fun `given picker millis at midnight UTC when device is in a negative offset zone then the chosen date is not shifted back`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
        val pickerMillis = Instant.parse("2026-11-05T00:00:00Z").toEpochMilli()

        val chosenDate = pickerMillis.toLocalDateFromDatePickerMillis()

        assertEquals(LocalDate.of(2026, 11, 5), chosenDate)
        // Contraprova: a leitura ingênua pelo fuso do aparelho recuaria um dia — o bug que este conversor evita.
        val naiveDate = Instant.ofEpochMilli(pickerMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        assertEquals(LocalDate.of(2026, 11, 4), naiveDate)
    }

    @Test
    fun `given any device zone when round tripping dates then the saved date equals the chosen date`() {
        val zones = listOf("America/Sao_Paulo", "UTC", "Pacific/Kiritimati", "Pacific/Pago_Pago", "Asia/Kolkata")
        val dates =
            listOf(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2028, 2, 29),
                LocalDate.of(2026, 11, 1), // virada de horário de verão em alguns países
                LocalDate.of(2026, 12, 31),
            )

        zones.forEach { zoneId ->
            TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
            dates.forEach { date ->
                assertEquals("zone=$zoneId", date, date.toDatePickerMillis().toLocalDateFromDatePickerMillis())
            }
        }
    }
}
