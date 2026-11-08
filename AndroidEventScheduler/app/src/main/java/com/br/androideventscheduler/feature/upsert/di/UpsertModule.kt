package com.br.androideventscheduler.feature.upsert.di

import com.br.androideventscheduler.core.di.DispatcherNames
import com.br.androideventscheduler.core.navigation.UpsertSchedule
import com.br.androideventscheduler.feature.upsert.domain.usecase.SaveScheduleUseCase
import com.br.androideventscheduler.feature.upsert.presentation.viewmodel.UpsertScheduleViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** API pública da feature: módulo Koin agregado em `app.AppModules`. */
val upsertModule =
    module {
        factory { SaveScheduleUseCase(scheduleRepository = get(), scheduleNextAlarm = get()) }
        // A rota (scheduleId + data inicial) chega por `parametersOf(UpsertSchedule(...))`.
        viewModel { parameters ->
            val route: UpsertSchedule = parameters.get()
            UpsertScheduleViewModel(
                scheduleId = route.scheduleId,
                initialDateEpochDay = route.initialDateEpochDay,
                scheduleRepository = get(),
                userSettingsRepository = get(),
                saveScheduleUseCase = get(),
                clock = get(),
                dispatcher = get(named(DispatcherNames.MAIN)),
            )
        }
    }
