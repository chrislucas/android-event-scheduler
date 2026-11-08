package com.br.androideventscheduler.core.alarm.domain.usecase

import com.br.androideventscheduler.core.alarm.domain.model.AlarmRequest
import com.br.androideventscheduler.core.alarm.domain.model.AlertPresentationMode
import com.br.androideventscheduler.core.alarm.domain.scheduler.AlarmScheduler
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleNotFoundException
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.FakeClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.ZoneId

class SnoozeAlarmUseCaseTest {
    private val clock = FakeClock(Instant.parse("2026-11-07T15:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val scheduleRepository = mockk<ScheduleRepository>(relaxed = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private val useCase = SnoozeAlarmUseCase(scheduleRepository, alarmScheduler, clock)

    private fun capturedRequest(): AlarmRequest {
        val requestSlot = slot<AlarmRequest>()
        verify(exactly = 1) { alarmScheduler.schedule(capture(requestSlot)) }
        return requestSlot.captured
    }

    @Test
    fun `given existing item when snoozing for ten minutes then schedules now plus ten minutes`() =
        runTest {
            coEvery { scheduleRepository.getById("item-1") } returns scheduleItem()

            val result = useCase("item-1", snoozeMinutes = 10)

            val expectedTriggerMillis = clock.millis() + TEN_MINUTES_MILLIS
            assertEquals(expectedTriggerMillis, result.getOrThrow())
            assertEquals(expectedTriggerMillis, capturedRequest().triggerAtEpochMillis)
            assertEquals("item-1", capturedRequest().scheduleId)
        }

    @Test
    fun `given each allowed duration when snoozing then the trigger is shifted by exactly that many minutes`() =
        runTest {
            coEvery { scheduleRepository.getById("item-1") } returns scheduleItem()

            listOf(5, 10, 15).forEach { minutes ->
                val result = useCase("item-1", snoozeMinutes = minutes)

                assertEquals(clock.millis() + minutes * 60_000L, result.getOrThrow())
            }
        }

    @Test
    fun `given an event when snoozing then the mode is full screen`() =
        runTest {
            coEvery { scheduleRepository.getById("item-1") } returns scheduleItem(type = ScheduleType.EVENT)

            useCase("item-1", snoozeMinutes = 5)

            assertEquals(AlertPresentationMode.FULL_SCREEN_EVENT, capturedRequest().presentationMode)
        }

    @Test
    fun `given a reminder when snoozing then the mode is notification only`() =
        runTest {
            coEvery { scheduleRepository.getById("item-1") } returns scheduleItem(type = ScheduleType.REMINDER)

            useCase("item-1", snoozeMinutes = 5)

            assertEquals(AlertPresentationMode.NOTIFICATION_ONLY, capturedRequest().presentationMode)
        }

    @Test
    fun `given zero or negative minutes when snoozing then fails with validation error and schedules nothing`() =
        runTest {
            coEvery { scheduleRepository.getById("item-1") } returns scheduleItem()

            listOf(0, -5).forEach { minutes ->
                val failure = useCase("item-1", snoozeMinutes = minutes).exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
            }
            verify(exactly = 0) { alarmScheduler.schedule(any()) }
            coVerify(exactly = 0) { scheduleRepository.getById(any()) }
        }

    @Test
    fun `given missing item when snoozing then fails with ScheduleNotFoundException and schedules nothing`() =
        runTest {
            coEvery { scheduleRepository.getById("gone") } returns null

            val failure = useCase("gone", snoozeMinutes = 10).exceptionOrNull()

            assertEquals("gone", (failure as ScheduleNotFoundException).scheduleId)
            verify(exactly = 0) { alarmScheduler.schedule(any()) }
        }

    @Test
    fun `given existing item when snoozing then the item is not modified`() =
        runTest {
            coEvery { scheduleRepository.getById("item-1") } returns scheduleItem()

            useCase("item-1", snoozeMinutes = 10)

            coVerify(exactly = 0) { scheduleRepository.upsert(any()) }
            coVerify(exactly = 0) { scheduleRepository.delete(any()) }
        }

    @Test
    fun `given scheduler failure when snoozing then returns failure`() =
        runTest {
            coEvery { scheduleRepository.getById("item-1") } returns scheduleItem()
            every { alarmScheduler.schedule(any()) } throws SecurityException("exact alarm denied")

            val result = useCase("item-1", snoozeMinutes = 10)

            assertTrue(result.exceptionOrNull() is SecurityException)
        }

    @Test
    fun `given repository failure when snoozing then returns failure without scheduling`() =
        runTest {
            coEvery { scheduleRepository.getById("item-1") } throws IOException("disk")

            val result = useCase("item-1", snoozeMinutes = 10)

            assertTrue(result.exceptionOrNull() is IOException)
            verify(exactly = 0) { alarmScheduler.schedule(any()) }
        }

    private companion object {
        const val TEN_MINUTES_MILLIS = 10 * 60_000L
    }
}
