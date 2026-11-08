package com.br.androideventscheduler

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.br.androideventscheduler.app.AppNavHost
import com.br.androideventscheduler.app.DeepLinkState
import com.br.androideventscheduler.core.alarm.data.AlarmIntents
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val deepLinkState: DeepLinkState by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // SplashScreen API: precisa vir antes de super.onCreate para trocar o tema de partida pelo principal.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Só no primeiro nascimento: após rotação/recriação o intent original voltaria e reabriria o detalhe.
        if (savedInstanceState == null) registerDeepLink(intent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        registerDeepLink(intent)
    }

    /** Toque na notificação traz o id do agendamento; aberturas pelo histórico de recentes repetiriam um id antigo. */
    private fun registerDeepLink(intent: Intent?) {
        val launchedFromHistory = intent?.flags?.and(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        if (!launchedFromHistory) deepLinkState.set(intent?.getStringExtra(AlarmIntents.EXTRA_SCHEDULE_ID))
    }
}
