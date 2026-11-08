package com.br.androideventscheduler.core.alarm.domain.usecase

import com.br.androideventscheduler.core.alarm.domain.model.AlarmRequest
import com.br.androideventscheduler.core.alarm.domain.model.AlertPresentationMode
import com.br.androideventscheduler.core.alarm.domain.scheduler.AlarmScheduler
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.domain.recurrence.RecurrenceEngine
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.FakeClock
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ScheduleNextAlarmUseCaseTest {
    private val zoneId = ZoneId.of("America/Sao_Paulo")

    // 2026-11-07 (sábado) 12:00 em São Paulo.
    private val clock = FakeClock(Instant.parse("2026-11-07T15:00:00Z"), zoneId)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private val useCase = ScheduleNextAlarmUseCase(RecurrenceEngine(), alarmScheduler, clock)

    private fun millisOf(
        date: LocalDate,
        time: LocalTime,
    ): Long =
        LocalDateTime
            .of(date, time)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

    private fun item(
        startDate: LocalDate,
        startTime: LocalTime = LocalTime.of(10, 0),
        offset: ReminderOffset = ReminderOffset.FIFTEEN_MINUTES,
        pattern: RecurrencePattern = RecurrencePattern.None,
        type: ScheduleType = ScheduleType.EVENT,
        isAllDay: Boolean = false,
    ): ScheduleItem =
        scheduleItem(startDate = startDate, startTime = startTime, recurrencePattern = pattern, type = type, isAllDay = isAllDay)
            .copy(reminderOffset = offset)

    private fun capturedRequest(): AlarmRequest {
        val requestSlot = slot<AlarmRequest>()
        verify(exactly = 1) { alarmScheduler.schedule(capture(requestSlot)) }
        return requestSlot.captured
    }

    @Test
    fun `given single future item when scheduling then triggers at start minus offset`() {
        val result = useCase(item(startDate = LocalDate.of(2026, 11, 8)))

        val expected = millisOf(LocalDate.of(2026, 11, 8), LocalTime.of(9, 45))
        assertEquals(expected, result)
        assertEquals(expected, capturedRequest().triggerAtEpochMillis)
        assertEquals("item-1", capturedRequest().scheduleId)
    }

    @Test
    fun `given single past item when scheduling then cancels and returns null`() {
        val result = useCase(item(startDate = LocalDate.of(2026, 11, 5)))

        assertNull(result)
        verify { alarmScheduler.cancel("item-1") }
        verify(exactly = 0) { alarmScheduler.schedule(any()) }
    }

    @Test
    fun `given reminder offset none when scheduling then cancels and returns null`() {
        val result = useCase(item(startDate = LocalDate.of(2026, 11, 8), offset = ReminderOffset.NONE))

        assertNull(result)
        verify { alarmScheduler.cancel("item-1") }
        verify(exactly = 0) { alarmScheduler.schedule(any()) }
    }

    @Test
    fun `given trigger exactly now when scheduling then it is not strictly in the future and is skipped`() {
        // Início 12:15 − 15 min = 12:00 = agora.
        val result = useCase(item(startDate = LocalDate.of(2026, 11, 7), startTime = LocalTime.of(12, 15)))

        assertNull(result)
    }

    @Test
    fun `given daily item whose time passed today when scheduling then triggers tomorrow`() {
        val result = useCase(item(startDate = LocalDate.of(2026, 11, 1), pattern = RecurrencePattern.Daily()))

        assertEquals(millisOf(LocalDate.of(2026, 11, 8), LocalTime.of(9, 45)), result)
    }

    @Test
    fun `given daily item whose time is still ahead today when scheduling then triggers today`() {
        val result =
            useCase(
                item(startDate = LocalDate.of(2026, 11, 1), startTime = LocalTime.of(18, 0), pattern = RecurrencePattern.Daily()),
            )

        assertEquals(millisOf(LocalDate.of(2026, 11, 7), LocalTime.of(17, 45)), result)
    }

    @Test
    fun `given weekly tuesday and thursday when scheduling on saturday then triggers next tuesday`() {
        val pattern = RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY))

        val result = useCase(item(startDate = LocalDate.of(2026, 11, 3), pattern = pattern))

        // 2026-11-10 é terça-feira.
        assertEquals(millisOf(LocalDate.of(2026, 11, 10), LocalTime.of(9, 45)), result)
    }

    @Test
    fun `given series with exhausted occurrence limit when scheduling then cancels and returns null`() {
        val result = useCase(item(startDate = LocalDate.of(2026, 11, 1), pattern = RecurrencePattern.Daily(occurrenceLimit = 3)))

        assertNull(result)
        verify { alarmScheduler.cancel("item-1") }
        verify(exactly = 0) { alarmScheduler.schedule(any()) }
    }

    @Test
    fun `given series with remaining occurrences when scheduling then triggers on the next remaining one`() {
        // Ocorrências: 5, 6, 7, 8, 9 de novembro (limite 5); hoje (7) às 10:00 já passou? Agora é 12:00 → próxima é dia 8.
        val result = useCase(item(startDate = LocalDate.of(2026, 11, 5), pattern = RecurrencePattern.Daily(occurrenceLimit = 5)))

        assertEquals(millisOf(LocalDate.of(2026, 11, 8), LocalTime.of(9, 45)), result)
    }

    @Test
    fun `given all day item when scheduling then occurrence starts at nine local time`() {
        val result =
            useCase(item(startDate = LocalDate.of(2026, 11, 8), startTime = LocalTime.MIDNIGHT, isAllDay = true))

        assertEquals(millisOf(LocalDate.of(2026, 11, 8), ScheduleNextAlarmUseCase.ALL_DAY_ALERT_TIME.minusMinutes(15)), result)
    }

    @Test
    fun `given early morning event and one hour offset when scheduling then trigger crosses midnight to the previous day`() {
        val result =
            useCase(
                item(startDate = LocalDate.of(2026, 11, 8), startTime = LocalTime.of(0, 30), offset = ReminderOffset.ONE_HOUR),
            )

        assertEquals(millisOf(LocalDate.of(2026, 11, 7), LocalTime.of(23, 30)), result)
    }

    @Test
    fun `given event when scheduling then mode is full screen`() {
        useCase(item(startDate = LocalDate.of(2026, 11, 8), type = ScheduleType.EVENT))

        assertEquals(AlertPresentationMode.FULL_SCREEN_EVENT, capturedRequest().presentationMode)
    }

    @Test
    fun `given reminder when scheduling then mode is notification only`() {
        useCase(item(startDate = LocalDate.of(2026, 11, 8), type = ScheduleType.REMINDER))

        assertEquals(AlertPresentationMode.NOTIFICATION_ONLY, capturedRequest().presentationMode)
    }

    @Test
    fun `given each schedule type when asking the presentation mode then it maps event and reminder`() {
        assertEquals(AlertPresentationMode.FULL_SCREEN_EVENT, AlertPresentationMode.forType(ScheduleType.EVENT))
        assertEquals(AlertPresentationMode.NOTIFICATION_ONLY, AlertPresentationMode.forType(ScheduleType.REMINDER))
    }

    @Test
    fun `given a different clock zone when scheduling then trigger uses the injected zone`() {
        val tokyoClock = FakeClock(Instant.parse("2026-11-07T15:00:00Z"), ZoneId.of("Asia/Tokyo"))
        val tokyoUseCase = ScheduleNextAlarmUseCase(RecurrenceEngine(), alarmScheduler, tokyoClock)

        // Em Tóquio já é 08/11 00:00; um item em 08/11 às 10:00 dispara 09:45 locais de Tóquio.
        val result = tokyoUseCase(item(startDate = LocalDate.of(2026, 11, 8)))

        val expected =
            LocalDateTime
                .of(2026, 11, 8, 9, 45)
                .atZone(ZoneId.of("Asia/Tokyo"))
                .toInstant()
                .toEpochMilli()
        assertEquals(expected, result)
    }
}
