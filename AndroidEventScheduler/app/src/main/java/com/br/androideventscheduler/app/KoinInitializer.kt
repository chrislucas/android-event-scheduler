package com.br.androideventscheduler.app

import android.content.Context
import androidx.startup.Initializer
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
            }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
