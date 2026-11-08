package com.br.androideventscheduler.core.alarm.data

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.alarm.domain.model.AlarmRequest
import com.br.androideventscheduler.core.alarm.domain.model.AlertPresentationMode
import com.br.androideventscheduler.core.alarm.domain.scheduler.AlarmScheduler
import com.br.androideventscheduler.core.alarm.domain.usecase.RescheduleAllAlarmsUseCase
import com.br.androideventscheduler.core.alarm.domain.usecase.ScheduleNextAlarmUseCase
import com.br.androideventscheduler.core.database.RememberMeDatabase
import com.br.androideventscheduler.core.di.DispatcherNames
import com.br.androideventscheduler.core.schedule.data.repository.ScheduleRepositoryImpl
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.recurrence.RecurrenceEngine
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.FakeClock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Ponta a ponta: Room em memória + `AndroidAlarmScheduler` real + `ShadowAlarmManager`. Simula o reinício
 * (o `AlarmManager` perde tudo), envia o broadcast ao receiver declarado e confere os alarmes recriados.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BootRestoreIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val zoneId = ZoneId.of("America/Sao_Paulo")

    // 2026-11-07 (sábado) 12:00 em São Paulo.
    private val clock = FakeClock(Instant.parse("2026-11-07T15:00:00Z"), zoneId)
    private lateinit var database: RememberMeDatabase
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var overrides: org.koin.core.module.Module

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(context, RememberMeDatabase::class.java)
                .allowMainThreadQueries()
                // Executores diretos: o trabalho do `goAsync` termina de forma determinística dentro do teste.
                .setQueryExecutor(Runnable::run)
                .setTransactionExecutor(Runnable::run)
                .build()
        val recurrenceEngine = RecurrenceEngine()
        val repository = ScheduleRepositoryImpl(database.scheduleDao(), recurrenceEngine, UnconfinedTestDispatcher())
        alarmScheduler = AndroidAlarmScheduler(context, alarmManager)
        val rescheduleAllAlarms =
            RescheduleAllAlarmsUseCase(repository, ScheduleNextAlarmUseCase(recurrenceEngine, alarmScheduler, clock))
        overrides =
            module {
                single<CoroutineDispatcher>(named(DispatcherNames.IO)) { UnconfinedTestDispatcher() }
                factory { rescheduleAllAlarms }
            }
        loadKoinModules(overrides)
        runBlocking { populate(repository) }
    }

    @After
    fun tearDown() {
        unloadKoinModules(overrides)
        database.close()
        ShadowAlarmManager.reset()
    }

    private suspend fun populate(repository: ScheduleRepositoryImpl) {
        listOf(
            item("future", startDate = LocalDate.of(2026, 11, 8)),
            item("daily", startDate = LocalDate.of(2026, 11, 1), pattern = RecurrencePattern.Daily()),
            item("past", startDate = LocalDate.of(2026, 11, 5)),
            item("silent", startDate = LocalDate.of(2026, 11, 8), offset = ReminderOffset.NONE),
        ).forEach { repository.upsert(it) }
    }

    private fun item(
        id: String,
        startDate: LocalDate,
        offset: ReminderOffset = ReminderOffset.FIFTEEN_MINUTES,
        pattern: RecurrencePattern = RecurrencePattern.None,
    ): ScheduleItem =
        scheduleItem(id = id, startDate = startDate, startTime = LocalTime.of(10, 0), recurrencePattern = pattern)
            .copy(reminderOffset = offset)

    private fun millisOf(date: LocalDate): Long =
        LocalDateTime
            .of(date, LocalTime.of(9, 45))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

    private fun simulateReboot() {
        ShadowAlarmManager.reset()
    }

    private fun sendBootCompleted() {
        context.sendBroadcast(Intent(context, BootCompletedReceiver::class.java).setAction(Intent.ACTION_BOOT_COMPLETED))
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun pendingAlarmsByScheduleId(): Map<String, Long> =
        shadowOf(alarmManager).scheduledAlarms.associate { alarm ->
            shadowOf(alarm.operation).savedIntent.data!!.lastPathSegment!! to alarm.triggerAtMs
        }

    @Test
    fun `given stored items and a cleared alarm manager when boot completes then the next alarms are recreated`() {
        simulateReboot()
        assertTrue(pendingAlarmsByScheduleId().isEmpty())

        sendBootCompleted()

        assertEquals(
            mapOf(
                "future" to millisOf(LocalDate.of(2026, 11, 8)),
                "daily" to millisOf(LocalDate.of(2026, 11, 8)),
            ),
            pendingAlarmsByScheduleId(),
        )
    }

    @Test
    fun `given boot completes twice when comparing the alarms then the set is identical`() {
        simulateReboot()

        sendBootCompleted()
        val afterFirst = pendingAlarmsByScheduleId()
        sendBootCompleted()

        assertEquals(afterFirst, pendingAlarmsByScheduleId())
        assertEquals(2, shadowOf(alarmManager).scheduledAlarms.size)
    }

    @Test
    fun `given a residual alarm of an item that no longer fires when boot completes then it is cancelled`() {
        alarmScheduler.schedule(AlarmRequest("past", millisOf(LocalDate.of(2026, 11, 9)), AlertPresentationMode.FULL_SCREEN_EVENT))

        sendBootCompleted()

        assertTrue("past" !in pendingAlarmsByScheduleId())
    }

    @Test
    fun `given the app was replaced when the broadcast arrives then the alarms are recreated too`() {
        simulateReboot()

        context.sendBroadcast(Intent(context, BootCompletedReceiver::class.java).setAction(Intent.ACTION_MY_PACKAGE_REPLACED))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(setOf("future", "daily"), pendingAlarmsByScheduleId().keys)
    }
}
