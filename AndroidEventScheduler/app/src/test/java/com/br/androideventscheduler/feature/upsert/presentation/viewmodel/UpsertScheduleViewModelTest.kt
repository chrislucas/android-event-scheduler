package com.br.androideventscheduler.feature.upsert.presentation.viewmodel

import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.testing.FakeUserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.EventColorOption
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.FakeClock
import com.br.androideventscheduler.core.testing.MainDispatcherRule
import com.br.androideventscheduler.feature.upsert.domain.model.SaveScheduleError
import com.br.androideventscheduler.feature.upsert.domain.usecase.SaveScheduleUseCase
import com.br.androideventscheduler.feature.upsert.presentation.effect.UpsertScheduleUiEffect
import com.br.androideventscheduler.feature.upsert.presentation.event.QuickTime
import com.br.androideventscheduler.feature.upsert.presentation.event.UpsertScheduleUiEvent
import com.br.androideventscheduler.feature.upsert.presentation.state.ActiveInlinePicker
import com.br.androideventscheduler.feature.upsert.presentation.state.UpsertScheduleUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class UpsertScheduleViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    // 2026-11-07 09:00 em São Paulo → próxima hora cheia: 10:00.
    private val clock = FakeClock()
    private val today = LocalDate.of(2026, 11, 7)
    private val scheduleRepository = mockk<ScheduleRepository>()
    private val saveScheduleUseCase = mockk<SaveScheduleUseCase>()
    private val settingsRepository =
        FakeUserSettingsRepository(
            UserSettings(activeAccountEmail = "ana@exemplo.com", defaultReminderOffset = ReminderOffset.THIRTY_MINUTES),
        )

    private fun createViewModel(
        scheduleId: String? = null,
        initialDateEpochDay: Long? = null,
    ) = UpsertScheduleViewModel(
        scheduleId = scheduleId,
        initialDateEpochDay = initialDateEpochDay,
        scheduleRepository = scheduleRepository,
        userSettingsRepository = settingsRepository,
        saveScheduleUseCase = saveScheduleUseCase,
        clock = clock,
        dispatcher = mainRule.dispatcher,
        idProvider = { "generated-id" },
    )

    private class EffectRecorder(
        val effects: MutableList<UpsertScheduleUiEffect>,
        val job: Job,
    )

    private fun TestScope.recordEffects(viewModel: UpsertScheduleViewModel): EffectRecorder {
        val effects = mutableListOf<UpsertScheduleUiEffect>()
        return EffectRecorder(effects, launch { viewModel.uiEffect.collect { effects += it } })
    }

    private fun UpsertScheduleViewModel.content() = uiState.value as UpsertScheduleUiState.Content

    private suspend fun TestScope.loadedCreation(initialDateEpochDay: Long? = null): UpsertScheduleViewModel {
        val viewModel = createViewModel(initialDateEpochDay = initialDateEpochDay)
        advanceUntilIdle()
        return viewModel
    }

    private fun stubSaveSuccess() {
        coEvery { saveScheduleUseCase(any()) } answers { Result.success(firstArg()) }
    }

    // region valores iniciais

    @Test
    fun `given creation when starting then it shows loading and then defaults from settings and clock`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            assertEquals(UpsertScheduleUiState.Loading, viewModel.uiState.value)
            advanceUntilIdle()

            val content = viewModel.content()
            assertEquals(ScheduleType.REMINDER, content.mode)
            assertEquals(today, content.startDate)
            assertEquals(LocalTime.of(10, 0), content.startTime)
            assertEquals(today, content.endDate)
            assertEquals(ReminderOffset.THIRTY_MINUTES, content.reminderOffset)
            assertEquals(RecurrencePattern.None, content.recurrencePattern)
            assertEquals(EventColorOption.LIGHT_BLUE.hex, content.colorHex)
            assertEquals(CalendarSource.MY_REMINDERS, content.calendarSource)
            assertEquals("ana@exemplo.com", content.accountEmail)
            assertFalse(content.isEditing)
        }

    @Test
    fun `given route date when creating then the start date is the route date and the time is the next full hour`() =
        runTest(mainRule.dispatcher) {
            val routeDate = LocalDate.of(2026, 12, 25)

            val viewModel = loadedCreation(initialDateEpochDay = routeDate.toEpochDay())

            assertEquals(routeDate, viewModel.content().startDate)
            assertEquals(LocalTime.of(10, 0), viewModel.content().startTime)
        }

    @Test
    fun `given clock late at night when creating without route date then the start is midnight of the next day`() =
        runTest(mainRule.dispatcher) {
            clock.setInstant(java.time.Instant.parse("2026-11-08T02:30:00Z")) // 23:30 em São Paulo, dia 7

            val viewModel = loadedCreation()

            assertEquals(LocalDate.of(2026, 11, 8), viewModel.content().startDate)
            assertEquals(LocalTime.MIDNIGHT, viewModel.content().startTime)
        }

    @Test
    fun `given existing event when editing then it fills every field from the item`() =
        runTest(mainRule.dispatcher) {
            val item =
                ScheduleItem(
                    id = "item-1",
                    title = "Workshop",
                    type = ScheduleType.EVENT,
                    startDate = LocalDate.of(2026, 11, 5),
                    endDate = LocalDate.of(2026, 11, 6),
                    startTime = LocalTime.of(22, 0),
                    endTime = LocalTime.of(1, 30),
                    isAllDay = false,
                    location = "Sala 3",
                    notes = "Levar notebook",
                    colorHex = EventColorOption.CORAL.hex,
                    calendarSource = CalendarSource.FAMILY,
                    reminderOffset = ReminderOffset.ONE_HOUR,
                    recurrencePattern = RecurrencePattern.Daily(occurrenceLimit = 3),
                    stickerEmoji = "🚀",
                )
            coEvery { scheduleRepository.getById("item-1") } returns item

            val viewModel = createViewModel(scheduleId = "item-1")
            advanceUntilIdle()

            val content = viewModel.content()
            assertTrue(content.isEditing)
            assertEquals("item-1", content.scheduleId)
            assertEquals(ScheduleType.EVENT, content.mode)
            assertEquals("Workshop", content.title)
            assertEquals(LocalDate.of(2026, 11, 6), content.endDate)
            assertEquals(LocalTime.of(1, 30), content.endTime)
            assertEquals("Sala 3", content.location)
            assertEquals("Levar notebook", content.notes)
            assertEquals(CalendarSource.FAMILY, content.calendarSource)
            assertEquals(ReminderOffset.ONE_HOUR, content.reminderOffset)
            assertEquals(RecurrencePattern.Daily(occurrenceLimit = 3), content.recurrencePattern)
            assertEquals(EventColorOption.CORAL.hex, content.colorHex)
            assertEquals("🚀", content.stickerEmoji)
        }

    @Test
    fun `given unknown schedule id when editing then it shows a message goes back and stays loading`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("missing") } returns null
            val viewModel = createViewModel(scheduleId = "missing")
            val recorder = recordEffects(viewModel)

            advanceUntilIdle()

            assertEquals(
                listOf(UpsertScheduleUiEffect.ShowMessage(R.string.upsert_not_found), UpsertScheduleUiEffect.NavigateBack),
                recorder.effects,
            )
            assertEquals(UpsertScheduleUiState.Loading, viewModel.uiState.value)
            recorder.job.cancel()
        }

    @Test
    fun `given repository failure when editing then it shows a message and goes back`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("item-1") } throws IOException("db closed")
            val viewModel = createViewModel(scheduleId = "item-1")
            val recorder = recordEffects(viewModel)

            advanceUntilIdle()

            assertEquals(
                listOf(UpsertScheduleUiEffect.ShowMessage(R.string.upsert_not_found), UpsertScheduleUiEffect.NavigateBack),
                recorder.effects,
            )
            recorder.job.cancel()
        }

    // endregion

    // region modos

    @Test
    fun `given reminder with title and time when switching to event then they are kept and the end is one hour later`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()
            viewModel.onEvent(UpsertScheduleUiEvent.OnTitleChanged("Consulta"))

            viewModel.onEvent(UpsertScheduleUiEvent.OnModeSelected(ScheduleType.EVENT))

            val content = viewModel.content()
            assertEquals(ScheduleType.EVENT, content.mode)
            assertEquals("Consulta", content.title)
            assertEquals(today, content.startDate)
            assertEquals(LocalTime.of(10, 0), content.startTime)
            assertEquals(LocalTime.of(11, 0), content.endTime)
            assertEquals(CalendarSource.PERSONAL_ACCOUNT, content.calendarSource)
        }

    @Test
    fun `given event with event only fields when switching to reminder then they are discarded and title is kept`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()
            viewModel.onEvent(UpsertScheduleUiEvent.OnModeSelected(ScheduleType.EVENT))
            viewModel.onEvent(UpsertScheduleUiEvent.OnTitleChanged("Festa"))
            viewModel.onEvent(UpsertScheduleUiEvent.OnAllDayToggled(true))
            viewModel.onEvent(UpsertScheduleUiEvent.OnLocationChanged("Casa"))
            viewModel.onEvent(UpsertScheduleUiEvent.OnCalendarSourceSelected(CalendarSource.FAMILY))

            viewModel.onEvent(UpsertScheduleUiEvent.OnModeSelected(ScheduleType.REMINDER))

            val content = viewModel.content()
            assertEquals(ScheduleType.REMINDER, content.mode)
            assertEquals("Festa", content.title)
            assertFalse(content.isAllDay)
            assertEquals("", content.location)
            assertEquals(CalendarSource.MY_REMINDERS, content.calendarSource)
            assertEquals(content.startDate, content.endDate)
            assertEquals(content.startTime, content.endTime)
        }

    @Test
    fun `given open end picker when switching to reminder then no picker stays open`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()
            viewModel.onEvent(UpsertScheduleUiEvent.OnModeSelected(ScheduleType.EVENT))
            viewModel.onEvent(UpsertScheduleUiEvent.OnPickerToggled(ActiveInlinePicker.END_DATE))

            viewModel.onEvent(UpsertScheduleUiEvent.OnModeSelected(ScheduleType.REMINDER))

            assertEquals(ActiveInlinePicker.NONE, viewModel.content().activePicker)
        }

    // endregion

    // region data e hora

    @Test
    fun `given event when moving the start date then the end keeps the one hour duration`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()
            viewModel.onEvent(UpsertScheduleUiEvent.OnModeSelected(ScheduleType.EVENT))

            viewModel.onEvent(UpsertScheduleUiEvent.OnDateSelected(ActiveInlinePicker.START_DATE, LocalDate.of(2026, 12, 1)))

            val content = viewModel.content()
            assertEquals(LocalDate.of(2026, 12, 1), content.startDate)
            assertEquals(LocalDate.of(2026, 12, 1), content.endDate)
            assertEquals(LocalTime.of(11, 0), content.endTime)
        }

    @Test
    fun `given event when choosing an end before the start then the end is clamped to the start`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()
            viewModel.onEvent(UpsertScheduleUiEvent.OnModeSelected(ScheduleType.EVENT))

            viewModel.onEvent(UpsertScheduleUiEvent.OnDateSelected(ActiveInlinePicker.END_DATE, LocalDate.of(2026, 11, 1)))

            val content = viewModel.content()
            assertEquals(content.startDate, content.endDate)
            assertEquals(content.startTime, content.endTime)
        }

    @Test
    fun `given event when changing the start time then the end time follows`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()
            viewModel.onEvent(UpsertScheduleUiEvent.OnModeSelected(ScheduleType.EVENT))

            viewModel.onEvent(UpsertScheduleUiEvent.OnTimeChanged(ActiveInlinePicker.START_TIME, LocalTime.of(14, 15)))

            assertEquals(LocalTime.of(14, 15), viewModel.content().startTime)
            assertEquals(LocalTime.of(15, 15), viewModel.content().endTime)
        }

    @Test
    fun `given event when choosing an end time before the start time on the same day then the end is clamped`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()
            viewModel.onEvent(UpsertScheduleUiEvent.OnModeSelected(ScheduleType.EVENT))

            viewModel.onEvent(UpsertScheduleUiEvent.OnTimeChanged(ActiveInlinePicker.END_TIME, LocalTime.of(8, 0)))

            assertEquals(LocalTime.of(10, 0), viewModel.content().endTime)
        }

    @Test
    fun `given reminder when tapping the quick date chips then the start date is today plus the offset`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()

            viewModel.onEvent(UpsertScheduleUiEvent.OnQuickDateChipClicked(daysOffset = 3))
            assertEquals(LocalDate.of(2026, 11, 10), viewModel.content().startDate)

            viewModel.onEvent(UpsertScheduleUiEvent.OnQuickDateChipClicked(daysOffset = 1))
            assertEquals(LocalDate.of(2026, 11, 8), viewModel.content().startDate)

            viewModel.onEvent(UpsertScheduleUiEvent.OnQuickDateChipClicked(daysOffset = 0))
            assertEquals(today, viewModel.content().startDate)
        }

    @Test
    fun `given quick time chip in one hour when tapped then start is now plus one hour`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()

            viewModel.onEvent(UpsertScheduleUiEvent.OnQuickTimeChipClicked(QuickTime.InOneHour))

            assertEquals(today, viewModel.content().startDate)
            assertEquals(LocalTime.of(10, 0), viewModel.content().startTime) // 09:00 + 1 h
        }

    @Test
    fun `given clock close to midnight when tapping in one hour then the date moves to the next day`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()
            clock.setInstant(java.time.Instant.parse("2026-11-08T02:30:00Z")) // 23:30 em São Paulo

            viewModel.onEvent(UpsertScheduleUiEvent.OnQuickTimeChipClicked(QuickTime.InOneHour))

            assertEquals(LocalDate.of(2026, 11, 8), viewModel.content().startDate)
            assertEquals(LocalTime.of(0, 30), viewModel.content().startTime)
        }

    @Test
    fun `given fixed quick time chip when tapped then only the start time changes`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()

            viewModel.onEvent(UpsertScheduleUiEvent.OnQuickTimeChipClicked(QuickTime.At(LocalTime.of(15, 0))))

            assertEquals(today, viewModel.content().startDate)
            assertEquals(LocalTime.of(15, 0), viewModel.content().startTime)
        }

    @Test
    fun `given one picker open when opening another then only the new one is active and tapping it again closes it`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()

            viewModel.onEvent(UpsertScheduleUiEvent.OnPickerToggled(ActiveInlinePicker.START_DATE))
            assertEquals(ActiveInlinePicker.START_DATE, viewModel.content().activePicker)

            viewModel.onEvent(UpsertScheduleUiEvent.OnPickerToggled(ActiveInlinePicker.START_TIME))
            assertEquals(ActiveInlinePicker.START_TIME, viewModel.content().activePicker)

            viewModel.onEvent(UpsertScheduleUiEvent.OnPickerToggled(ActiveInlinePicker.START_TIME))
            assertEquals(ActiveInlinePicker.NONE, viewModel.content().activePicker)
        }

    @Test
    fun `given open time picker when turning all day on then the picker closes`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()
            viewModel.onEvent(UpsertScheduleUiEvent.OnModeSelected(ScheduleType.EVENT))
            viewModel.onEvent(UpsertScheduleUiEvent.OnPickerToggled(ActiveInlinePicker.START_TIME))

            viewModel.onEvent(UpsertScheduleUiEvent.OnAllDayToggled(true))

            assertTrue(viewModel.content().isAllDay)
            assertEquals(ActiveInlinePicker.NONE, viewModel.content().activePicker)
        }

    // endregion

    // region salvar

    @Test
    fun `given valid reminder when saving then the use case receives a reminder item and the screen goes back`() =
        runTest(mainRule.dispatcher) {
            stubSaveSuccess()
            val viewModel = loadedCreation()
            val recorder = recordEffects(viewModel)
            viewModel.onEvent(UpsertScheduleUiEvent.OnTitleChanged("Pagar aluguel"))
            viewModel.onEvent(UpsertScheduleUiEvent.OnLocationChanged("ignorado"))

            viewModel.onEvent(UpsertScheduleUiEvent.OnSaveClicked)
            advanceUntilIdle()

            val savedItem = slot<ScheduleItem>()
            coVerify(exactly = 1) { saveScheduleUseCase(capture(savedItem)) }
            with(savedItem.captured) {
                assertEquals("generated-id", id)
                assertEquals("Pagar aluguel", title)
                assertEquals(ScheduleType.REMINDER, type)
                assertEquals(startDate, endDate)
                assertEquals(null, endTime)
                assertFalse(isAllDay)
                assertEquals("", location)
                assertEquals(CalendarSource.MY_REMINDERS, calendarSource)
                assertEquals(ReminderOffset.THIRTY_MINUTES, reminderOffset)
            }
            assertEquals(listOf<UpsertScheduleUiEffect>(UpsertScheduleUiEffect.NavigateBack), recorder.effects)
            recorder.job.cancel()
        }

    @Test
    fun `given all day event when saving then the item has no end time and keeps the dates`() =
        runTest(mainRule.dispatcher) {
            stubSaveSuccess()
            val viewModel = loadedCreation()
            viewModel.onEvent(UpsertScheduleUiEvent.OnModeSelected(ScheduleType.EVENT))
            viewModel.onEvent(UpsertScheduleUiEvent.OnTitleChanged("Viagem"))
            viewModel.onEvent(UpsertScheduleUiEvent.OnAllDayToggled(true))
            viewModel.onEvent(UpsertScheduleUiEvent.OnDateSelected(ActiveInlinePicker.END_DATE, LocalDate.of(2026, 11, 9)))

            viewModel.onEvent(UpsertScheduleUiEvent.OnSaveClicked)
            advanceUntilIdle()

            val savedItem = slot<ScheduleItem>()
            coVerify { saveScheduleUseCase(capture(savedItem)) }
            assertTrue(savedItem.captured.isAllDay)
            assertEquals(null, savedItem.captured.endTime)
            assertEquals(LocalDate.of(2026, 11, 9), savedItem.captured.endDate)
        }

    @Test
    fun `given editing when saving then the item keeps its id and the fields without a control yet`() =
        runTest(mainRule.dispatcher) {
            stubSaveSuccess()
            val original =
                scheduleItem(
                    id = "item-1",
                    title = "Aula",
                    endTime = LocalTime.of(11, 0),
                    recurrencePattern = RecurrencePattern.Daily(),
                ).copy(
                    colorHex = EventColorOption.TEAL.hex,
                    stickerEmoji = "📚",
                )
            coEvery { scheduleRepository.getById("item-1") } returns original
            val viewModel = createViewModel(scheduleId = "item-1")
            advanceUntilIdle()

            viewModel.onEvent(UpsertScheduleUiEvent.OnTitleChanged("Aula de Kotlin"))
            viewModel.onEvent(UpsertScheduleUiEvent.OnSaveClicked)
            advanceUntilIdle()

            val savedItem = slot<ScheduleItem>()
            coVerify { saveScheduleUseCase(capture(savedItem)) }
            assertEquals(original.copy(title = "Aula de Kotlin"), savedItem.captured)
        }

    @Test
    fun `given blank title error when saving then title error is shown and typing clears it`() =
        runTest(mainRule.dispatcher) {
            coEvery { saveScheduleUseCase(any()) } returns Result.failure(SaveScheduleError.BlankTitle)
            val viewModel = loadedCreation()
            val recorder = recordEffects(viewModel)

            viewModel.onEvent(UpsertScheduleUiEvent.OnSaveClicked)
            advanceUntilIdle()

            assertTrue(viewModel.content().titleError)
            assertFalse(viewModel.content().isSaving)
            assertTrue(recorder.effects.isEmpty())

            viewModel.onEvent(UpsertScheduleUiEvent.OnTitleChanged("A"))
            assertFalse(viewModel.content().titleError)
            recorder.job.cancel()
        }

    @Test
    fun `given end before start error when saving then it shows the message and stays on the screen`() =
        runTest(mainRule.dispatcher) {
            coEvery { saveScheduleUseCase(any()) } returns Result.failure(SaveScheduleError.EndBeforeStart)
            val viewModel = loadedCreation()
            val recorder = recordEffects(viewModel)

            viewModel.onEvent(UpsertScheduleUiEvent.OnSaveClicked)
            advanceUntilIdle()

            assertEquals(
                listOf<UpsertScheduleUiEffect>(UpsertScheduleUiEffect.ShowMessage(R.string.upsert_error_end_before_start)),
                recorder.effects,
            )
            assertFalse(viewModel.content().isSaving)
            recorder.job.cancel()
        }

    @Test
    fun `given persistence failure when saving then it shows the save error and keeps the typed data`() =
        runTest(mainRule.dispatcher) {
            coEvery { saveScheduleUseCase(any()) } returns Result.failure(IOException("disk full"))
            val viewModel = loadedCreation()
            val recorder = recordEffects(viewModel)
            viewModel.onEvent(UpsertScheduleUiEvent.OnTitleChanged("Consulta"))

            viewModel.onEvent(UpsertScheduleUiEvent.OnSaveClicked)
            advanceUntilIdle()

            assertEquals(listOf<UpsertScheduleUiEffect>(UpsertScheduleUiEffect.ShowMessage(R.string.upsert_save_error)), recorder.effects)
            assertEquals("Consulta", viewModel.content().title)
            assertFalse(viewModel.content().isSaving)
            recorder.job.cancel()
        }

    @Test
    fun `given save in progress when tapping save again then the use case runs only once`() =
        runTest(mainRule.dispatcher) {
            stubSaveSuccess()
            val viewModel = loadedCreation()

            viewModel.onEvent(UpsertScheduleUiEvent.OnSaveClicked)
            assertTrue(viewModel.content().isSaving)
            viewModel.onEvent(UpsertScheduleUiEvent.OnSaveClicked)
            advanceUntilIdle()

            coVerify(exactly = 1) { saveScheduleUseCase(any()) }
        }

    @Test
    fun `given cancel when clicked then it goes back without saving`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()
            val recorder = recordEffects(viewModel)

            viewModel.onEvent(UpsertScheduleUiEvent.OnCancelClicked)
            advanceUntilIdle()

            assertEquals(listOf<UpsertScheduleUiEffect>(UpsertScheduleUiEffect.NavigateBack), recorder.effects)
            coVerify(exactly = 0) { saveScheduleUseCase(any()) }
            recorder.job.cancel()
        }

    // endregion

    @Test
    fun `given other field events when handled then the content reflects them`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()

            viewModel.onEvent(UpsertScheduleUiEvent.OnReminderOffsetSelected(ReminderOffset.NONE))
            viewModel.onEvent(UpsertScheduleUiEvent.OnNotesChanged("linha 1\nlinha 2"))

            assertEquals(ReminderOffset.NONE, viewModel.content().reminderOffset)
            assertEquals("linha 1\nlinha 2", viewModel.content().notes)
        }

    // region recorrência (spec 08)

    @Test
    fun `given the recurrence row when clicked then the dialog becomes visible without changing the pattern`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()

            viewModel.onEvent(UpsertScheduleUiEvent.OnRecurrenceRowClicked)

            assertTrue(viewModel.content().isRecurrenceDialogVisible)
            assertEquals(RecurrencePattern.None, viewModel.content().recurrencePattern)
        }

    @Test
    fun `given the dialog is open when confirming a pattern then it is stored and the dialog closes`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()
            val pattern = RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), occurrenceLimit = 5000)
            viewModel.onEvent(UpsertScheduleUiEvent.OnRecurrenceRowClicked)

            viewModel.onEvent(UpsertScheduleUiEvent.OnRecurrencePatternConfirmed(pattern))

            assertEquals(pattern, viewModel.content().recurrencePattern)
            assertFalse(viewModel.content().isRecurrenceDialogVisible)
        }

    @Test
    fun `given a stored pattern and the dialog open when dismissing then the pattern is kept and the dialog closes`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedCreation()
            val pattern = RecurrencePattern.Daily(occurrenceLimit = 3)
            viewModel.onEvent(UpsertScheduleUiEvent.OnRecurrencePatternConfirmed(pattern))
            viewModel.onEvent(UpsertScheduleUiEvent.OnRecurrenceRowClicked)

            viewModel.onEvent(UpsertScheduleUiEvent.OnRecurrenceDialogDismissed)

            assertEquals(pattern, viewModel.content().recurrencePattern)
            assertFalse(viewModel.content().isRecurrenceDialogVisible)
        }

    @Test
    fun `given a confirmed pattern when saving then the saved item carries the pattern`() =
        runTest(mainRule.dispatcher) {
            val savedItem = slot<ScheduleItem>()
            coEvery { saveScheduleUseCase(capture(savedItem)) } answers { Result.success(firstArg()) }
            val viewModel = loadedCreation()
            val pattern = RecurrencePattern.Daily(occurrenceLimit = 4)
            viewModel.onEvent(UpsertScheduleUiEvent.OnTitleChanged("Remédio"))
            viewModel.onEvent(UpsertScheduleUiEvent.OnRecurrencePatternConfirmed(pattern))

            viewModel.onEvent(UpsertScheduleUiEvent.OnSaveClicked)
            advanceUntilIdle()

            assertEquals(pattern, savedItem.captured.recurrencePattern)
        }

    // endregion
}
