package com.br.androideventscheduler.core.alarm.data.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.br.androideventscheduler.MainActivity
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.alarm.data.AlarmIntents
import com.br.androideventscheduler.core.alarm.domain.model.AlertPresentationMode
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.ui.format.toTimeLabel

/**
 * Monta e publica a notificação de um agendamento. Detalhe de plataforma (sem UseCase): decide o modo
 * efetivo (tela cheia só se pedido, habilitado nas preferências, permitido pelo sistema e com a atividade
 * instalada; senão degrada para notificação comum) e nunca lança por falta de permissão.
 */
internal class ScheduleNotifier(
    private val context: Context,
    private val systemNotificationManager: NotificationManager = context.getSystemService(NotificationManager::class.java),
) {
    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)

    /** Devolve o modo efetivamente usado, ou `null` se nada foi exibido (sem permissão ou falha do sistema). */
    fun show(
        item: ScheduleItem,
        settings: UserSettings,
        requestedMode: AlertPresentationMode,
    ): AlertPresentationMode? {
        if (!canPostNotifications()) return null
        val effectiveMode = effectiveMode(item.id, requestedMode, settings)
        val notification = buildNotification(item, effectiveMode)
        return try {
            @Suppress("MissingPermission") // POST_NOTIFICATIONS conferida em canPostNotifications().
            notificationManager.notify(AlarmIntents.notificationId(item.id), notification)
            effectiveMode
        } catch (securityException: SecurityException) {
            Log.w(TAG, "Notification refused for ${item.id}", securityException)
            null
        }
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun effectiveMode(
        scheduleId: String,
        requestedMode: AlertPresentationMode,
        settings: UserSettings,
    ): AlertPresentationMode {
        val canShowFullScreen =
            requestedMode == AlertPresentationMode.FULL_SCREEN_EVENT &&
                settings.fullScreenEventAlertEnabled &&
                canUseFullScreenIntent() &&
                isFullScreenActivityInstalled(scheduleId)
        return if (canShowFullScreen) AlertPresentationMode.FULL_SCREEN_EVENT else AlertPresentationMode.NOTIFICATION_ONLY
    }

    /** No API 34+ o usuário pode revogar o acesso; antes, `USE_FULL_SCREEN_INTENT` é concedida na instalação. */
    private fun canUseFullScreenIntent(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            systemNotificationManager.canUseFullScreenIntent()

    private fun isFullScreenActivityInstalled(scheduleId: String): Boolean =
        context.packageManager.resolveActivity(AlarmIntents.fullScreenIntent(context, scheduleId), 0) != null

    private fun buildNotification(
        item: ScheduleItem,
        mode: AlertPresentationMode,
    ) = NotificationCompat
        .Builder(context, channelFor(mode))
        .setSmallIcon(R.drawable.ic_bell)
        .setContentTitle(item.title)
        .setContentText(contentText(item))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(
            if (mode ==
                AlertPresentationMode.FULL_SCREEN_EVENT
            ) {
                NotificationCompat.CATEGORY_EVENT
            } else {
                NotificationCompat.CATEGORY_REMINDER
            },
        ).setAutoCancel(true)
        .setContentIntent(openDetailPendingIntent(item.id))
        .apply {
            if (mode == AlertPresentationMode.FULL_SCREEN_EVENT) {
                setFullScreenIntent(fullScreenPendingIntent(item.id), true)
            }
        }.build()

    private fun channelFor(mode: AlertPresentationMode): String =
        if (mode == AlertPresentationMode.FULL_SCREEN_EVENT) {
            NotificationChannels.EVENTS_CHANNEL_ID
        } else {
            NotificationChannels.REMINDERS_CHANNEL_ID
        }

    private fun contentText(item: ScheduleItem): String {
        val timeLabel = if (item.isAllDay) context.getString(R.string.notification_all_day) else item.startTime.toTimeLabel()
        return if (item.location.isBlank()) {
            context.getString(R.string.notification_text_time, timeLabel)
        } else {
            context.getString(R.string.notification_text_time_location, timeLabel, item.location)
        }
    }

    /** Toque na notificação: traz a `MainActivity` à frente (ou a cria) com o id para o deep link. */
    private fun openDetailPendingIntent(scheduleId: String): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java)
                .setData("${AlarmIntents.ALARM_URI_SCHEME}://schedule/${Uri.encode(scheduleId)}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(AlarmIntents.EXTRA_SCHEDULE_ID, scheduleId)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun fullScreenPendingIntent(scheduleId: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            // O intent não tem `data` (o filtro da tela cheia não a declara): o requestCode distingue os itens.
            AlarmIntents.notificationId(scheduleId),
            AlarmIntents.fullScreenIntent(context, scheduleId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private companion object {
        const val TAG = "ScheduleNotifier"
    }
}
