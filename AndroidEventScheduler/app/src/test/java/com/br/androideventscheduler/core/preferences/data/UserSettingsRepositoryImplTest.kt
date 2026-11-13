package com.br.androideventscheduler.core.preferences.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.br.androideventscheduler.core.datastore.createAppPreferencesDataStore
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UserSettingsRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun TestScope.dataStoreAt(file: File): DataStore<Preferences> =
        createAppPreferencesDataStore(scope = backgroundScope, produceFile = { file })

    private fun newPreferencesFile(): File = File(temporaryFolder.newFolder(), "settings.preferences_pb")

    @Test
    fun `given nothing saved when reading then defaults are returned`() =
        runTest {
            val repository = UserSettingsRepositoryImpl(dataStoreAt(newPreferencesFile()))

            assertEquals(UserSettings(), repository.current())
        }

    @Test
    fun `given saved settings when reading them back then every field round trips`() =
        runTest {
            val repository = UserSettingsRepositoryImpl(dataStoreAt(newPreferencesFile()))
            val saved =
                UserSettings(
                    activeAccountEmail = "ana@exemplo.com",
                    defaultReminderOffset = ReminderOffset.ONE_HOUR,
                    defaultSnoozeMinutes = 5,
                    fullScreenEventAlertEnabled = false,
                )

            repository.save(saved)

            assertEquals(saved, repository.current())
        }

    @Test
    fun `given reminder offset none when saving then it is read back as none`() =
        runTest {
            val repository = UserSettingsRepositoryImpl(dataStoreAt(newPreferencesFile()))

            repository.save(UserSettings(defaultReminderOffset = ReminderOffset.NONE))

            assertEquals(ReminderOffset.NONE, repository.current().defaultReminderOffset)
        }

    @Test
    fun `given five minute default reminder when saving then it is stored as minutes and read back`() =
        runTest {
            val dataStore = dataStoreAt(newPreferencesFile())
            val repository = UserSettingsRepositoryImpl(dataStore)

            repository.save(UserSettings(defaultReminderOffset = ReminderOffset.FIVE_MINUTES))

            assertEquals(5, dataStore.data.first()[intPreferencesKey("default_reminder_minutes")])
            assertEquals(ReminderOffset.FIVE_MINUTES, repository.current().defaultReminderOffset)
        }

    @Test
    fun `given email with spaces when saving then it is stored trimmed`() =
        runTest {
            val repository = UserSettingsRepositoryImpl(dataStoreAt(newPreferencesFile()))

            repository.save(UserSettings(activeAccountEmail = "  ana@exemplo.com  "))

            assertEquals("ana@exemplo.com", repository.current().activeAccountEmail)
        }

    @Test
    fun `given corrupted preferences file when reading then defaults are returned without crashing`() =
        runTest {
            val corruptedFile = newPreferencesFile().apply { writeBytes(byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42, 0x0A, 0x55)) }
            val repository = UserSettingsRepositoryImpl(dataStoreAt(corruptedFile))

            assertEquals(UserSettings(), repository.current())
        }
}
