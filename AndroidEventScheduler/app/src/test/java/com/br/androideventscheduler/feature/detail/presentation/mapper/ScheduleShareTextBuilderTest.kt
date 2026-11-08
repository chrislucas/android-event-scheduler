package com.br.androideventscheduler.feature.detail.presentation.mapper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.common.AndroidStringProvider
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Usa os resources reais pt-BR e o mesmo mapper da tela de detalhes, para provar que não há divergência de formato. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR")
class ScheduleShareTextBuilderTest {
    private val strings = AndroidStringProvider(ApplicationProvider.getApplicationContext<Context>())
    private val mapper = ScheduleDetailMapper(strings)
    private val builder = ScheduleShareTextBuilder(strings)
    private val accountEmail = "ana@exemplo.com"
    private val thursday = LocalDate.of(2026, 11, 5)

    private fun contentOf(
        title: String = "Aula de Kotlin",
        location: String = "Sala 3",
        notes: String = "Levar o notebook",
        recurrencePattern: RecurrencePattern = RecurrencePattern.None,
    ) = builder.build(
        mapper.map(
            scheduleItem(
                id = "internal-id-123",
                title = title,
                startDate = thursday,
                startTime = LocalTime.of(22, 0),
                endTime = LocalTime.of(23, 30),
                location = location,
                notes = notes,
                recurrencePattern = recurrencePattern,
            ),
            accountEmail,
        ),
    )

    @Test
    fun `given item with location and notes when building then body has one line per field in order`() {
        val content = contentOf()

        assertEquals(
            listOf(
                "📅 Aula de Kotlin",
                "🕒 qui., 5 de nov., 22:00 – 23:30",
                "🔔 Alerta: 15 minutos antes",
                "🔁 Repetição: Não repetir",
                "📍 Local: Sala 3",
                "📝 Notas: Levar o notebook",
            ),
            content.body.lines(),
        )
    }

    @Test
    fun `given item without location when building then the location line is omitted`() {
        val body = contentOf(location = "").body

        assertFalse(body.contains("📍"))
        assertEquals(5, body.lines().size)
        assertEquals("📝 Notas: Levar o notebook", body.lines().last())
    }

    @Test
    fun `given item without notes when building then the notes line is omitted`() {
        val body = contentOf(notes = "").body

        assertFalse(body.contains("📝"))
        assertEquals("📍 Local: Sala 3", body.lines().last())
    }

    @Test
    fun `given item without location and notes when building then body has only the four fixed lines`() {
        val body = contentOf(location = "", notes = "").body

        assertEquals(4, body.lines().size)
        assertEquals("🔁 Repetição: Não repetir", body.lines().last())
    }

    @Test
    fun `given weekly recurring item when building then the repetition line uses the detail screen text`() {
        val pattern = RecurrencePattern.Weekly(daysOfWeek = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), occurrenceLimit = 5000)
        val recurrenceText = mapper.map(scheduleItem(recurrencePattern = pattern), accountEmail).recurrenceText

        val body = contentOf(recurrencePattern = pattern).body

        assertEquals("🔁 Repetição: Toda semana em ter., qui. (5000 vezes)", body.lines()[3])
        assertEquals("🔁 Repetição: $recurrenceText", body.lines()[3])
    }

    @Test
    fun `given item when building then subject is the app name followed by the title`() {
        assertEquals("RememberMe: Aula de Kotlin", contentOf().subject)
    }

    @Test
    fun `given item when building then neither the internal id nor the account email are shared`() {
        val content = contentOf()

        assertFalse(content.body.contains("internal-id-123"))
        assertFalse(content.body.contains(accountEmail))
        assertFalse(content.subject.contains(accountEmail))
    }
}
