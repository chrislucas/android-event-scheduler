package com.br.androideventscheduler.feature.search

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.database.RememberMeDatabase
import com.br.androideventscheduler.core.schedule.data.repository.ScheduleRepositoryImpl
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.domain.recurrence.RecurrenceEngine
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.feature.search.domain.model.SearchFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek
import java.time.LocalDate

/** Pesquisa de ponta a ponta no dado: filtros da feature + repositório real sobre Room em memória. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SearchIntegrationTest {
    private lateinit var database: RememberMeDatabase
    private lateinit var repository: ScheduleRepositoryImpl

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), RememberMeDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = ScheduleRepositoryImpl(database.scheduleDao(), RecurrenceEngine(), UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun searchIds(
        query: String,
        filter: SearchFilter = SearchFilter.ALL,
    ): List<String> = repository.search(query, filter.targetType).first().map { it.id }

    @Test
    fun `given accented uppercase items when searching without accents then title notes and location match`() =
        runTest {
            repository.upsert(scheduleItem(id = "title", title = "REUNIÃO de equipe"))
            repository.upsert(scheduleItem(id = "notes", title = "Almoço", notes = "Levar pauta da Reunião"))
            repository.upsert(scheduleItem(id = "location", title = "Visita", location = "Sala de REUNIÃO 2"))
            repository.upsert(scheduleItem(id = "unrelated", title = "Dentista"))

            val ids = searchIds("reuniao").sorted()

            assertEquals(listOf("location", "notes", "title"), ids)
        }

    @Test
    fun `given accented query when searching then unaccented stored text matches`() =
        runTest {
            repository.upsert(scheduleItem(id = "plain", title = "reuniao semanal"))

            assertEquals(listOf("plain"), searchIds("REUNIÃO"))
        }

    @Test
    fun `given events and reminders when filtering by type then only that type is returned`() =
        runTest {
            repository.upsert(scheduleItem(id = "event", title = "Reunião", type = ScheduleType.EVENT))
            repository.upsert(scheduleItem(id = "reminder", title = "Reunião", type = ScheduleType.REMINDER))

            assertEquals(listOf("event", "reminder"), searchIds("reuniao", SearchFilter.ALL).sorted())
            assertEquals(listOf("event"), searchIds("reuniao", SearchFilter.EVENTS))
            assertEquals(listOf("reminder"), searchIds("reuniao", SearchFilter.REMINDERS))
        }

    @Test
    fun `given results on different days when searching then order is chronological by series start`() =
        runTest {
            repository.upsert(scheduleItem(id = "late", title = "Reunião", startDate = LocalDate.of(2026, 12, 1)))
            repository.upsert(
                scheduleItem(
                    id = "recurring-early",
                    title = "Reunião semanal",
                    startDate = LocalDate.of(2026, 11, 3),
                    recurrencePattern = RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY)),
                ),
            )
            repository.upsert(scheduleItem(id = "middle", title = "Reunião", startDate = LocalDate.of(2026, 11, 20)))

            assertEquals(listOf("recurring-early", "middle", "late"), searchIds("reuniao"))
        }

    @Test
    fun `given recurring item when searching then it appears once with its recurrence`() =
        runTest {
            repository.upsert(scheduleItem(id = "daily", title = "Remédio", recurrencePattern = RecurrencePattern.Daily()))

            val results = repository.search("remedio").first()

            assertEquals(1, results.size)
            assertEquals(RecurrencePattern.Daily(), results.single().recurrencePattern)
        }

    @Test
    fun `given text with percent when searching then it is matched literally`() =
        runTest {
            repository.upsert(scheduleItem(id = "literal", title = "Desconto 50% hoje"))
            repository.upsert(scheduleItem(id = "other", title = "Desconto 500 hoje"))

            assertEquals(listOf("literal"), searchIds("50%"))
        }

    @Test
    fun `given an open search when a matching item is saved then a new list is emitted`() =
        runTest {
            // Invalidação do Room em thread própria: colete fora do agendador de teste e espere a emissão real.
            val emittedIds = Channel<List<String>>(Channel.UNLIMITED)
            backgroundScope.launch(Dispatchers.Default) {
                repository.search("reuniao").collect { items -> emittedIds.send(items.map { it.id }) }
            }
            assertEquals(emptyList<String>(), emittedIds.receive())

            repository.upsert(scheduleItem(id = "new", title = "Reunião nova"))

            assertEquals(listOf("new"), emittedIds.receive())
        }
}
