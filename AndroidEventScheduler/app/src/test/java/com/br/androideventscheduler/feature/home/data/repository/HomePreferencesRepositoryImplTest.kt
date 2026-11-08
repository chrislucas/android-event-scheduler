package com.br.androideventscheduler.feature.home.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.br.androideventscheduler.core.datastore.createAppPreferencesDataStore
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HomePreferencesRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun TestScope.dataStoreAt(file: File): DataStore<Preferences> =
        createAppPreferencesDataStore(scope = backgroundScope, produceFile = { file })

    private fun newPreferencesFile(): File = File(temporaryFolder.newFolder(), "home.preferences_pb")

    @Test
    fun `given nothing saved when observing then every calendar is visible`() =
        runTest {
            val repository = HomePreferencesRepositoryImpl(dataStoreAt(newPreferencesFile()))

            assertEquals(CalendarSource.entries.toSet(), repository.observeEnabledCalendars().first())
        }

    @Test
    fun `given a calendar disabled when observing then it is the only one hidden`() =
        runTest {
            val repository = HomePreferencesRepositoryImpl(dataStoreAt(newPreferencesFile()))

            repository.setCalendarEnabled(CalendarSource.FAMILY, enabled = false)

            val restored = repository.observeEnabledCalendars().first()
            assertEquals(setOf(CalendarSource.PERSONAL_ACCOUNT, CalendarSource.MY_REMINDERS), restored)
        }

    @Test
    fun `given a disabled calendar when enabling it again then every calendar is visible`() =
        runTest {
            val repository = HomePreferencesRepositoryImpl(dataStoreAt(newPreferencesFile()))
            repository.setCalendarEnabled(CalendarSource.FAMILY, enabled = false)

            repository.setCalendarEnabled(CalendarSource.FAMILY, enabled = true)

            assertEquals(CalendarSource.entries.toSet(), repository.observeEnabledCalendars().first())
        }

    @Test
    fun `given an unknown persisted id when observing then it is ignored`() =
        runTest {
            val dataStore = dataStoreAt(newPreferencesFile())
            dataStore.edit { it[stringSetPreferencesKey("enabled_calendar_ids")] = setOf("family_calendar", "removed_in_future") }
            val repository = HomePreferencesRepositoryImpl(dataStore)

            assertEquals(setOf(CalendarSource.FAMILY), repository.observeEnabledCalendars().first())
        }

    @Test
    fun `given two consecutive toggles when both complete then both are preserved`() =
        runTest {
            val repository = HomePreferencesRepositoryImpl(dataStoreAt(newPreferencesFile()))

            repository.setCalendarEnabled(CalendarSource.FAMILY, enabled = false)
            repository.setCalendarEnabled(CalendarSource.MY_REMINDERS, enabled = false)

            assertEquals(setOf(CalendarSource.PERSONAL_ACCOUNT), repository.observeEnabledCalendars().first())
        }

    @Test
    fun `given every calendar disabled when observing then the set is empty and not the default`() =
        runTest {
            val repository = HomePreferencesRepositoryImpl(dataStoreAt(newPreferencesFile()))

            CalendarSource.entries.forEach { repository.setCalendarEnabled(it, enabled = false) }

            assertEquals(emptySet<CalendarSource>(), repository.observeEnabledCalendars().first())
        }

    @Test
    fun `given nothing saved when observing a day sticker then it is null`() =
        runTest {
            val repository = HomePreferencesRepositoryImpl(dataStoreAt(newPreferencesFile()))

            assertNull(repository.observeDaySticker(epochDay = 20_000L).first())
        }

    @Test
    fun `given a sticker set for a day when observing then it is restored`() =
        runTest {
            val repository = HomePreferencesRepositoryImpl(dataStoreAt(newPreferencesFile()))

            repository.setDaySticker(epochDay = 20_000L, emoji = "🎉")

            assertEquals("🎉", repository.observeDaySticker(20_000L).first())
        }

    @Test
    fun `given a sticker saved when the app restarts on the same file then it is still there`() =
        runTest {
            val file = newPreferencesFile()
            // Primeira "execução do app": escopo próprio, cancelado ao final para liberar o arquivo.
            val firstRun = Job()
            val firstStore = createAppPreferencesDataStore(scope = CoroutineScope(firstRun + Dispatchers.IO), produceFile = { file })
            HomePreferencesRepositoryImpl(firstStore).setDaySticker(epochDay = 20_000L, emoji = "🚀")
            firstRun.cancelAndJoin()

            val reopened = HomePreferencesRepositoryImpl(dataStoreAt(file))

            assertEquals("🚀", reopened.observeDaySticker(20_000L).first())
        }

    @Test
    fun `given stickers on two days when changing one then the other day is independent`() =
        runTest {
            val repository = HomePreferencesRepositoryImpl(dataStoreAt(newPreferencesFile()))
            repository.setDaySticker(epochDay = 20_000L, emoji = "🎉")
            repository.setDaySticker(epochDay = 20_001L, emoji = "☕")

            repository.setDaySticker(epochDay = 20_000L, emoji = "⭐")

            assertEquals("⭐", repository.observeDaySticker(20_000L).first())
            assertEquals("☕", repository.observeDaySticker(20_001L).first())
        }

    @Test
    fun `given a sticker set when removing it with null then the day has no sticker and the other days are kept`() =
        runTest {
            val repository = HomePreferencesRepositoryImpl(dataStoreAt(newPreferencesFile()))
            repository.setDaySticker(epochDay = 20_000L, emoji = "🎉")
            repository.setDaySticker(epochDay = 20_001L, emoji = "☕")

            repository.setDaySticker(epochDay = 20_000L, emoji = null)

            assertNull(repository.observeDaySticker(20_000L).first())
            assertEquals("☕", repository.observeDaySticker(20_001L).first())
        }

    @Test
    fun `given a sticker set when saving a blank emoji then the key is removed`() =
        runTest {
            val dataStore = dataStoreAt(newPreferencesFile())
            val repository = HomePreferencesRepositoryImpl(dataStore)
            repository.setDaySticker(epochDay = 20_000L, emoji = "🎉")

            repository.setDaySticker(epochDay = 20_000L, emoji = "  ")

            assertNull(repository.observeDaySticker(20_000L).first())
            assertNull(dataStore.data.first()[stringPreferencesKey("day_sticker_20000")])
        }

    @Test
    fun `given a day sticker when setting calendar filters then the sticker is untouched`() =
        runTest {
            val repository = HomePreferencesRepositoryImpl(dataStoreAt(newPreferencesFile()))
            repository.setDaySticker(epochDay = 20_000L, emoji = "🎉")

            repository.setCalendarEnabled(CalendarSource.FAMILY, enabled = false)

            assertEquals("🎉", repository.observeDaySticker(20_000L).first())
        }
}
