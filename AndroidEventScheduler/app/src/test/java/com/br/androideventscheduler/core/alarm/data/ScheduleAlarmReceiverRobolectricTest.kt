package com.br.androideventscheduler.core.alarm.data

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.alarm.data.notification.NotificationChannels
import com.br.androideventscheduler.core.alarm.data.notification.ScheduleNotifier
import com.br.androideventscheduler.core.alarm.domain.model.AlarmRequest
import com.br.androideventscheduler.core.alarm.domain.model.AlertPresentationMode
import com.br.androideventscheduler.core.alarm.domain.scheduler.AlarmScheduler
import com.br.androideventscheduler.core.alarm.domain.usecase.ScheduleNextAlarmUseCase
import com.br.androideventscheduler.core.alarm.testing.uninstallFullScreenActivity
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
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Dispara o receiver como o sistema faz (`sendBroadcast` para o componente declarado no manifest) com o
 * grafo do Koin sobrescrito por fakes. O dispatcher de I/O vira um `Unconfined` para o trabalho do
 * `goAsync` terminar de forma determinística dentro do teste.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScheduleAlarmReceiverRobolectricTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val context: Context = application
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val scheduleRepository = mockk<ScheduleRepository>()
    private val settingsRepository = FakeUserSettingsRepository(UserSettings(fullScreenEventAlertEnabled = true))
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private val systemManager = mockk<NotificationManager>(relaxed = true)
    private val clock = FakeClock(Instant.parse("2026-11-07T15:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private var notifier: ScheduleNotifier = ScheduleNotifier(context, systemManager)

    private val overrides =
        module {
            single<CoroutineDispatcher>(named(DispatcherNames.IO)) { UnconfinedTestDispatcher() }
            single<ScheduleRepository> { scheduleRepository }
            single<UserSettingsRepository> { settingsRepository }
            single<AlarmScheduler> { alarmScheduler }
            single { notifier }
            single { ScheduleNextAlarmUseCase(RecurrenceEngine(), alarmScheduler, clock) }
        }

    @Before
    fun setUp() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.ensureCreated(context)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            every { systemManager.canUseFullScreenIntent() } returns true
        }
        loadKoinModules(overrides)
    }

    @After
    fun tearDown() {
        unloadKoinModules(overrides)
    }

    private fun givenStoredItem(item: ScheduleItem?) {
        coEvery { scheduleRepository.getById(any()) } returns item
    }

    private fun fireAlarm(
        scheduleId: String = "item-1",
        mode: AlertPresentationMode? = null,
    ) {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).setData(AlarmIntents.alarmUri(scheduleId))
        mode?.let { intent.putExtra(AlarmIntents.EXTRA_PRESENTATION_MODE, it.name) }
        context.sendBroadcast(intent)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun postedNotification(scheduleId: String = "item-1"): Notification? =
        shadowOf(notificationManager).getNotification(AlarmIntents.notificationId(scheduleId))

    private val dailyReminder =
        scheduleItem(
            title = "Remédio",
            type = ScheduleType.REMINDER,
            startDate = LocalDate.of(2026, 11, 1),
            recurrencePattern = RecurrencePattern.Daily(),
        )

    @Test
    fun `given a reminder alarm when it fires then a notification is posted on the reminders channel`() {
        givenStoredItem(dailyReminder)

        fireAlarm()

        val notification = requireNotNull(postedNotification())
        assertEquals(NotificationChannels.REMINDERS_CHANNEL_ID, notification.channelId)
        assertEquals("Remédio", notification.extras.getString(Notification.EXTRA_TITLE))
        assertNull(notification.fullScreenIntent)
    }

    @Test
    fun `given event with full screen enabled and activity when the alarm fires then the notification has a full screen intent`() {
        givenStoredItem(scheduleItem(type = ScheduleType.EVENT))

        fireAlarm(mode = AlertPresentationMode.FULL_SCREEN_EVENT)

        val notification = requireNotNull(postedNotification())
        assertEquals(NotificationChannels.EVENTS_CHANNEL_ID, notification.channelId)
        assertNotNull(notification.fullScreenIntent)
    }

    @Test
    fun `given an event with full screen disabled when the alarm fires then it degrades to a plain notification`() {
        settingsRepository.settings.value = UserSettings(fullScreenEventAlertEnabled = false)
        givenStoredItem(scheduleItem(type = ScheduleType.EVENT))

        fireAlarm(mode = AlertPresentationMode.FULL_SCREEN_EVENT)

        val notification = requireNotNull(postedNotification())
        assertEquals(NotificationChannels.REMINDERS_CHANNEL_ID, notification.channelId)
        assertNull(notification.fullScreenIntent)
    }

    @Test
    fun `given an event when the full screen activity is not installed then it degrades to a plain notification`() {
        uninstallFullScreenActivity(context)
        givenStoredItem(scheduleItem(type = ScheduleType.EVENT))

        fireAlarm(mode = AlertPresentationMode.FULL_SCREEN_EVENT)

        val notification = requireNotNull(postedNotification())
        assertEquals(NotificationChannels.REMINDERS_CHANNEL_ID, notification.channelId)
        assertNull(notification.fullScreenIntent)
    }

    @Test
    fun `given an alarm without mode extra when it fires then the item type decides the mode`() {
        givenStoredItem(scheduleItem(type = ScheduleType.EVENT))

        fireAlarm(mode = null)

        assertNotNull(postedNotification()?.fullScreenIntent)
    }

    @Test
    fun `given an item that no longer exists when the alarm fires then nothing is notified or rescheduled`() {
        givenStoredItem(null)

        fireAlarm()

        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
        verify(exactly = 0) { alarmScheduler.schedule(any()) }
        verify(exactly = 0) { alarmScheduler.cancel(any()) }
    }

    @Test
    fun `given a recurring item when the alarm fires then the next occurrence is rescheduled`() {
        givenStoredItem(dailyReminder)

        fireAlarm()

        val requestSlot = slot<AlarmRequest>()
        verify(exactly = 1) { alarmScheduler.schedule(capture(requestSlot)) }
        assertEquals("item-1", requestSlot.captured.scheduleId)
        assertTrue(requestSlot.captured.triggerAtEpochMillis > clock.millis())
        assertEquals(AlertPresentationMode.NOTIFICATION_ONLY, requestSlot.captured.presentationMode)
    }

    @Test
    fun `given a single occurrence item when the alarm fires then the pending alarm is cancelled after notifying`() {
        givenStoredItem(scheduleItem(startDate = LocalDate.of(2026, 11, 7)))

        fireAlarm()

        assertNotNull(postedNotification())
        verify(exactly = 1) { alarmScheduler.cancel("item-1") }
        verify(exactly = 0) { alarmScheduler.schedule(any()) }
    }

    @Test
    @Config(sdk = [33])
    fun `given notification permission denied when the alarm fires then nothing is posted but the series is still rescheduled`() {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        givenStoredItem(dailyReminder)

        fireAlarm()

        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
        verify(exactly = 1) { alarmScheduler.schedule(any()) }
    }

    @Test
    fun `given the notifier throws when the alarm fires then the series is still rescheduled`() {
        notifier = mockk { every { show(any(), any(), any()) } throws IllegalStateException("boom") }
        unloadKoinModules(overrides)
        loadKoinModules(overrides)
        givenStoredItem(dailyReminder)

        fireAlarm()

        verify(exactly = 1) { alarmScheduler.schedule(any()) }
    }

    @Test
    fun `given an intent without schedule id when the alarm fires then the receiver ignores it`() {
        givenStoredItem(dailyReminder)

        context.sendBroadcast(Intent(context, ScheduleAlarmReceiver::class.java))
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
        verify(exactly = 0) { alarmScheduler.schedule(any()) }
    }
}
