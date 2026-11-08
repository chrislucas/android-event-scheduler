package com.br.androideventscheduler.core.alarm.data.notification

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.startup.InitializationProvider
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationChannelsRobolectricTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Test
    fun `given fresh install when ensuring channels then reminders and events exist with portuguese names and high importance`() {
        NotificationChannels.ensureCreated(context)

        val reminders = requireNotNull(notificationManager.getNotificationChannel(NotificationChannels.REMINDERS_CHANNEL_ID))
        val events = requireNotNull(notificationManager.getNotificationChannel(NotificationChannels.EVENTS_CHANNEL_ID))
        assertEquals(context.getString(R.string.notification_channel_reminders_name), reminders.name.toString())
        assertEquals(context.getString(R.string.notification_channel_events_name), events.name.toString())
        assertEquals("Lembretes", reminders.name.toString())
        assertEquals("Eventos", events.name.toString())
        assertEquals(NotificationManager.IMPORTANCE_HIGH, reminders.importance)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, events.importance)
    }

    @Test
    fun `given channels already created when ensuring again then it is idempotent`() {
        NotificationChannels.ensureCreated(context)
        NotificationChannels.ensureCreated(context)

        val channelIds = notificationManager.notificationChannels.map { it.id }
        assertEquals(1, channelIds.count { it == NotificationChannels.REMINDERS_CHANNEL_ID })
        assertEquals(1, channelIds.count { it == NotificationChannels.EVENTS_CHANNEL_ID })
    }

    @Test
    fun `given the app startup initializer when it runs then the channels are created`() {
        NotificationChannelsInitializer().create(context)

        assertNotNull(notificationManager.getNotificationChannel(NotificationChannels.REMINDERS_CHANNEL_ID))
        assertNotNull(notificationManager.getNotificationChannel(NotificationChannels.EVENTS_CHANNEL_ID))
    }

    @Test
    fun `given merged manifest when reading startup provider then the channels initializer is registered`() {
        val providerInfo =
            context.packageManager.getProviderInfo(
                ComponentName(context, InitializationProvider::class.java),
                PackageManager.GET_META_DATA,
            )

        assertEquals("androidx.startup", providerInfo.metaData.getString(NotificationChannelsInitializer::class.java.name))
    }
}
