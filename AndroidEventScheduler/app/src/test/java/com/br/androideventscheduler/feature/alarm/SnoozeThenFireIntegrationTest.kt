package com.br.androideventscheduler.feature.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.alarm.data.AlarmIntents
import com.br.androideventscheduler.core.alarm.data.AndroidAlarmScheduler
import com.br.androideventscheduler.core.alarm.data.notification.NotificationChannels
import com.br.androideventscheduler.core.alarm.data.notification.ScheduleNotifier
import com.br.androideventscheduler.core.alarm.domain.model.AlertPresentationMode
import com.br.androideventscheduler.core.alarm.domain.usecase.ScheduleNextAlarmUseCase
import com.br.androideventscheduler.core.alarm.domain.usecase.SnoozeAlarmUseCase
import com.br.androideventscheduler.core.di.DispatcherNames
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import com.br.androideventscheduler.core.preferences.testing.FakeUserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.domain.recurrence.RecurrenceEngine
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.FakeClock
import com.br.androideventscheduler.core.testing.pendingOperation
import com.br.androideventscheduler.feature.alarm.presentation.screen.FullScreenAlarmActivity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Adiar → disparar (spec 14 R2.4): usa o `AlarmManager` simulado, o agendador real e o receiver real (spec 04).
 * Mostra que o adiamento substitui o alarme pendente sem alterar o item e que, ao disparar, o receiver
 * apresenta o alerta e reagenda a próxima ocorrência da série (ou nada, se a série acabou).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SnoozeThenFireIntegrationTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val context: Context = application
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val zoneId = ZoneId.of("America/Sao_Paulo")

    // 2026-11-07 12:00 em São Paulo.
    private val clock = FakeClock(Instant.parse("2026-11-07T15:00:00Z"), zoneId)
    private val scheduleRepository = mockk<ScheduleRepository>(relaxed = true)
    private val alarmScheduler = AndroidAlarmScheduler(context, alarmManager)
    private val systemManager = mockk<NotificationManager>(relaxed = true)
    private val recurrenceEngine = RecurrenceEngine()
    private val scheduleNextAlarm = ScheduleNextAlarmUseCase(recurrenceEngine, alarmScheduler, clock)
    private val snoozeAlarm = SnoozeAlarmUseCase(scheduleRepository, alarmScheduler, clock)

    private val overrides =
        module {
            single<CoroutineDispatcher>(named(DispatcherNames.IO)) { UnconfinedTestDispatcher() }
            single<Clock> { clock }
            single<ScheduleRepository> { scheduleRepository }
            single<UserSettingsRepository> { FakeUserSettingsRepository(UserSettings(fullScreenEventAlertEnabled = true)) }
            single { ScheduleNotifier(context, systemManager) }
            single { scheduleNextAlarm }
        }

    @Before
    fun setUp() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.ensureCreated(context)
        every { systemManager.canUseFullScreenIntent() } returns true
        loadKoinModules(overrides)
    }

    @After
    fun tearDown() {
        unloadKoinModules(overrides)
        ShadowAlarmManager.reset()
    }

    private fun millisOf(
        date: LocalDate,
        time: LocalTime,
    ): Long =
        LocalDateTime
            .of(date, time)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

    private fun givenStoredItem(item: ScheduleItem) {
        coEvery { scheduleRepository.getById(item.id) } returns item
    }

    private fun snooze(minutes: Int): Result<Long> = runBlocking { snoozeAlarm("item-1", minutes) }

    private fun scheduledAlarms() = shadowOf(alarmManager).scheduledAlarms

    /** Dispara o `PendingIntent` do alarme pendente, como o `AlarmManager` faria no horário. */
    private fun fireScheduledAlarm() {
        val alarm = scheduledAlarms().single()
        val firedIntent: Intent = shadowOf(alarm.pendingOperation()).savedIntent
        context.sendBroadcast(firedIntent)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun postedNotification(scheduleId: String = "item-1"): Notification? =
        shadowOf(notificationManager).getNotification(AlarmIntents.notificationId(scheduleId))

    private val dailyEvent =
        scheduleItem(
            title = "Aula",
            type = ScheduleType.EVENT,
            startDate = LocalDate.of(2026, 11, 1),
            startTime = LocalTime.of(18, 0),
            recurrencePattern = RecurrencePattern.Daily(),
        )

    @Test
    fun `given a daily event when snoozed and fired then the series alarm is restored to the next occurrence`() {
        givenStoredItem(dailyEvent)
        // Alarme regular: hoje às 17:45 (18:00 − 15 min).
        scheduleNextAlarm(dailyEvent)
        val regularTriggerMillis = millisOf(LocalDate.of(2026, 11, 7), LocalTime.of(17, 45))
        assertEquals(regularTriggerMillis, scheduledAlarms().single().triggerAtMs)

        // O usuário adia 10 minutos: o alarme pendente é substituído, sem tocar no item.
        val snoozedTriggerMillis = snooze(10).getOrThrow()

        assertEquals(clock.millis() + Duration.ofMinutes(10).toMillis(), snoozedTriggerMillis)
        assertEquals(snoozedTriggerMillis, scheduledAlarms().single().triggerAtMs)
        coVerify(exactly = 0) { scheduleRepository.upsert(any()) }

        // O alarme adiado dispara: o receiver apresenta o evento e a série volta ao fluxo regular.
        clock.advanceBy(Duration.ofMinutes(10))
        fireScheduledAlarm()

        val notification = requireNotNull(postedNotification())
        assertEquals(NotificationChannels.EVENTS_CHANNEL_ID, notification.channelId)
        assertNotNull(notification.fullScreenIntent)
        assertEquals(regularTriggerMillis, scheduledAlarms().single().triggerAtMs)
    }

    @Test
    fun `given a single event when snoozed and fired then it is presented and no further alarm remains`() {
        val singleEvent =
            scheduleItem(
                title = "Reunião",
                type = ScheduleType.EVENT,
                startDate = LocalDate.of(2026, 11, 7),
                startTime = LocalTime.of(12, 30),
            )
        givenStoredItem(singleEvent)
        // Alarme regular às 12:15; o app o recebe e o usuário adia 10 minutos.
        clock.setInstant(Instant.parse("2026-11-07T15:15:00Z"))
        snooze(10).getOrThrow()
        assertEquals(millisOf(LocalDate.of(2026, 11, 7), LocalTime.of(12, 25)), scheduledAlarms().single().triggerAtMs)

        clock.advanceBy(Duration.ofMinutes(10))
        fireScheduledAlarm()

        assertNotNull(postedNotification())
        assertTrue(scheduledAlarms().isEmpty())
    }

    @Test
    fun `given a snoozed reminder when fired then it is a plain notification`() {
        val reminder = dailyEvent.copy(type = ScheduleType.REMINDER)
        givenStoredItem(reminder)
        snooze(5).getOrThrow()
        assertEquals(
            AlertPresentationMode.NOTIFICATION_ONLY.name,
            shadowOf(scheduledAlarms().single().pendingOperation()).savedIntent.getStringExtra(AlarmIntents.EXTRA_PRESENTATION_MODE),
        )

        clock.advanceBy(Duration.ofMinutes(5))
        fireScheduledAlarm()

        assertNull(requireNotNull(postedNotification()).fullScreenIntent)
    }

    @Test
    fun `given a fired event with full screen when the system launches its intent then the activity opens for that item`() {
        givenStoredItem(dailyEvent)
        snooze(10).getOrThrow()
        clock.advanceBy(Duration.ofMinutes(10))
        fireScheduledAlarm()
        val fullScreenPendingIntent = requireNotNull(postedNotification()?.fullScreenIntent)

        val launchedIntent = shadowOf(fullScreenPendingIntent).savedIntent.setClass(context, FullScreenAlarmActivity::class.java)
        ActivityScenario.launch<FullScreenAlarmActivity>(launchedIntent).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(AlarmIntents.ACTION_SHOW_FULL_SCREEN_ALARM, activity.intent.action)
                assertEquals("item-1", activity.intent.getStringExtra(AlarmIntents.EXTRA_SCHEDULE_ID))
                assertNull(activity.intent.data)
            }
        }
    }
}
