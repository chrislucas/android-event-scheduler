package com.br.androideventscheduler.feature.splash.di

import com.br.androideventscheduler.core.di.DispatcherNames
import com.br.androideventscheduler.feature.splash.presentation.viewmodel.SplashViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** API pública da feature: módulo Koin agregado em `app.AppModules`. */
val splashModule =
    module {
        viewModel { SplashViewModel(dispatcher = get(named(DispatcherNames.DEFAULT))) }
    }
