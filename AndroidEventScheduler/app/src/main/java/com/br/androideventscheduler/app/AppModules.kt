package com.br.androideventscheduler.app

import com.br.androideventscheduler.core.di.dispatchersModule
import org.koin.core.module.Module

/** Lista única de módulos Koin: `core` primeiro, depois um módulo por feature (cada spec acrescenta o seu). */
val appModules: List<Module> =
    listOf(
        dispatchersModule,
    )
