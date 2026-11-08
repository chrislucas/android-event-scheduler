package com.br.androideventscheduler.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules

/**
 * Application usada por todos os testes Robolectric (ver `robolectric.properties`).
 *
 * 1. O Robolectric não instancia os `ContentProvider`s do manifest, então o `InitializationProvider`
 *    do App Startup não roda sozinho: no primeiro teste chamamos o mesmo [KoinInitializer] de produção.
 * 2. Cada teste recebe uma Application e diretórios de arquivos **novos**, mas o Koin é global e
 *    reiniciá-lo (`stopKoin`) entre testes com Activity não é confiável no Robolectric. Então, a partir
 *    do segundo teste, trocamos o `Context` e recarregamos os módulos: os `single` (DataStore, Room)
 *    são recriados apontando para os arquivos do teste atual.
 */
class TestRememberMeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val runningKoin = GlobalContext.getKoinApplicationOrNull()
        if (runningKoin == null) {
            KoinInitializer().create(this)
        } else {
            runningKoin.androidContext(this)
            unloadKoinModules(appModules)
            loadKoinModules(appModules)
        }
    }
}
