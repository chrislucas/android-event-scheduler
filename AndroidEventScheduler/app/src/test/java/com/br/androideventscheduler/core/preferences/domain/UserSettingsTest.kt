package com.br.androideventscheduler.core.preferences.domain

import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsTest {
    @Test
    fun `given default settings when reading then the documented defaults are used`() {
        val settings = UserSettings()

        assertEquals("christoffer.luccas@gmail.com", settings.activeAccountEmail)
        assertEquals(ReminderOffset.FIFTEEN_MINUTES, settings.defaultReminderOffset)
        assertEquals(10, settings.defaultSnoozeMinutes)
        assertTrue(settings.fullScreenEventAlertEnabled)
    }

    @Test
    fun `given valid email with surrounding spaces when validating then it succeeds with trimmed email`() {
        val result = UserSettings(activeAccountEmail = "  ana@exemplo.com.br ").validate()

        assertEquals("ana@exemplo.com.br", result.getOrThrow().activeAccountEmail)
    }

    @Test
    fun `given blank or malformed email when validating then it fails with InvalidEmail`() {
        listOf("", "   ", "sem-arroba", "a@b", "@dominio.com", "nome@", "a b@c.com").forEach { email ->
            val error = UserSettings(activeAccountEmail = email).validate().exceptionOrNull()

            assertEquals("email '$email'", SettingsValidationError.InvalidEmail, error)
        }
    }

    @Test
    fun `given snooze outside the allowed options when validating then it fails with InvalidSnooze`() {
        val error = UserSettings(defaultSnoozeMinutes = 7).validate().exceptionOrNull()

        assertEquals(SettingsValidationError.InvalidSnooze, error)
    }

    @Test
    fun `given each allowed snooze option when validating then it succeeds`() {
        UserSettings.ALLOWED_SNOOZE_MINUTES.forEach { minutes ->
            assertTrue(UserSettings(defaultSnoozeMinutes = minutes).validate().isSuccess)
        }
        assertEquals(listOf(5, 10, 15), UserSettings.ALLOWED_SNOOZE_MINUTES)
    }
}
