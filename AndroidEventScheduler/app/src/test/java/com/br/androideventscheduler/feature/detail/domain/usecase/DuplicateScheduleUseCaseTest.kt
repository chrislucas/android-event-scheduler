package com.br.androideventscheduler.feature.detail.domain.usecase

import com.br.androideventscheduler.core.alarm.domain.usecase.ScheduleNextAlarmUseCase
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleNotFoundException
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class DuplicateScheduleUseCaseTest {
    private val repository = mockk<ScheduleRepository>(relaxUnitFun = true)
    private val scheduleNextAlarm = mockk<ScheduleNextAlarmUseCase>(relaxed = true)
    private val useCase =
        DuplicateScheduleUseCase(
            scheduleRepository = repository,
            scheduleNextAlarm = scheduleNextAlarm,
            idProvider = { COPY_ID },
        )

    private val original =
        ScheduleItem(
            id = "original-id",
            title = "Aula",
            type = ScheduleType.EVENT,
            startDate = LocalDate.of(2026, 11, 5),
            endDate = LocalDate.of(2026, 11, 6),
            startTime = LocalTime.of(22, 0),
            endTime = LocalTime.of(23, 30),
            isAllDay = false,
            location = "Sala 3",
            notes = "Levar o notebook",
            colorHex = 0xFF5C6BC0,
            calendarSource = CalendarSource.PERSONAL_ACCOUNT,
            reminderOffset = ReminderOffset.DEFAULT,
            recurrencePattern = RecurrencePattern.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), occurrenceLimit = 8),
            stickerEmoji = "📚",
        )

    @Test
    fun `given existing item when duplicating then the copy has a new id and the suffixed title`() =
        runTest {
            coEvery { repository.getById("original-id") } returns original

            val copy = useCase("original-id", " (Cópia)").getOrThrow()

            assertEquals(COPY_ID, copy.id)
            assertNotEquals(original.id, copy.id)
            assertEquals("Aula (Cópia)", copy.title)
        }

    @Test
    fun `given recurring item when duplicating then every other field including recurrence and limit is preserved`() =
        runTest {
            coEvery { repository.getById("original-id") } returns original

            val copy = useCase("original-id", " (Cópia)").getOrThrow()

            assertEquals(original.copy(id = COPY_ID, title = "Aula (Cópia)"), copy)
        }

    @Test
    fun `given existing item when duplicating then it saves the copy and schedules its alarm`() =
        runTest {
            coEvery { repository.getById("original-id") } returns original

            val copy = useCase("original-id", " (Cópia)").getOrThrow()

            coVerify(exactly = 1) { repository.upsert(copy) }
            verify(exactly = 1) { scheduleNextAlarm(copy) }
        }

    @Test
    fun `given missing item when duplicating then it fails with not found and nothing is saved`() =
        runTest {
            coEvery { repository.getById("missing") } returns null

            val result = useCase("missing", " (Cópia)")

            assertTrue(result.exceptionOrNull() is ScheduleNotFoundException)
            coVerify(exactly = 0) { repository.upsert(any()) }
            verify(exactly = 0) { scheduleNextAlarm(any()) }
        }

    @Test
    fun `given save failure when duplicating then it fails and no alarm is scheduled`() =
        runTest {
            coEvery { repository.getById("original-id") } returns original
            coEvery { repository.upsert(any()) } throws IOException("disk full")

            val result = useCase("original-id", " (Cópia)")

            assertTrue(result.exceptionOrNull() is IOException)
            verify(exactly = 0) { scheduleNextAlarm(any()) }
        }

    @Test
    fun `given alarm failure when duplicating then it still succeeds and the copy stays saved`() =
        runTest {
            coEvery { repository.getById("original-id") } returns original
            every { scheduleNextAlarm(any()) } throws SecurityException("no exact alarm")

            val result = useCase("original-id", " (Cópia)")

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.upsert(result.getOrThrow()) }
        }

    private companion object {
        const val COPY_ID = "copy-id"
    }
}
