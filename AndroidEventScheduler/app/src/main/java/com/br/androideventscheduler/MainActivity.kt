package com.br.androideventscheduler

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.br.androideventscheduler.app.AppNavHost
import com.br.androideventscheduler.app.DeepLinkState
import com.br.androideventscheduler.app.IncomingFileState
import com.br.androideventscheduler.app.incomingAgendaFileUri
import com.br.androideventscheduler.core.alarm.data.AlarmIntents
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import com.br.androideventscheduler.core.security.data.CredentialLauncherRegistry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

// FragmentActivity: exigência do BiometricPrompt (spec 16); o Compose (setContent) funciona igual.
class MainActivity : FragmentActivity() {
    private val deepLinkState: DeepLinkState by inject()
    private val incomingFileState: IncomingFileState by inject()
    private val credentialLauncherRegistry: CredentialLauncherRegistry by inject()
    private val userSettingsRepository: UserSettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // SplashScreen API: precisa vir antes de super.onCreate para trocar o tema de partida pelo principal.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Confirmação de credencial em API 24–29 (Activity Result): precisa ser registrada antes de STARTED (spec 16).
        credentialLauncherRegistry.attach(this)
        // Só no primeiro nascimento: após rotação/recriação o intent original voltaria e reabriria o detalhe.
        if (savedInstanceState == null) {
            registerDeepLink(intent)
            registerIncomingFile(intent)
        }
        applySecureFlagFromPreference()
        // Tema escuro fixo: barras do sistema sempre com ícones claros, independente do modo do sistema.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            AndroidEventSchedulerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavHost()
                }
            }
        }
    }

    /**
     * `FLAG_SECURE` só com a proteção ligada (spec 16, R4.1): a miniatura em "Recentes" não mostra a agenda e capturas
     * de tela ficam bloqueadas. Acompanha a preferência enquanto a Activity existe.
     */
    private fun applySecureFlagFromPreference() {
        lifecycleScope.launch {
            userSettingsRepository
                .observe()
                .map { settings -> settings.appLockEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) {
                        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        registerDeepLink(intent)
        registerIncomingFile(intent)
    }

    /** Toque na notificação traz o id do agendamento; aberturas pelo histórico de recentes repetiriam um id antigo. */
    private fun registerDeepLink(intent: Intent?) {
        val launchedFromHistory = intent?.flags?.and(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        if (!launchedFromHistory) deepLinkState.set(intent?.getStringExtra(AlarmIntents.EXTRA_SCHEDULE_ID))
    }

    /**
     * Arquivo de agenda recebido por `VIEW`/`SEND` (spec 17). O intent é de origem não confiável: só a URI validada por
     * [incomingAgendaFileUri] é guardada; reaberturas pelo histórico de recentes não repetem o arquivo antigo.
     */
    private fun registerIncomingFile(intent: Intent?) {
        val launchedFromHistory = intent?.flags?.and(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        if (!launchedFromHistory) incomingFileState.set(intent?.incomingAgendaFileUri())
    }
}
