package com.br.androideventscheduler.core.ui.format

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR")
class DisplayLabelTest {
    private val resources: Resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun `given no recurrence when formatting then label is Nao repetir`() {
        assertEquals("Não repetir", RecurrencePattern.None.toDisplayLabel(resources))
    }

    @Test
    fun `given daily without limit when formatting then label is Diariamente`() {
        assertEquals("Diariamente", RecurrencePattern.Daily().toDisplayLabel(resources))
    }

    @Test
    fun `given weekly on thursday and tuesday with five thousand times when formatting then days follow sunday order`() {
        val pattern = RecurrencePattern.Weekly(setOf(DayOfWeek.THURSDAY, DayOfWeek.TUESDAY), occurrenceLimit = 5000)

        assertEquals("Toda semana em ter., qui. (5000 vezes)", pattern.toDisplayLabel(resources))
    }

    @Test
    fun `given daily limited to one occurrence when formatting then singular is used`() {
        assertEquals("Diariamente (1 vez)", RecurrencePattern.Daily(occurrenceLimit = 1).toDisplayLabel(resources))
    }

    @Test
    fun `given weekly on sunday and saturday when formatting then sunday comes first`() {
        val pattern = RecurrencePattern.Weekly(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))

        assertEquals("Toda semana em dom., sáb.", pattern.toDisplayLabel(resources))
    }

    @Test
    fun `given each reminder offset when formatting then labels are in portuguese`() {
        val labels = ReminderOffset.entries.map { it.toDisplayLabel(resources) }

        assertEquals(
            listOf("Sem alerta", "10 minutos antes", "15 minutos antes", "30 minutos antes", "1 hora antes"),
            labels,
        )
    }

    @Test
    fun `given date and time when formatting short date time then it uses brazilian abbreviations`() {
        val label = shortDateTimeLabel(LocalDate.of(2026, 11, 5), LocalTime.of(22, 0), resources)

        assertEquals("qui., 5 de nov., 22:00", label)
    }
}
