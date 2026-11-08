package com.br.androideventscheduler.feature.upsert

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.alarm.data.AndroidAlarmScheduler
import com.br.androideventscheduler.core.alarm.domain.usecase.ScheduleNextAlarmUseCase
import com.br.androideventscheduler.core.database.RememberMeDatabase
import com.br.androideventscheduler.core.schedule.data.repository.ScheduleRepositoryImpl
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.domain.recurrence.RecurrenceEngine
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.testing.FakeClock
import com.br.androideventscheduler.feature.upsert.domain.usecase.SaveScheduleUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Liga as specs 01, 04 e 07 à recorrência (spec 08): `SaveScheduleUseCase` real + Room em memória +
 * `AndroidAlarmScheduler` sobre o `ShadowAlarmManager`. Confere as ocorrências que a Home consome
 * (`observeOccurrences`) e o alarme da primeira ocorrência futura.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RecurrenceSaveIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val zoneId = ZoneId.of("America/Sao_Paulo")

    // 2026-11-07 (sábado) 12:00 em São Paulo.
    private val clock = FakeClock(Instant.parse("2026-11-07T15:00:00Z"), zoneId)
    private lateinit var database: RememberMeDatabase
    private lateinit var repository: ScheduleRepository
    private lateinit var saveSchedule: SaveScheduleUseCase

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(context, RememberMeDatabase::class.java)
                .allowMainThreadQueries()
                .setQueryExecutor(Runnable::run)
                .setTransactionExecutor(Runnable::run)
                .build()
        val recurrenceEngine = RecurrenceEngine()
        repository = ScheduleRepositoryImpl(database.scheduleDao(), recurrenceEngine, UnconfinedTestDispatcher())
        val scheduleNextAlarm = ScheduleNextAlarmUseCase(recurrenceEngine, AndroidAlarmScheduler(context, alarmManager), clock)
        saveSchedule = SaveScheduleUseCase(repository, scheduleNextAlarm)
    }

    @After
    fun tearDown() {
        database.close()
        ShadowAlarmManager.reset()
    }

    private fun reminderAt(
        startDate: LocalDate,
        pattern: RecurrencePattern,
    ) = ScheduleItem(
        id = "recurring-1",
        title = "Tomar remédio",
        type = ScheduleType.REMINDER,
        startDate = startDate,
        startTime = LocalTime.of(10, 0),
        reminderOffset = ReminderOffset.THIRTY_MINUTES,
        recurrencePattern = pattern,
    )

    private fun occurrenceDatesBetween(
        from: LocalDate,
        to: LocalDate,
    ): List<LocalDate> = runBlocking { repository.observeOccurrences(from, to).first() }.map { it.startDate }

    private fun triggerMillisOf(
        date: LocalDate,
        time: LocalTime,
    ): Long =
        LocalDateTime
            .of(date, time)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

    @Test
    fun `given a weekly tue and thu item starting on a thursday when saving then the occurrences and the first alarm are created`() {
        val firstThursday = LocalDate.of(2026, 11, 12)
        val pattern = RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY))

        val result = runBlocking { saveSchedule(reminderAt(firstThursday, pattern)) }

        assertTrue(result.isSuccess)
        assertEquals(
            listOf(12, 17, 19, 24, 26).map { LocalDate.of(2026, 11, it) },
            occurrenceDatesBetween(LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 28)),
        )
        val alarm = shadowOf(alarmManager).scheduledAlarms.single()
        // 10:00 − 30 min na primeira quinta.
        assertEquals(triggerMillisOf(firstThursday, LocalTime.of(9, 30)), alarm.triggerAtMs)
    }

    @Test
    fun `given a weekly item starting on a day outside the selected days when saving then the first occurrence is the next marked day`() {
        val startSaturday = LocalDate.of(2026, 11, 14)
        val pattern = RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY))

        runBlocking { saveSchedule(reminderAt(startSaturday, pattern)) }

        assertEquals(
            listOf(LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 24)),
            occurrenceDatesBetween(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 24)),
        )
        val alarm = shadowOf(alarmManager).scheduledAlarms.single()
        assertEquals(triggerMillisOf(LocalDate.of(2026, 11, 17), LocalTime.of(9, 30)), alarm.triggerAtMs)
    }

    @Test
    fun `given a weekly item with an occurrence limit when saving then only that many occurrences exist`() {
        val firstThursday = LocalDate.of(2026, 11, 12)
        val pattern = RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), occurrenceLimit = 3)

        runBlocking { saveSchedule(reminderAt(firstThursday, pattern)) }

        assertEquals(
            listOf(12, 17, 19).map { LocalDate.of(2026, 11, it) },
            occurrenceDatesBetween(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31)),
        )
    }

    @Test
    fun `given a daily item that started before now when saving then the alarm targets the next future occurrence`() {
        // Hoje (sábado 7) às 10:00 − 30 min já passou (agora são 12:00): o alarme vai para domingo 8 às 09:30.
        val today = LocalDate.of(2026, 11, 7)

        runBlocking { saveSchedule(reminderAt(today, RecurrencePattern.Daily(occurrenceLimit = 5))) }

        assertEquals(
            (7..11).map { LocalDate.of(2026, 11, it) },
            occurrenceDatesBetween(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)),
        )
        val alarm = shadowOf(alarmManager).scheduledAlarms.single()
        assertEquals(triggerMillisOf(LocalDate.of(2026, 11, 8), LocalTime.of(9, 30)), alarm.triggerAtMs)
    }
}
