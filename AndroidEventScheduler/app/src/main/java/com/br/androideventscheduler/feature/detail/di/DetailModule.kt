package com.br.androideventscheduler.feature.detail.di

import com.br.androideventscheduler.core.di.DispatcherNames
import com.br.androideventscheduler.feature.detail.domain.usecase.DeleteScheduleUseCase
import com.br.androideventscheduler.feature.detail.domain.usecase.DuplicateScheduleUseCase
import com.br.androideventscheduler.feature.detail.presentation.mapper.ScheduleDetailMapper
import com.br.androideventscheduler.feature.detail.presentation.viewmodel.ScheduleDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** API pública da feature: módulo Koin agregado em `app.AppModules`. */
val detailModule =
    module {
        factory { ScheduleDetailMapper(strings = get()) }
        // Duplicar/excluir (spec 11)
        factory { DuplicateScheduleUseCase(scheduleRepository = get(), scheduleNextAlarm = get()) }
        factory { DeleteScheduleUseCase(scheduleRepository = get(), alarmScheduler = get()) }
        // O id do agendamento chega da rota via `parametersOf(scheduleId)`.
        viewModel { parameters ->
            ScheduleDetailViewModel(
                scheduleId = parameters.get(),
                scheduleRepository = get(),
                userSettingsRepository = get(),
                duplicateSchedule = get(),
                deleteSchedule = get(),
                strings = get(),
                savedStateHandle = get(),
                mapper = get(),
                dispatcher = get(named(DispatcherNames.MAIN)),
            )
        }
    }
