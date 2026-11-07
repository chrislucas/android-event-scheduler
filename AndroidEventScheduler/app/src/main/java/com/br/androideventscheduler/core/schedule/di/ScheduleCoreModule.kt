package com.br.androideventscheduler.core.schedule.di

import com.br.androideventscheduler.core.di.DispatcherNames
import com.br.androideventscheduler.core.schedule.data.repository.ScheduleRepositoryImpl
import com.br.androideventscheduler.core.schedule.domain.recurrence.RecurrenceEngine
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val scheduleCoreModule =
    module {
        single { RecurrenceEngine() }
        single<ScheduleRepository> {
            ScheduleRepositoryImpl(
                scheduleDao = get(),
                recurrenceEngine = get(),
                ioDispatcher = get(named(DispatcherNames.IO)),
            )
        }
    }
