package com.br.androideventscheduler.feature.alarm.di

import com.br.androideventscheduler.core.di.DispatcherNames
import com.br.androideventscheduler.feature.alarm.presentation.mapper.FullScreenAlarmMapper
import com.br.androideventscheduler.feature.alarm.presentation.viewmodel.FullScreenAlarmViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** API pública da feature: módulo Koin agregado em `app.AppModules` (a tela é aberta pelo sistema via manifest). */
val alarmFeatureModule =
    module {
        factory { FullScreenAlarmMapper(strings = get(), recurrenceEngine = get(), clock = get()) }
        // O id do agendamento chega da Activity via `parametersOf(scheduleId)`.
        viewModel { parameters ->
            FullScreenAlarmViewModel(
                scheduleId = parameters.get(),
                scheduleRepository = get(),
                userSettingsRepository = get(),
                snoozeAlarmUseCase = get(),
                mapper = get(),
                dispatcher = get(named(DispatcherNames.MAIN)),
            )
        }
    }
