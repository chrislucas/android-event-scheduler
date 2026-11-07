package com.br.androideventscheduler.app

import android.app.Application
import androidx.startup.AppInitializer

/**
 * Application usada por todos os testes Robolectric (ver `robolectric.properties`).
 *
 * O Robolectric não instancia os `ContentProvider`s do manifest, então o `InitializationProvider` do
 * App Startup não roda sozinho nos testes. Aqui disparamos o MESMO caminho de produção
 * ([AppInitializer] + [KoinInitializer]) para que activities e receivers testados encontrem o grafo.
 */
class TestRememberMeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppInitializer.getInstance(this).initializeComponent(KoinInitializer::class.java)
    }
}
