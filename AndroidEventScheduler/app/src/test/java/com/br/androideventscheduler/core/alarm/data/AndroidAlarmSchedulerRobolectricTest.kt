package com.br.androideventscheduler.core.alarm.data

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.alarm.domain.model.AlarmRequest
import com.br.androideventscheduler.core.alarm.domain.model.AlertPresentationMode
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
class AndroidAlarmSchedulerRobolectricTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val scheduler = AndroidAlarmScheduler(context, alarmManager)

    @After
    fun resetShadow() {
        ShadowAlarmManager.reset()
    }

    private fun pendingAlarm(): ShadowAlarmManager.ScheduledAlarm = requireNotNull(shadowOf(alarmManager).peekNextScheduledAlarm())

    private fun request(
        scheduleId: String = "item-1",
        triggerAtEpochMillis: Long = 1_000_000L,
        mode: AlertPresentationMode = AlertPresentationMode.FULL_SCREEN_EVENT,
    ) = AlarmRequest(scheduleId, triggerAtEpochMillis, mode)

    @Test
    fun `given exact alarms allowed when scheduling then an rtc wakeup alarm is registered at the trigger time`() {
        scheduler.schedule(request(triggerAtEpochMillis = 5_000L))

        val alarm = pendingAlarm()
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.type)
        assertEquals(5_000L, alarm.triggerAtMs)
        assertTrue(alarm.allowWhileIdle)
    }

    @Test
    fun `given alarm pending intent when inspecting it then it targets the receiver with item uri and mode extra`() {
        scheduler.schedule(request(scheduleId = "abc", mode = AlertPresentationMode.NOTIFICATION_ONLY))

        val pendingIntent = shadowOf(pendingAlarm().operation)
        val savedIntent = pendingIntent.savedIntent
        assertEquals("rememberme://alarm/abc", savedIntent.data.toString())
        assertEquals(ScheduleAlarmReceiver::class.java.name, savedIntent.component?.className)
        assertEquals("NOTIFICATION_ONLY", savedIntent.getStringExtra(AlarmIntents.EXTRA_PRESENTATION_MODE))
        assertTrue(pendingIntent.isBroadcastIntent)
        assertTrue(pendingIntent.flags and android.app.PendingIntent.FLAG_IMMUTABLE != 0)
    }

    @Test
    fun `given an alarm already pending for the item when scheduling again then it replaces the previous one`() {
        scheduler.schedule(request(triggerAtEpochMillis = 1_000L))

        scheduler.schedule(request(triggerAtEpochMillis = 9_000L))

        val alarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, alarms.size)
        assertEquals(9_000L, alarms.single().triggerAtMs)
    }

    @Test
    fun `given alarms for two items when scheduling then each item keeps its own alarm`() {
        scheduler.schedule(request(scheduleId = "a"))
        scheduler.schedule(request(scheduleId = "b"))

        assertEquals(2, shadowOf(alarmManager).scheduledAlarms.size)
    }

    @Test
    fun `given a pending alarm when cancelling then it is removed and the pending intent no longer exists`() {
        scheduler.schedule(request())

        scheduler.cancel("item-1")

        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
        assertNull(AlarmIntents.alarmPendingIntent(context, "item-1", AlertPresentationMode.NOTIFICATION_ONLY, create = false))
    }

    @Test
    fun `given no pending alarm when cancelling then nothing happens`() {
        scheduler.cancel("unknown")
        scheduler.cancel("unknown")

        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun `given cancelling one item when another is pending then the other alarm is kept`() {
        scheduler.schedule(request(scheduleId = "a"))
        scheduler.schedule(request(scheduleId = "b"))

        scheduler.cancel("a")

        assertEquals(1, shadowOf(alarmManager).scheduledAlarms.size)
        assertFalse(
            AlarmIntents.alarmPendingIntent(context, "b", AlertPresentationMode.NOTIFICATION_ONLY, create = false) == null,
        )
    }

    @Test
    @Config(sdk = [34])
    fun `given exact alarm permission denied when scheduling then falls back to inexact without throwing`() {
        val manager = mockk<AlarmManager>(relaxed = true)
        every { manager.canScheduleExactAlarms() } returns false

        AndroidAlarmScheduler(context, manager).schedule(request(triggerAtEpochMillis = 7_000L))

        verify(exactly = 0) { manager.setExactAndAllowWhileIdle(any(), any(), any()) }
        verify(exactly = 1) { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 7_000L, any()) }
    }

    @Test
    @Config(sdk = [34])
    fun `given exact alarm permission granted when scheduling then uses the exact api`() {
        val manager = mockk<AlarmManager>(relaxed = true)
        every { manager.canScheduleExactAlarms() } returns true

        AndroidAlarmScheduler(context, manager).schedule(request(triggerAtEpochMillis = 7_000L))

        verify(exactly = 1) { manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 7_000L, any()) }
        verify(exactly = 0) { manager.setAndAllowWhileIdle(any(), any(), any()) }
    }

    @Test
    @Config(sdk = [34])
    fun `given security exception on exact alarm when scheduling then falls back to inexact`() {
        val manager = mockk<AlarmManager>(relaxed = true)
        every { manager.canScheduleExactAlarms() } returns true
        every { manager.setExactAndAllowWhileIdle(any(), any(), any()) } throws SecurityException("revoked")

        AndroidAlarmScheduler(context, manager).schedule(request(triggerAtEpochMillis = 7_000L))

        verify(exactly = 1) { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 7_000L, any()) }
    }

    @Test
    @Config(sdk = [30])
    fun `given api below 31 when scheduling then exact alarm is used without asking the permission state`() {
        val manager = mockk<AlarmManager>(relaxed = true)

        AndroidAlarmScheduler(context, manager).schedule(request(triggerAtEpochMillis = 7_000L))

        verify(exactly = 1) { manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 7_000L, any()) }
        confirmVerified(manager)
    }

    @Test
    @Config(sdk = [34])
    fun `given robolectric shadow denies exact alarms when scheduling then an alarm is still registered`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        scheduler.schedule(request(triggerAtEpochMillis = 3_000L))

        assertEquals(3_000L, pendingAlarm().triggerAtMs)
    }
}
