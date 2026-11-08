package com.br.androideventscheduler.core.ui.format

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR")
class CalendarSourceFormatTest {
    private val strings = ResourcesStringProvider(ApplicationProvider.getApplicationContext<Context>().resources)

    @Test
    fun `given the personal account when labeling then the active email is used`() {
        assertEquals("bia@exemplo.com", CalendarSource.PERSONAL_ACCOUNT.label("bia@exemplo.com", strings))
    }

    @Test
    fun `given the fixed calendars when labeling then the portuguese names are used regardless of the email`() {
        assertEquals("Family", CalendarSource.FAMILY.label("bia@exemplo.com", strings))
        assertEquals("Meus lembretes", CalendarSource.MY_REMINDERS.label("bia@exemplo.com", strings))
    }
}
