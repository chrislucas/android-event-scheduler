package com.br.androideventscheduler.core.preferences.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.br.androideventscheduler.core.datastore.createAppPreferencesDataStore
import com.br.androideventscheduler.core.preferences.domain.ThemeMode
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class ThemePreferenceRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun TestScope.dataStoreAt(file: File): DataStore<Preferences> =
        createAppPreferencesDataStore(scope = backgroundScope, produceFile = { file })

    private fun newPreferencesFile(): File = File(temporaryFolder.newFolder(), "theme.preferences_pb")

    @Test
    fun `given nothing saved when observing then the theme is dark`() =
        runTest {
            val repository = ThemePreferenceRepositoryImpl(dataStoreAt(newPreferencesFile()))

            assertEquals(ThemeMode.DARK, repository.observe().first())
        }

    @Test
    fun `given light saved when observing then light is emitted`() =
        runTest {
            val repository = ThemePreferenceRepositoryImpl(dataStoreAt(newPreferencesFile()))

            repository.set(ThemeMode.LIGHT)

            assertEquals(ThemeMode.LIGHT, repository.observe().first())
        }

    @Test
    fun `given each mode when saving it then it round trips`() =
        runTest {
            val repository = ThemePreferenceRepositoryImpl(dataStoreAt(newPreferencesFile()))

            ThemeMode.entries.forEach { mode ->
                repository.set(mode)
                assertEquals(mode, repository.observe().first())
            }
        }

    @Test
    fun `given light saved when a new instance reads the same file then light is still there`() =
        runTest {
            val file = newPreferencesFile()
            // Primeira "execução do app": escopo próprio, cancelado antes da segunda (duas instâncias ativas no mesmo arquivo falham).
            val firstRun = Job()
            val firstStore = createAppPreferencesDataStore(scope = CoroutineScope(firstRun + Dispatchers.IO), produceFile = { file })
            ThemePreferenceRepositoryImpl(firstStore).set(ThemeMode.LIGHT)
            firstRun.cancelAndJoin()

            val reopened = ThemePreferenceRepositoryImpl(dataStoreAt(file))

            assertEquals(ThemeMode.LIGHT, reopened.observe().first())
        }

    @Test
    fun `given the mode is saved then the raw value is the stable id and not the enum name`() =
        runTest {
            val dataStore = dataStoreAt(newPreferencesFile())

            ThemePreferenceRepositoryImpl(dataStore).set(ThemeMode.SYSTEM)

            assertEquals("system", dataStore.data.first()[stringPreferencesKey("theme_mode")])
        }

    @Test
    fun `given an unknown value written by hand when observing then the theme is dark`() =
        runTest {
            val dataStore = dataStoreAt(newPreferencesFile())
            dataStore.edit { it[stringPreferencesKey("theme_mode")] = "sepia" }
            val repository = ThemePreferenceRepositoryImpl(dataStore)

            assertEquals(ThemeMode.DARK, repository.observe().first())
        }

    @Test
    fun `given an IOException while reading when observing then the theme is dark and nothing crashes`() =
        runTest {
            val repository = ThemePreferenceRepositoryImpl(FailingDataStore(IOException("disk error")))

            assertEquals(ThemeMode.DARK, repository.observe().first())
        }

    @Test
    fun `given a non IO failure while reading when observing then it is not swallowed`() =
        runTest {
            val repository = ThemePreferenceRepositoryImpl(FailingDataStore(IllegalStateException("bug")))

            val failure = runCatching { repository.observe().first() }.exceptionOrNull()

            assertTrue(failure is IllegalStateException)
        }

    @Test
    fun `given other preferences saved when setting the theme then they are untouched`() =
        runTest {
            val dataStore = dataStoreAt(newPreferencesFile())
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey("active_account_email")] = "ana@exemplo.com"
                preferences[intPreferencesKey("default_reminder_minutes")] = ReminderOffset.ONE_HOUR.minutes ?: -1
                preferences[intPreferencesKey("default_snooze_minutes")] = UserSettings.ALLOWED_SNOOZE_MINUTES.first()
                preferences[booleanPreferencesKey("full_screen_event_alert")] = false
                preferences[stringSetPreferencesKey("enabled_calendar_ids")] = setOf("family_calendar")
                preferences[stringPreferencesKey("day_sticker_20000")] = "🎉"
            }
            val before =
                dataStore.data
                    .first()
                    .asMap()
                    .filterKeys { it.name != "theme_mode" }

            ThemePreferenceRepositoryImpl(dataStore).set(ThemeMode.LIGHT)

            val after =
                dataStore.data
                    .first()
                    .asMap()
                    .filterKeys { it.name != "theme_mode" }
            assertEquals(before, after)
            assertEquals(6, before.size)
        }

    @Test
    fun `given the theme saved when saving user settings then the theme is untouched`() =
        runTest {
            val dataStore = dataStoreAt(newPreferencesFile())
            ThemePreferenceRepositoryImpl(dataStore).set(ThemeMode.LIGHT)

            UserSettingsRepositoryImpl(dataStore).save(UserSettings(activeAccountEmail = "ana@exemplo.com"))

            assertEquals(ThemeMode.LIGHT, ThemePreferenceRepositoryImpl(dataStore).observe().first())
        }

    /** DataStore cuja leitura sempre falha com [error]. */
    private class FailingDataStore(
        private val error: Throwable,
    ) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw error }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw error
    }
}
