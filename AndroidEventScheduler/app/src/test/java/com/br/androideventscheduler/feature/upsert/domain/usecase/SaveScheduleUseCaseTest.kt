package com.br.androideventscheduler.feature.upsert.domain.usecase

import com.br.androideventscheduler.core.alarm.domain.usecase.ScheduleNextAlarmUseCase
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.feature.upsert.domain.model.SaveScheduleError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime

class SaveScheduleUseCaseTest {
    private val scheduleRepository = mockk<ScheduleRepository>(relaxed = true)
    private val scheduleNextAlarm = mockk<ScheduleNextAlarmUseCase>(relaxed = true)
    private val useCase = SaveScheduleUseCase(scheduleRepository, scheduleNextAlarm)

    @Test
    fun `given blank title when saving then fails with BlankTitle and persists nothing`() =
        runTest {
            val result = useCase(scheduleItem(title = "   "))

            assertSame(SaveScheduleError.BlankTitle, result.exceptionOrNull())
            coVerify(exactly = 0) { scheduleRepository.upsert(any()) }
            verify(exactly = 0) { scheduleNextAlarm(any()) }
        }

    @Test
    fun `given end time before start time on the same day when saving then fails with EndBeforeStart`() =
        runTest {
            val item = scheduleItem(startTime = LocalTime.of(10, 0), endTime = LocalTime.of(9, 59))

            val result = useCase(item)

            assertSame(SaveScheduleError.EndBeforeStart, result.exceptionOrNull())
            coVerify(exactly = 0) { scheduleRepository.upsert(any()) }
            verify(exactly = 0) { scheduleNextAlarm(any()) }
        }

    @Test
    fun `given end time equal to start time when saving then it succeeds`() =
        runTest {
            val item = scheduleItem(startTime = LocalTime.of(10, 0), endTime = LocalTime.of(10, 0))

            assertTrue(useCase(item).isSuccess)
        }

    @Test
    fun `given end on a later day with earlier clock time when saving then it succeeds`() =
        runTest {
            val item =
                scheduleItem(
                    startDate = LocalDate.of(2026, 11, 5),
                    endDate = LocalDate.of(2026, 11, 6),
                    startTime = LocalTime.of(22, 0),
                    endTime = LocalTime.of(1, 0),
                )

            assertTrue(useCase(item).isSuccess)
        }

    @Test
    fun `given all day item whose end time is earlier than start time when saving then only dates are compared`() =
        runTest {
            val item = scheduleItem(isAllDay = true, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(8, 0))

            assertTrue(useCase(item).isSuccess)
        }

    @Test
    fun `given reminder without end time when saving then it succeeds`() =
        runTest {
            val item = scheduleItem(type = ScheduleType.REMINDER, endTime = null)

            assertTrue(useCase(item).isSuccess)
        }

    @Test
    fun `given valid item when saving then persists it and schedules the alarm`() =
        runTest {
            val item = scheduleItem(title = "Consulta")

            val result = useCase(item)

            assertEquals(item, result.getOrNull())
            coVerify(exactly = 1) { scheduleRepository.upsert(item) }
            verify(exactly = 1) { scheduleNextAlarm(item) }
        }

    @Test
    fun `given title with surrounding spaces when saving then persists the trimmed title`() =
        runTest {
            val result = useCase(scheduleItem(title = "  Consulta  "))

            assertEquals("Consulta", result.getOrNull()?.title)
            coVerify(exactly = 1) { scheduleRepository.upsert(match { it.title == "Consulta" }) }
            verify(exactly = 1) { scheduleNextAlarm(match { it.title == "Consulta" }) }
        }

    @Test
    fun `given alarm scheduling fails when saving then the item is still saved and the result is success`() =
        runTest {
            every { scheduleNextAlarm(any()) } throws SecurityException("no exact alarm")
            val item = scheduleItem()

            val result = useCase(item)

            assertEquals(item, result.getOrNull())
            coVerify(exactly = 1) { scheduleRepository.upsert(item) }
        }

    @Test
    fun `given repository failure when saving then fails with that error and does not schedule the alarm`() =
        runTest {
            val failure = IOException("disk full")
            coEvery { scheduleRepository.upsert(any()) } throws failure

            val result = useCase(scheduleItem())

            assertSame(failure, result.exceptionOrNull())
            verify(exactly = 0) { scheduleNextAlarm(any()) }
        }
}
