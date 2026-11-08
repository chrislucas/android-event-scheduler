package com.br.androideventscheduler.core.alarm.di

import android.app.AlarmManager
import android.content.Context
import com.br.androideventscheduler.core.alarm.data.AndroidAlarmScheduler
import com.br.androideventscheduler.core.alarm.data.notification.ScheduleNotifier
import com.br.androideventscheduler.core.alarm.domain.scheduler.AlarmScheduler
import com.br.androideventscheduler.core.alarm.domain.usecase.RescheduleAllAlarmsUseCase
import com.br.androideventscheduler.core.alarm.domain.usecase.ScheduleNextAlarmUseCase
import com.br.androideventscheduler.core.alarm.domain.usecase.SnoozeAlarmUseCase
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
        // Tela cheia (spec 14)
        single { SnoozeAlarmUseCase(scheduleRepository = get(), alarmScheduler = get(), clock = get()) }

        // Pós-boot (spec 15)
        factory { RescheduleAllAlarmsUseCase(scheduleRepository = get(), scheduleNextAlarm = get()) }
    }
