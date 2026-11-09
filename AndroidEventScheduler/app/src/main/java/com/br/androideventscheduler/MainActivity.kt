package com.br.androideventscheduler

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.br.androideventscheduler.app.AppNavHost
import com.br.androideventscheduler.app.DeepLinkState
import com.br.androideventscheduler.app.IncomingFileState
import com.br.androideventscheduler.app.incomingAgendaFileUri
import com.br.androideventscheduler.core.alarm.data.AlarmIntents
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.designsystem.isDark
import com.br.androideventscheduler.core.preferences.domain.ThemeMode
import com.br.androideventscheduler.core.preferences.domain.ThemePreferenceRepository
import com.br.androideventscheduler.core.ui.theme.SystemThemeEffects
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val deepLinkState: DeepLinkState by inject()
    private val incomingFileState: IncomingFileState by inject()
    private val themePreferenceRepository: ThemePreferenceRepository by inject()

    /**
     * Tema escolhido (spec 19). `null` até o DataStore responder: a Splash segue na tela até lá, sem `runBlocking` na
     * main thread. A leitura começa já na criação da Activity, antes do `setContent`.
     */
    private val themeMode: StateFlow<ThemeMode?> by lazy {
        themePreferenceRepository.observe().stateIn(lifecycleScope, SharingStarted.Eagerly, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // SplashScreen API: precisa vir antes de super.onCreate para trocar o tema de partida pelo principal.
        // A Splash fica até a preferência de tema ser lida: o primeiro quadro do Compose já sai no tema certo.
        installSplashScreen().setKeepOnScreenCondition { themeMode.value == null }
        super.onCreate(savedInstanceState)
        // Só no primeiro nascimento: após rotação/recriação o intent original voltaria e reabriria o detalhe.
        if (savedInstanceState == null) {
            registerDeepLink(intent)
            registerIncomingFile(intent)
        }
        // Borda a borda desde o primeiro quadro (ícones claros, o padrão escuro); o SystemThemeEffects ajusta ao tema.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            // Enquanto a preferência não chega a Splash ainda cobre a tela: nada é composto.
            val resolvedMode = themeMode.collectAsStateWithLifecycle().value ?: return@setContent
            val darkTheme = resolvedMode.isDark(systemInDarkTheme = isSystemInDarkTheme())
            AndroidEventSchedulerTheme(darkTheme = darkTheme) {
                SystemThemeEffects(darkTheme = darkTheme)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavHost()
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
