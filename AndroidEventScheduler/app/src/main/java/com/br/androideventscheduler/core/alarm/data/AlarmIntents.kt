package com.br.androideventscheduler.core.alarm.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.br.androideventscheduler.core.alarm.domain.model.AlertPresentationMode

/**
 * Constantes e fábricas de [Intent]/[PendingIntent] do alarme. Públicas porque a tela cheia (spec 14) e a
 * restauração pós-boot (spec 15) reutilizam a ação, o extra e o id de notificação.
 */
object AlarmIntents {
    /** Ação da atividade de tela cheia (spec 14); declarada por ela, apenas referenciada aqui. */
    const val ACTION_SHOW_FULL_SCREEN_ALARM = "com.br.androideventscheduler.action.SHOW_FULL_SCREEN_ALARM"

    /** Id do agendamento, no `PendingIntent` do alarme, na notificação e na tela cheia. */
    const val EXTRA_SCHEDULE_ID = "com.br.androideventscheduler.extra.SCHEDULE_ID"

    /** Modo de apresentação ([AlertPresentationMode]) pedido ao agendar o alarme. */
    const val EXTRA_PRESENTATION_MODE = "com.br.androideventscheduler.extra.PRESENTATION_MODE"

    internal const val ALARM_URI_SCHEME = "rememberme"
    internal const val ALARM_URI_HOST = "alarm"

    /** Id da notificação de um agendamento (usado para notificar e, na spec 14, para cancelar). */
    fun notificationId(scheduleId: String): Int = scheduleId.hashCode()

    /** `rememberme://alarm/<scheduleId>`: identidade única por item (o `PendingIntent` ignora extras). */
    internal fun alarmUri(scheduleId: String): Uri =
        Uri
            .Builder()
            .scheme(ALARM_URI_SCHEME)
            .authority(ALARM_URI_HOST)
            .appendPath(scheduleId)
            .build()

    internal fun alarmIntent(
        context: Context,
        scheduleId: String,
    ): Intent = Intent(context, ScheduleAlarmReceiver::class.java).setData(alarmUri(scheduleId))

    /** `PendingIntent` do alarme; com [create] `false` devolve `null` se não houver alarme pendente. */
    internal fun alarmPendingIntent(
        context: Context,
        scheduleId: String,
        presentationMode: AlertPresentationMode,
        create: Boolean = true,
    ): PendingIntent? {
        val intent = alarmIntent(context, scheduleId).putExtra(EXTRA_PRESENTATION_MODE, presentationMode.name)
        val flags =
            if (create) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            }
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    /** Intent da atividade de tela cheia (spec 14), restrita ao nosso pacote. */
    fun fullScreenIntent(
        context: Context,
        scheduleId: String,
    ): Intent =
        Intent(ACTION_SHOW_FULL_SCREEN_ALARM)
            .setPackage(context.packageName)
            .putExtra(EXTRA_SCHEDULE_ID, scheduleId)
}
