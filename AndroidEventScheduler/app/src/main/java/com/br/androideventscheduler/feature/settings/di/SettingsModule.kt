package com.br.androideventscheduler.feature.settings.di

import com.br.androideventscheduler.core.di.DispatcherNames
import com.br.androideventscheduler.feature.settings.presentation.viewmodel.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** API pública da feature: módulo Koin agregado em `app.AppModules`. */
val settingsModule =
    module {
        viewModel {
            SettingsViewModel(
                userSettingsRepository = get(),
                dispatcher = get(named(DispatcherNames.MAIN)),
            )
        }
    }
