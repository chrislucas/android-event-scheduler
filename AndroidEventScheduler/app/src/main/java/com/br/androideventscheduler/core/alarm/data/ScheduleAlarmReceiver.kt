package com.br.androideventscheduler.core.alarm.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.br.androideventscheduler.core.alarm.data.notification.ScheduleNotifier
import com.br.androideventscheduler.core.alarm.domain.model.AlertPresentationMode
import com.br.androideventscheduler.core.alarm.domain.usecase.ScheduleNextAlarmUseCase
import com.br.androideventscheduler.core.di.DispatcherNames
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named

/**
 * Recebe o disparo do alarme: relê o item atual, notifica e reagenda a próxima ocorrência da série.
 * Usa `goAsync()` para trabalhar fora da main thread; conclui o `PendingResult` em `finally`.
 * O Koin já está pronto (App Startup roda antes dos receivers).
 */
class ScheduleAlarmReceiver :
    BroadcastReceiver(),
    KoinComponent {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val scheduleId = intent.data?.lastPathSegment ?: return
        val requestedMode = requestedMode(intent)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + get<CoroutineDispatcher>(named(DispatcherNames.IO))).launch {
            try {
                handleAlarm(scheduleId, requestedMode)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private suspend fun handleAlarm(
        scheduleId: String,
        requestedMode: AlertPresentationMode?,
    ) {
        val item = runCatching { get<ScheduleRepository>().getById(scheduleId) }.getOrNull() ?: return
        val settings = get<UserSettingsRepository>().current()
        // Falha ao notificar (permissão, sistema) não pode impedir o reagendamento da série.
        try {
            get<ScheduleNotifier>().show(item, settings, requestedMode ?: AlertPresentationMode.forType(item.type))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (
            @Suppress("TooGenericExceptionCaught") failure: Exception,
        ) {
            Log.w(TAG, "Failed to notify schedule $scheduleId", failure)
        }
        get<ScheduleNextAlarmUseCase>()(item)
    }

    private fun requestedMode(intent: Intent): AlertPresentationMode? =
        intent.getStringExtra(AlarmIntents.EXTRA_PRESENTATION_MODE)?.let { modeName ->
            AlertPresentationMode.entries.firstOrNull { it.name == modeName }
        }

    private companion object {
        const val TAG = "ScheduleAlarmReceiver"
    }
}
