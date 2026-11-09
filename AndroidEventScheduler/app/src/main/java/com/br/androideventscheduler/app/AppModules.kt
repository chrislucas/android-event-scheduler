package com.br.androideventscheduler.app

import com.br.androideventscheduler.core.alarm.di.alarmModule
import com.br.androideventscheduler.core.common.commonModule
import com.br.androideventscheduler.core.database.databaseModule
import com.br.androideventscheduler.core.datastore.dataStoreModule
import com.br.androideventscheduler.core.di.dispatchersModule
import com.br.androideventscheduler.core.preferences.di.preferencesModule
import com.br.androideventscheduler.core.schedule.di.scheduleCoreModule
import com.br.androideventscheduler.core.security.di.securityModule
import com.br.androideventscheduler.feature.alarm.di.alarmFeatureModule
import com.br.androideventscheduler.feature.backup.di.backupModule
import com.br.androideventscheduler.feature.detail.di.detailModule
import com.br.androideventscheduler.feature.home.di.homeModule
import com.br.androideventscheduler.feature.search.di.searchModule
import com.br.androideventscheduler.feature.settings.di.settingsModule
import com.br.androideventscheduler.feature.splash.di.splashModule
import com.br.androideventscheduler.feature.upsert.di.upsertModule
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
        // Detalhes (spec 10)
        commonModule,
        detailModule,
        // Alarmes (spec 04)
        alarmModule,
        deepLinkModule,
        // Pesquisa (spec 13)
        searchModule,
        // Home (spec 05)
        homeModule,
        // Criar/editar (spec 07)
        upsertModule,
        // Tela cheia (spec 14)
        alarmFeatureModule,
        // Importar/exportar (spec 17)
        backupModule,
        incomingFileModule,

        // Bloqueio do app (spec 16)
        securityModule,
    )
