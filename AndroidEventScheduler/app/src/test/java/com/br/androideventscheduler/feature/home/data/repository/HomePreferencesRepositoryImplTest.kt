package com.br.androideventscheduler.feature.home.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.br.androideventscheduler.core.datastore.createAppPreferencesDataStore
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
}
