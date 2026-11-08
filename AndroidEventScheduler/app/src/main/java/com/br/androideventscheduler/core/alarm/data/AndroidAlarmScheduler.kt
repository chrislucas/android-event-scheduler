package com.br.androideventscheduler.core.alarm.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log
import com.br.androideventscheduler.core.alarm.domain.model.AlarmRequest
import com.br.androideventscheduler.core.alarm.domain.model.AlertPresentationMode
import com.br.androideventscheduler.core.alarm.domain.scheduler.AlarmScheduler

/**
 * [AlarmScheduler] sobre o `AlarmManager`. Usa alarme exato quando permitido (`USE_EXACT_ALARM` no API 33+,
 * `SCHEDULE_EXACT_ALARM` concedido no API 31–32) e cai para o inexato (`setAndAllowWhileIdle`) sem lançar exceção.
 */
internal class AndroidAlarmScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager,
) : AlarmScheduler {
    override fun schedule(request: AlarmRequest) {
        val pendingIntent = requireNotNull(alarmPendingIntent(request.scheduleId, request.presentationMode, create = true))
        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, request.triggerAtEpochMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, request.triggerAtEpochMillis, pendingIntent)
            }
        } catch (securityException: SecurityException) {
            // A permissão pode ser revogada entre a checagem e a chamada: degrada para o alarme inexato.
            Log.w(TAG, "Exact alarm refused for ${request.scheduleId}; falling back to inexact", securityException)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, request.triggerAtEpochMillis, pendingIntent)
        }
    }

    override fun cancel(scheduleId: String) {
        val pendingIntent = alarmPendingIntent(scheduleId, AlertPresentationMode.NOTIFICATION_ONLY, create = false) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun canScheduleExactAlarms(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun alarmPendingIntent(
        scheduleId: String,
        presentationMode: AlertPresentationMode,
        create: Boolean,
    ): PendingIntent? = AlarmIntents.alarmPendingIntent(context, scheduleId, presentationMode, create)

    private companion object {
        const val TAG = "AlarmScheduler"
    }
}
