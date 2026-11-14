package com.br.androideventscheduler.core.alarm.data.notification

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.MainActivity
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.alarm.data.AlarmIntents
import com.br.androideventscheduler.core.alarm.domain.model.AlertPresentationMode
import com.br.androideventscheduler.core.alarm.testing.uninstallFullScreenActivity
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class ScheduleNotifierRobolectricTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val context: Context = application
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // `canUseFullScreenIntent()` só existe no API 34+: o stub é feito por teste, em allowFullScreenAccess().
    private val systemManager = mockk<NotificationManager>(relaxed = true)
    private val notifier = ScheduleNotifier(context, systemManager)

    private fun allowFullScreenAccess() {
        every { systemManager.canUseFullScreenIntent() } returns true
    }

    @Before
    fun setUp() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.ensureCreated(context)
    }

    private fun postedNotification(scheduleId: String = "item-1"): Notification? =
        shadowOf(notificationManager).getNotification(AlarmIntents.notificationId(scheduleId))

    private fun Notification.titleText() = extras.getString(Notification.EXTRA_TITLE)

    private fun Notification.contentText() = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

    @Test
    fun `given a reminder when showing then it posts on the reminders channel with title time and location`() {
        val item =
            scheduleItem(title = "Pagar conta", startTime = LocalTime.of(14, 5), location = "Banco")
                .copy(type = com.br.androideventscheduler.core.schedule.domain.model.ScheduleType.REMINDER)

        val shown = notifier.show(item, UserSettings(), AlertPresentationMode.NOTIFICATION_ONLY)

        val notification = requireNotNull(postedNotification())
        assertEquals(AlertPresentationMode.NOTIFICATION_ONLY, shown)
        assertEquals(NotificationChannels.REMINDERS_CHANNEL_ID, notification.channelId)
        assertEquals("Pagar conta", notification.titleText())
        assertEquals("14:05 • Banco", notification.contentText())
        assertNull(notification.fullScreenIntent)
        assertEquals(Notification.CATEGORY_REMINDER, notification.category)
    }

    @Test
    fun `given an item without location when showing then the text has only the time`() {
        notifier.show(scheduleItem(startTime = LocalTime.of(9, 30)), UserSettings(), AlertPresentationMode.NOTIFICATION_ONLY)

        assertEquals("09:30", postedNotification()?.contentText())
    }

    @Test
    fun `given an all day item when showing then the text says all day instead of midnight`() {
        notifier.show(
            scheduleItem(startTime = LocalTime.MIDNIGHT, isAllDay = true),
            UserSettings(),
            AlertPresentationMode.NOTIFICATION_ONLY,
        )

        assertEquals(context.getString(R.string.notification_all_day), postedNotification()?.contentText())
    }

    @Test
    fun `given a notification when tapping its content intent then it opens main activity with the schedule id`() {
        notifier.show(scheduleItem(id = "abc"), UserSettings(), AlertPresentationMode.NOTIFICATION_ONLY)

        val contentIntent = requireNotNull(postedNotification("abc")?.contentIntent)
        val savedIntent = shadowOf(contentIntent).savedIntent
        assertEquals(MainActivity::class.java.name, savedIntent.component?.className)
        assertEquals("abc", savedIntent.getStringExtra(AlarmIntents.EXTRA_SCHEDULE_ID))
        assertEquals(true, shadowOf(contentIntent).isActivity)
        assertEquals(true, postedNotification("abc")?.flags?.and(Notification.FLAG_AUTO_CANCEL) != 0)
    }

    @Test
    fun `given event with full screen enabled and activity when showing then posts on events channel with full screen intent`() {
        allowFullScreenAccess()

        val shown = notifier.show(scheduleItem(), UserSettings(fullScreenEventAlertEnabled = true), AlertPresentationMode.FULL_SCREEN_EVENT)

        val notification = requireNotNull(postedNotification())
        assertEquals(AlertPresentationMode.FULL_SCREEN_EVENT, shown)
        assertEquals(NotificationChannels.EVENTS_CHANNEL_ID, notification.channelId)
        val fullScreenIntent = requireNotNull(notification.fullScreenIntent)
        val savedIntent = shadowOf(fullScreenIntent).savedIntent
        assertEquals(AlarmIntents.ACTION_SHOW_FULL_SCREEN_ALARM, savedIntent.action)
        assertEquals("item-1", savedIntent.getStringExtra(AlarmIntents.EXTRA_SCHEDULE_ID))
        assertEquals(context.packageName, savedIntent.`package`)
        assertEquals(Notification.CATEGORY_EVENT, notification.category)
    }

    @Test
    fun `given an event with full screen disabled in settings when showing then it degrades to a plain notification`() {
        val shown =
            notifier.show(
                scheduleItem(),
                UserSettings(fullScreenEventAlertEnabled = false),
                AlertPresentationMode.FULL_SCREEN_EVENT,
            )

        val notification = requireNotNull(postedNotification())
        assertEquals(AlertPresentationMode.NOTIFICATION_ONLY, shown)
        assertEquals(NotificationChannels.REMINDERS_CHANNEL_ID, notification.channelId)
        assertNull(notification.fullScreenIntent)
    }

    @Test
    fun `given an event when the full screen activity is not installed then it degrades to a plain notification`() {
        uninstallFullScreenActivity(context)
        allowFullScreenAccess()
        val shown = notifier.show(scheduleItem(), UserSettings(fullScreenEventAlertEnabled = true), AlertPresentationMode.FULL_SCREEN_EVENT)

        assertEquals(AlertPresentationMode.NOTIFICATION_ONLY, shown)
        assertNull(postedNotification()?.fullScreenIntent)
        assertEquals(NotificationChannels.REMINDERS_CHANNEL_ID, postedNotification()?.channelId)
    }

    @Test
    @Config(sdk = [34])
    fun `given api 34 user revoked full screen access when showing an event then it degrades to a plain notification`() {
        val restrictedManager = mockk<NotificationManager> { every { canUseFullScreenIntent() } returns false }

        val restrictedNotifier = ScheduleNotifier(context, restrictedManager)

        val shown = restrictedNotifier.show(scheduleItem(), UserSettings(), AlertPresentationMode.FULL_SCREEN_EVENT)

        assertEquals(AlertPresentationMode.NOTIFICATION_ONLY, shown)
        assertNull(postedNotification()?.fullScreenIntent)
    }

    @Test
    @Config(sdk = [34])
    fun `given api 34 full screen access granted when showing an event then the full screen intent is present`() {
        allowFullScreenAccess()

        notifier.show(scheduleItem(), UserSettings(), AlertPresentationMode.FULL_SCREEN_EVENT)

        assertNotNull(postedNotification()?.fullScreenIntent)
    }

    @Test
    @Config(sdk = [33])
    fun `given api 33 without post notifications permission when showing then nothing is posted and nothing is thrown`() {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val shown = notifier.show(scheduleItem(), UserSettings(), AlertPresentationMode.NOTIFICATION_ONLY)

        assertNull(shown)
        assertNull(postedNotification())
    }

    @Test
    @Config(sdk = [32])
    fun `given api 32 when showing then no runtime permission is required`() {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val shown = notifier.show(scheduleItem(), UserSettings(), AlertPresentationMode.NOTIFICATION_ONLY)

        assertEquals(AlertPresentationMode.NOTIFICATION_ONLY, shown)
        assertNotNull(postedNotification())
    }

    @Test
    fun `given the same item shown twice when showing then the notification is replaced`() {
        notifier.show(scheduleItem(title = "Antes"), UserSettings(), AlertPresentationMode.NOTIFICATION_ONLY)
        notifier.show(scheduleItem(title = "Depois"), UserSettings(), AlertPresentationMode.NOTIFICATION_ONLY)

        assertEquals(1, shadowOf(notificationManager).allNotifications.size)
        assertEquals("Depois", postedNotification()?.titleText())
    }
}
