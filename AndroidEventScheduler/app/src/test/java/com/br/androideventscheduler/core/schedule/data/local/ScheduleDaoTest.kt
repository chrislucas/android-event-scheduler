package com.br.androideventscheduler.core.schedule.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.database.RememberMeDatabase
import com.br.androideventscheduler.core.schedule.data.mapper.toEntity
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class ScheduleDaoTest {
    private lateinit var database: RememberMeDatabase
    private lateinit var dao: ScheduleDao

    private val novemberFirst = LocalDate.of(2026, 11, 1).toEpochDay()
    private val novemberLast = LocalDate.of(2026, 11, 30).toEpochDay()

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), RememberMeDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.scheduleDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `given items inside outside and across the month when observing november then only overlapping ones return`() =
        runTest {
            dao.upsert(scheduleItem(id = "inside", startDate = LocalDate.of(2026, 11, 10)).toEntity())
            dao.upsert(scheduleItem(id = "before", startDate = LocalDate.of(2026, 10, 10)).toEntity())
            dao.upsert(scheduleItem(id = "after", startDate = LocalDate.of(2026, 12, 2)).toEntity())
            dao.upsert(
                scheduleItem(id = "across", startDate = LocalDate.of(2026, 10, 30), endDate = LocalDate.of(2026, 11, 1)).toEntity(),
            )

            val ids = dao.observeInRange(novemberFirst, novemberLast).first().map { it.id }

            assertEquals(listOf("across", "inside"), ids)
        }

    @Test
    fun `given recurring item started before the month when observing november then it is returned`() =
        runTest {
            dao.upsert(
                scheduleItem(
                    id = "daily-since-january",
                    startDate = LocalDate.of(2026, 1, 1),
                    recurrencePattern = RecurrencePattern.Daily(),
                ).toEntity(),
            )

            val ids = dao.observeInRange(novemberFirst, novemberLast).first().map { it.id }

            assertEquals(listOf("daily-since-january"), ids)
        }

    @Test
    fun `given accented uppercase text in title notes and location when searching without accents then all are found`() =
        runTest {
            dao.upsert(scheduleItem(id = "title", title = "REUNIÃO geral").toEntity())
            dao.upsert(
                scheduleItem(id = "notes", title = "Café", notes = "pauta da reunião", startDate = LocalDate.of(2026, 11, 6)).toEntity(),
            )
            dao.upsert(
                scheduleItem(
                    id = "location",
                    title = "Treino",
                    location = "Sala de Reunião",
                    startDate = LocalDate.of(2026, 11, 7),
                ).toEntity(),
            )
            dao.upsert(scheduleItem(id = "other", title = "Mercado", startDate = LocalDate.of(2026, 11, 8)).toEntity())

            val ids = dao.search(escapedPattern = "reuniao", type = null).first().map { it.id }

            assertEquals(listOf("title", "notes", "location"), ids)
        }

    @Test
    fun `given wildcard characters when searching with escaped pattern then they match literally`() =
        runTest {
            dao.upsert(scheduleItem(id = "percent", title = "Desconto 50% loja").toEntity())
            dao.upsert(scheduleItem(id = "underscore", title = "arquivo_final").toEntity())
            dao.upsert(scheduleItem(id = "plain", title = "Desconto 500 loja arquivoXfinal").toEntity())

            val percentIds = dao.search(escapedPattern = "50\\%", type = null).first().map { it.id }
            val underscoreIds = dao.search(escapedPattern = "arquivo\\_final", type = null).first().map { it.id }

            assertEquals(listOf("percent"), percentIds)
            assertEquals(listOf("underscore"), underscoreIds)
        }

    @Test
    fun `given events and reminders when searching with type filter then only that type returns`() =
        runTest {
            dao.upsert(scheduleItem(id = "event", title = "Dentista", type = ScheduleType.EVENT).toEntity())
            dao.upsert(scheduleItem(id = "reminder", title = "Dentista", type = ScheduleType.REMINDER).toEntity())

            val reminders = dao.search(escapedPattern = "dentista", type = ScheduleType.REMINDER.name).first().map { it.id }

            assertEquals(listOf("reminder"), reminders)
        }

    @Test
    fun `given existing item when upserting same id then it is replaced`() =
        runTest {
            dao.upsert(scheduleItem(id = "x", title = "Antigo").toEntity())

            dao.upsert(scheduleItem(id = "x", title = "Novo").toEntity())

            assertEquals(listOf("Novo"), dao.getAll().map { it.title })
        }

    @Test
    fun `given observed item when deleted then observer receives null`() =
        runTest {
            dao.upsert(scheduleItem(id = "x").toEntity())
            assertEquals("x", dao.observeById("x").first()?.id)

            dao.deleteById("x")

            assertNull(dao.observeById("x").first())
            assertNull(dao.getById("x"))
        }

    @Test
    fun `given same day items when listing all then all day comes first then by time`() =
        runTest {
            dao.upsert(scheduleItem(id = "late", startTime = LocalTime.of(18, 0)).toEntity())
            dao.upsert(scheduleItem(id = "allday", isAllDay = true, startTime = LocalTime.of(23, 0)).toEntity())
            dao.upsert(scheduleItem(id = "early", startTime = LocalTime.of(8, 0)).toEntity())

            assertEquals(listOf("allday", "early", "late"), dao.getAll().map { it.id })
        }

    @Test
    fun `given new and existing items when upserting all then all are stored and existing ones replaced`() =
        runTest {
            dao.upsert(scheduleItem(id = "a", title = "Antigo").toEntity())

            dao.upsertAll(listOf(scheduleItem(id = "a", title = "Novo"), scheduleItem(id = "b", title = "B")).map { it.toEntity() })

            assertEquals(mapOf("a" to "Novo", "b" to "B"), dao.getAll().associate { it.id to it.title })
        }

    @Test
    fun `given a failure in the middle of the list when upserting all then nothing is stored`() =
        runTest {
            dao.upsert(scheduleItem(id = "kept", title = "Original").toEntity())
            // Gatilho que aborta a inserção do id "boom": simula falha no meio do lote.
            database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER fail_boom BEFORE INSERT ON schedules WHEN NEW.id = 'boom' BEGIN SELECT RAISE(ABORT, 'boom'); END",
            )
            val batch =
                listOf(
                    scheduleItem(id = "kept", title = "Alterado"),
                    scheduleItem(id = "first"),
                    scheduleItem(id = "boom"),
                    scheduleItem(id = "last"),
                ).map { it.toEntity() }

            val result = runCatching { dao.upsertAll(batch) }

            assertTrue(result.isFailure)
            assertEquals(mapOf("kept" to "Original"), dao.getAll().associate { it.id to it.title })
        }

    @Test
    fun `given an empty list when upserting all then nothing changes`() =
        runTest {
            dao.upsertAll(emptyList())

            assertTrue(dao.getAll().isEmpty())
        }
}
