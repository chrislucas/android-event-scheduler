package com.br.androideventscheduler.feature.home.di

import com.br.androideventscheduler.core.di.DispatcherNames
import com.br.androideventscheduler.feature.home.data.repository.HomePreferencesRepositoryImpl
import com.br.androideventscheduler.feature.home.domain.calendar.MonthGridBuilder
import com.br.androideventscheduler.feature.home.domain.repository.HomePreferencesRepository
import com.br.androideventscheduler.feature.home.presentation.viewmodel.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** API pública da feature: módulo Koin agregado em `app.AppModules`. O `Clock` vem do `alarmModule`. */
val homeModule =
    module {
        factory { MonthGridBuilder() }
        single<HomePreferencesRepository> { HomePreferencesRepositoryImpl(dataStore = get()) }
        viewModel {
            HomeViewModel(
                scheduleRepository = get(),
                userSettingsRepository = get(),
                homePreferencesRepository = get(),
                monthGridBuilder = get(),
                clock = get(),
                savedStateHandle = get(),
                dispatcher = get(named(DispatcherNames.MAIN)),
            )
        }
    }
