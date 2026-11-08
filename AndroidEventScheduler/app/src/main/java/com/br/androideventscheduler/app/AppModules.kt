package com.br.androideventscheduler.app

import com.br.androideventscheduler.core.database.databaseModule
import com.br.androideventscheduler.core.datastore.dataStoreModule
import com.br.androideventscheduler.core.di.dispatchersModule
import com.br.androideventscheduler.core.preferences.di.preferencesModule
import com.br.androideventscheduler.core.schedule.di.scheduleCoreModule
import com.br.androideventscheduler.feature.settings.di.settingsModule
import com.br.androideventscheduler.feature.splash.di.splashModule
import org.koin.core.module.Module

/** Lista única de módulos Koin: `core` primeiro, depois um módulo por feature (cada spec acrescenta o seu). */
val appModules: List<Module> =
    listOf(
        dispatchersModule,
        databaseModule,
        scheduleCoreModule,
        dataStoreModule,
        preferencesModule,
        splashModule,
        settingsModule,
    )
