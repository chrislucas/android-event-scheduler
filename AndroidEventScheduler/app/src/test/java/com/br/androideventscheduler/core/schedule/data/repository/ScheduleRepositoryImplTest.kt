package com.br.androideventscheduler.core.schedule.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.database.RememberMeDatabase
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.recurrence.RecurrenceEngine
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScheduleRepositoryImplTest {
    private lateinit var database: RememberMeDatabase
    private lateinit var repository: ScheduleRepositoryImpl

    private val novemberFirst = LocalDate.of(2026, 11, 1)
    private val novemberLast = LocalDate.of(2026, 11, 30)

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

    @Test
    fun `given weekly recurring item when observing november then each tuesday and thursday is an occurrence`() =
        runTest {
            repository.upsert(
                scheduleItem(
                    id = "weekly",
                    startDate = LocalDate.of(2026, 11, 5),
                    recurrencePattern = RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)),
                ),
            )

            val occurrenceDays = repository.observeOccurrences(novemberFirst, novemberLast).first().map { it.startDate.dayOfMonth }

            assertEquals(listOf(5, 10, 12, 17, 19, 24, 26), occurrenceDays)
        }

    @Test
    fun `given observer of november when an item is inserted then a new list is emitted`() =
        runTest {
            // A invalidação do Room roda em thread própria: coletamos fora do agendador de teste e
            // esperamos cada emissão de verdade, sem depender de tempo virtual.
            val emittedSizes = Channel<Int>(Channel.UNLIMITED)
            backgroundScope.launch(Dispatchers.Default) {
                repository.observeOccurrences(novemberFirst, novemberLast).collect { emittedSizes.send(it.size) }
            }

            assertEquals(0, emittedSizes.receive())
            repository.upsert(scheduleItem(id = "new", startDate = LocalDate.of(2026, 11, 20)))

            assertEquals(1, emittedSizes.receive())
        }

    @Test
    fun `given blank query when searching then empty list is returned without querying`() =
        runTest {
            repository.upsert(scheduleItem(id = "x", title = "Qualquer"))

            assertTrue(repository.search("   ").first().isEmpty())
        }

    @Test
    fun `given accented query with wildcard when searching then it is normalized and escaped`() =
        runTest {
            repository.upsert(scheduleItem(id = "percent", title = "Promoção 50% hoje"))
            repository.upsert(scheduleItem(id = "other", title = "Promoção 500 hoje"))

            val ids = repository.search("PROMOÇÃO 50%").first().map { it.id }

            assertEquals(listOf("percent"), ids)
        }

    @Test
    fun `given items when getting by id and deleting then repository reflects the change`() =
        runTest {
            val item = scheduleItem(id = "x", title = "Treino")
            repository.upsert(item)

            assertEquals(item, repository.getById("x"))
            assertEquals(listOf(item), repository.getAll())

            repository.delete("x")

            assertEquals(null, repository.observeById("x").first())
        }
}
