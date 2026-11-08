package com.br.androideventscheduler.core.alarm.di

import android.app.AlarmManager
import android.content.Context
import com.br.androideventscheduler.core.alarm.data.AndroidAlarmScheduler
import com.br.androideventscheduler.core.alarm.data.notification.ScheduleNotifier
import com.br.androideventscheduler.core.alarm.domain.scheduler.AlarmScheduler
import com.br.androideventscheduler.core.alarm.domain.usecase.ScheduleNextAlarmUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.time.Clock

val alarmModule =
    module {
        single<Clock> { Clock.systemDefaultZone() }
        single<AlarmScheduler> {
            AndroidAlarmScheduler(
                context = androidContext(),
                alarmManager = androidContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager,
            )
        }
        single { ScheduleNotifier(context = androidContext()) }
        single { ScheduleNextAlarmUseCase(recurrenceEngine = get(), alarmScheduler = get(), clock = get()) }
    }
