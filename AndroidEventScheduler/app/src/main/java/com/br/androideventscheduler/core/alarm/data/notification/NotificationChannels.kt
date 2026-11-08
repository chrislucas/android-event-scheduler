package com.br.androideventscheduler.core.alarm.data.notification

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.br.androideventscheduler.R

/** Canais de notificação do app. Criar é idempotente (recriar com o mesmo id não altera o canal). */
object NotificationChannels {
    const val REMINDERS_CHANNEL_ID = "reminders"
    const val EVENTS_CHANNEL_ID = "events"

    /** Cria os canais "Lembretes" e "Eventos" (no-op antes do API 26). Chamado na inicialização do app. */
    fun ensureCreated(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.createNotificationChannelsCompat(
            listOf(
                channel(
                    context = context,
                    channelId = REMINDERS_CHANNEL_ID,
                    nameRes = R.string.notification_channel_reminders_name,
                    descriptionRes = R.string.notification_channel_reminders_description,
                ),
                channel(
                    context = context,
                    channelId = EVENTS_CHANNEL_ID,
                    nameRes = R.string.notification_channel_events_name,
                    descriptionRes = R.string.notification_channel_events_description,
                ),
            ),
        )
    }

    private fun channel(
        context: Context,
        channelId: String,
        nameRes: Int,
        descriptionRes: Int,
    ): NotificationChannelCompat =
        NotificationChannelCompat
            .Builder(channelId, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(nameRes))
            .setDescription(context.getString(descriptionRes))
            .build()
}
