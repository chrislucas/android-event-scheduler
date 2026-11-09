package com.br.androideventscheduler.feature.auth.di

import com.br.androideventscheduler.core.di.DispatcherNames
import com.br.androideventscheduler.feature.auth.presentation.viewmodel.LockViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** API pública da feature: módulo Koin agregado em `app.AppModules`. */
val authModule =
    module {
        viewModel {
            LockViewModel(
                appLockController = get(),
                dispatcher = get(named(DispatcherNames.MAIN)),
            )
        }
    }
