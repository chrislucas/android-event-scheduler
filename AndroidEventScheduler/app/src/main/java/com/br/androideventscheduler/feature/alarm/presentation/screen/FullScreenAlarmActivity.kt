package com.br.androideventscheduler.feature.alarm.presentation.screen

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.br.androideventscheduler.core.alarm.data.AlarmIntents
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.designsystem.isDark
import com.br.androideventscheduler.core.preferences.domain.ThemePreferenceRepository
import com.br.androideventscheduler.core.ui.theme.SystemThemeEffects
import org.koin.android.ext.android.inject

// Sem preview: Activity (ponto de entrada do sistema); as previews estão em FullScreenAlarmScreen.kt.

/**
 * Alerta de evento em tela cheia, aberto pelo `fullScreenIntent` da notificação (spec 04) com a ação
 * [AlarmIntents.ACTION_SHOW_FULL_SCREEN_ALARM] e o extra [AlarmIntents.EXTRA_SCHEDULE_ID]. Aparece sobre a tela de
 * bloqueio e liga a tela sem exigir nenhuma permissão extra. Não é exportada: só o sistema a abre, via `PendingIntent`.
 */
class FullScreenAlarmActivity : ComponentActivity() {
    private val themePreferenceRepository: ThemePreferenceRepository by inject()

    private var scheduleId: String? by mutableStateOf(null)

    /** Sobe a cada `onNewIntent`: faz a tela recarregar o item mesmo quando o id é o mesmo. */
    private var reloadSignal: Int by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        // Após rotação/recriação o `intent` original volta, mesmo que um `onNewIntent` tenha trocado o item.
        val requestedScheduleId = savedInstanceState?.getString(STATE_SCHEDULE_ID) ?: scheduleIdOf(intent)
        if (requestedScheduleId == null) {
            finish()
            return
        }
        scheduleId = requestedScheduleId
        // Borda a borda desde o primeiro quadro (ícones claros, o padrão escuro); o SystemThemeEffects ajusta ao tema.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            // Mesmo tema do app (spec 19). Sem Splash aqui: até o valor chegar (poucos ms) a janela mostra o fundo escuro
            // do tema XML e nada é composto.
            val themeMode by themePreferenceRepository.observe().collectAsStateWithLifecycle(initialValue = null)
            val resolvedMode = themeMode ?: return@setContent
            val darkTheme = resolvedMode.isDark(systemInDarkTheme = isSystemInDarkTheme())
            AndroidEventSchedulerTheme(darkTheme = darkTheme) {
                SystemThemeEffects(darkTheme = darkTheme)
                scheduleId?.let { currentId ->
                    FullScreenAlarmScreen(scheduleId = currentId, reloadSignal = reloadSignal, onClose = ::closeScreen)
                }
            }
        }
    }

    /** Novo alarme com a tela aberta (`singleTop`): troca/recarrega o item em vez de empilhar outra tela. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newScheduleId = scheduleIdOf(intent) ?: return
        scheduleId = newScheduleId
        reloadSignal++
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SCHEDULE_ID, scheduleId)
    }

    private fun scheduleIdOf(intent: Intent): String? = intent.getStringExtra(AlarmIntents.EXTRA_SCHEDULE_ID)?.takeIf { it.isNotBlank() }

    /**
     * `setShowWhenLocked`/`setTurnScreenOn` existem a partir do API 27 (sem permissão); antes, os flags de janela
     * equivalentes. Em ambos, mantém a tela acesa enquanto o alerta está visível.
     */
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun closeScreen(cancelNotification: Boolean) {
        if (cancelNotification) {
            scheduleId?.let { id -> NotificationManagerCompat.from(this).cancel(AlarmIntents.notificationId(id)) }
        }
        finish()
    }

    private companion object {
        const val STATE_SCHEDULE_ID = "schedule_id"
    }
}
