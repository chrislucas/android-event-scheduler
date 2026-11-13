package com.br.androideventscheduler.core.alarm.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.br.androideventscheduler.core.alarm.domain.usecase.RescheduleAllAlarmsUseCase
import com.br.androideventscheduler.core.di.DispatcherNames
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named

/**
 * Reagenda os alarmes de todos os itens quando o `AlarmManager` os perde ou eles podem estar errados:
 * boot, atualização do app, mudança de fuso/hora e mudança da permissão de alarme exato.
 *
 * Só `BOOT_COMPLETED` (nunca `LOCKED_BOOT_COMPLETED`) e sem `directBootAware`: o Room fica em
 * armazenamento protegido por credencial. Não abre UI nem notificações. `goAsync()` com `finish()` em
 * `finally`; o Koin já está pronto (App Startup roda antes dos receivers).
 */
class BootCompletedReceiver :
    BroadcastReceiver(),
    KoinComponent {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in SUPPORTED_ACTIONS) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + get<CoroutineDispatcher>(named(DispatcherNames.IO))).launch {
            try {
                get<RescheduleAllAlarmsUseCase>()()
                    .onSuccess { summary -> Log.i(TAG, "Alarms rescheduled by ${intent.action}: $summary") }
                    .onFailure { failure -> Log.w(TAG, "Failed to reschedule alarms", failure) }
            } catch (
                @Suppress("TooGenericExceptionCaught") failure: Exception,
            ) {
                // Falha ao resolver o use case no Koin: registra e libera o PendingResult em finally.
                Log.w(TAG, "Failed to start the reschedule", failure)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        /** Ação enviada pelo sistema quando a permissão de alarme exato muda (API 31+). */
        const val ACTION_EXACT_ALARM_PERMISSION_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

        /** `Intent.ACTION_TIME_CHANGED` é `android.intent.action.TIME_SET`. */
        val SUPPORTED_ACTIONS: Set<String> =
            setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                ACTION_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            )

        private const val TAG = "BootCompletedReceiver"
    }
}
