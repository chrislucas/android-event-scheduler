package com.br.androideventscheduler.app

import android.content.Context
import androidx.lifecycle.ProcessLifecycleInitializer
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.startup.Initializer
import com.br.androideventscheduler.core.security.data.AppLockController
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

/**
 * Inicializa o Koin pelo Jetpack App Startup (`InitializationProvider`), que roda **antes** de
 * `Application.onCreate`, de Activities e de BroadcastReceivers — por isso receivers (alarmes, boot)
 * já encontram o grafo pronto.
 */
class KoinInitializer : Initializer<KoinApplication> {
    override fun create(context: Context): KoinApplication =
        GlobalContext.getKoinApplicationOrNull()
            ?: startKoin {
                androidLogger()
                androidContext(context.applicationContext)
                modules(appModules)
            }.also { koinApplication ->
                // Bloqueio do app (spec 16): o controller observa o ciclo de vida do processo, que ignora rotação (R2.3).
                ProcessLifecycleOwner.get().lifecycle.addObserver(koinApplication.koin.get<AppLockController>())
            }

    // O ProcessLifecycleOwner precisa estar inicializado antes de receber o observer do bloqueio.
    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(ProcessLifecycleInitializer::class.java)
}
