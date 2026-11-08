package com.br.androideventscheduler.core.alarm.domain.usecase

import com.br.androideventscheduler.core.alarm.domain.model.AlarmRequest
import com.br.androideventscheduler.core.alarm.domain.model.RescheduleSummary
import com.br.androideventscheduler.core.alarm.domain.scheduler.AlarmScheduler
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.recurrence.RecurrenceEngine
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.FakeClock
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class RescheduleAllAlarmsUseCaseTest {
    private val zoneId = ZoneId.of("America/Sao_Paulo")

    // 2026-11-07 (sábado) 12:00 em São Paulo.
    private val clock = FakeClock(Instant.parse("2026-11-07T15:00:00Z"), zoneId)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private val scheduleRepository = mockk<ScheduleRepository>()
    private val useCase =
        RescheduleAllAlarmsUseCase(
            scheduleRepository = scheduleRepository,
            scheduleNextAlarm = ScheduleNextAlarmUseCase(RecurrenceEngine(), alarmScheduler, clock),
        )

    private fun givenStoredItems(vararg items: ScheduleItem) {
        coEvery { scheduleRepository.getAll() } returns items.toList()
    }

    private fun item(
        id: String,
        startDate: LocalDate,
        startTime: LocalTime = LocalTime.of(10, 0),
        offset: ReminderOffset = ReminderOffset.FIFTEEN_MINUTES,
        pattern: RecurrencePattern = RecurrencePattern.None,
    ): ScheduleItem =
        scheduleItem(id = id, startDate = startDate, startTime = startTime, recurrencePattern = pattern)
            .copy(reminderOffset = offset)

    private fun millisOf(
        date: LocalDate,
        time: LocalTime,
    ): Long =
        LocalDateTime
            .of(date, time)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

    private fun scheduledRequests(): List<AlarmRequest> {
        val requests = mutableListOf<AlarmRequest>()
        verify { alarmScheduler.schedule(capture(requests)) }
        return requests
    }

    @Test
    fun `given a single future item when rescheduling then it is scheduled at start minus offset`() =
        runTest {
            givenStoredItems(item("future", startDate = LocalDate.of(2026, 11, 8)))

            val summary = useCase().getOrThrow()

            assertEquals(RescheduleSummary(scheduled = 1, skipped = 0, failed = 0), summary)
            val request = scheduledRequests().single()
            assertEquals("future", request.scheduleId)
            assertEquals(millisOf(LocalDate.of(2026, 11, 8), LocalTime.of(9, 45)), request.triggerAtEpochMillis)
        }

    @Test
    fun `given a single past item when rescheduling then it is skipped and its residual alarm is cancelled`() =
        runTest {
            givenStoredItems(item("past", startDate = LocalDate.of(2026, 11, 5)))

            val summary = useCase().getOrThrow()

            assertEquals(RescheduleSummary(scheduled = 0, skipped = 1, failed = 0), summary)
            verify(exactly = 1) { alarmScheduler.cancel("past") }
            verify(exactly = 0) { alarmScheduler.schedule(any()) }
        }

    @Test
    fun `given a daily item missed while the device was off when rescheduling then it triggers on the next future day`() =
        runTest {
            givenStoredItems(item("daily", startDate = LocalDate.of(2026, 11, 1), pattern = RecurrencePattern.Daily()))

            useCase().getOrThrow()

            val request = scheduledRequests().single()
            assertEquals(millisOf(LocalDate.of(2026, 11, 8), LocalTime.of(9, 45)), request.triggerAtEpochMillis)
        }

    @Test
    fun `given a weekly tuesday and thursday item when rescheduling on saturday then it triggers next tuesday`() =
        runTest {
            val pattern = RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY))
            givenStoredItems(item("weekly", startDate = LocalDate.of(2026, 11, 3), pattern = pattern))

            useCase().getOrThrow()

            val request = scheduledRequests().single()
            assertEquals(millisOf(LocalDate.of(2026, 11, 10), LocalTime.of(9, 45)), request.triggerAtEpochMillis)
        }

    @Test
    fun `given a finished series when rescheduling then it is skipped and its residual alarm is cancelled`() =
        runTest {
            val finished = item("finished", startDate = LocalDate.of(2026, 11, 1), pattern = RecurrencePattern.Daily(occurrenceLimit = 3))
            givenStoredItems(finished)

            val summary = useCase().getOrThrow()

            assertEquals(RescheduleSummary(scheduled = 0, skipped = 1, failed = 0), summary)
            verify(exactly = 1) { alarmScheduler.cancel("finished") }
        }

    @Test
    fun `given a reminder offset none when rescheduling then it is skipped without a new alarm`() =
        runTest {
            givenStoredItems(item("silent", startDate = LocalDate.of(2026, 11, 8), offset = ReminderOffset.NONE))

            val summary = useCase().getOrThrow()

            assertEquals(RescheduleSummary(scheduled = 0, skipped = 1, failed = 0), summary)
            verify(exactly = 1) { alarmScheduler.cancel("silent") }
            verify(exactly = 0) { alarmScheduler.schedule(any()) }
        }

    @Test
    fun `given one item failing when rescheduling then the others are still scheduled and the failure is counted`() =
        runTest {
            every { alarmScheduler.schedule(match { it.scheduleId == "broken" }) } throws SecurityException("boom")
            givenStoredItems(
                item("first", startDate = LocalDate.of(2026, 11, 8)),
                item("broken", startDate = LocalDate.of(2026, 11, 8)),
                item("last", startDate = LocalDate.of(2026, 11, 9)),
                item("past", startDate = LocalDate.of(2026, 11, 5)),
            )

            val summary = useCase().getOrThrow()

            assertEquals(RescheduleSummary(scheduled = 2, skipped = 1, failed = 1), summary)
            assertEquals(listOf("first", "broken", "last"), scheduledRequests().map { it.scheduleId })
        }

    @Test
    fun `given an empty database when rescheduling then it completes with zero counts`() =
        runTest {
            givenStoredItems()

            val summary = useCase().getOrThrow()

            assertEquals(RescheduleSummary(scheduled = 0, skipped = 0, failed = 0), summary)
            verify(exactly = 0) { alarmScheduler.schedule(any()) }
            verify(exactly = 0) { alarmScheduler.cancel(any()) }
        }

    @Test
    fun `given the same items when rescheduling twice then the same alarm requests are produced`() =
        runTest {
            givenStoredItems(
                item("daily", startDate = LocalDate.of(2026, 11, 1), pattern = RecurrencePattern.Daily()),
                item("future", startDate = LocalDate.of(2026, 11, 8)),
            )

            val firstSummary = useCase().getOrThrow()
            val firstRequests = scheduledRequests().toList()
            val secondSummary = useCase().getOrThrow()
            val allRequests = scheduledRequests()

            assertEquals(firstSummary, secondSummary)
            assertEquals(firstRequests + firstRequests, allRequests)
        }

    @Test
    fun `given reading the items fails when rescheduling then the result is a failure`() =
        runTest {
            val failure = IllegalStateException("database unavailable")
            coEvery { scheduleRepository.getAll() } throws failure

            val result = useCase()

            assertTrue(result.isFailure)
            assertEquals(failure, result.exceptionOrNull())
            verify(exactly = 0) { alarmScheduler.schedule(any()) }
        }
}
