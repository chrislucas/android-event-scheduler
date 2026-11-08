package com.br.androideventscheduler.core.alarm.di

import com.br.androideventscheduler.core.alarm.data.AndroidAlarmScheduler
import com.br.androideventscheduler.core.alarm.domain.scheduler.AlarmScheduler
import com.br.androideventscheduler.core.alarm.domain.usecase.ScheduleNextAlarmUseCase
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import java.time.Clock

@RunWith(RobolectricTestRunner::class)
class AlarmModuleRobolectricTest {
    private val koin = GlobalContext.get()

    @Test
    fun `given the production graph when resolving the alarm scheduler then it is the android implementation`() {
        assertTrue(koin.get<AlarmScheduler>() is AndroidAlarmScheduler)
    }

    @Test
    fun `given the production graph when resolving the use case then it is a shared singleton`() {
        assertSame(koin.get<ScheduleNextAlarmUseCase>(), koin.get<ScheduleNextAlarmUseCase>())
    }

    @Test
    fun `given the production graph when resolving the clock then it follows the device time zone`() {
        assertNotNull(koin.get<Clock>())
        assertTrue(koin.get<Clock>().zone == Clock.systemDefaultZone().zone)
    }
}
