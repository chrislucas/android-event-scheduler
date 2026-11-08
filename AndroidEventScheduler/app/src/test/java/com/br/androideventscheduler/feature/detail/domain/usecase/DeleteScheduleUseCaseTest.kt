package com.br.androideventscheduler.feature.detail.domain.usecase

import com.br.androideventscheduler.core.alarm.domain.scheduler.AlarmScheduler
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DeleteScheduleUseCaseTest {
    private val repository = mockk<ScheduleRepository>(relaxUnitFun = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxUnitFun = true)
    private val useCase = DeleteScheduleUseCase(scheduleRepository = repository, alarmScheduler = alarmScheduler)

    @Test
    fun `given existing item when deleting then the database is cleared before the alarm is cancelled`() =
        runTest {
            val result = useCase("item-1")

            assertTrue(result.isSuccess)
            coVerifyOrder {
                repository.delete("item-1")
                alarmScheduler.cancel("item-1")
            }
        }

    @Test
    fun `given database failure when deleting then it fails and the alarm is not cancelled`() =
        runTest {
            coEvery { repository.delete("item-1") } throws IOException("db locked")

            val result = useCase("item-1")

            assertTrue(result.exceptionOrNull() is IOException)
            verify(exactly = 0) { alarmScheduler.cancel(any()) }
        }

    @Test
    fun `given alarm cancel failure when deleting then it still succeeds with the item removed`() =
        runTest {
            every { alarmScheduler.cancel("item-1") } throws SecurityException("alarm service gone")

            val result = useCase("item-1")

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.delete("item-1") }
        }
}
