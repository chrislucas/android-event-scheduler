package com.br.androideventscheduler.feature.alarm.presentation.viewmodel

import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.alarm.domain.usecase.SnoozeAlarmUseCase
import com.br.androideventscheduler.core.common.testing.FakeStringProvider
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.testing.FakeUserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleNotFoundException
import com.br.androideventscheduler.core.schedule.domain.recurrence.RecurrenceEngine
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.FakeClock
import com.br.androideventscheduler.core.testing.MainDispatcherRule
import com.br.androideventscheduler.feature.alarm.presentation.effect.FullScreenAlarmUiEffect
import com.br.androideventscheduler.feature.alarm.presentation.event.FullScreenAlarmUiEvent
import com.br.androideventscheduler.feature.alarm.presentation.mapper.FullScreenAlarmMapper
import com.br.androideventscheduler.feature.alarm.presentation.state.FullScreenAlarmUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class FullScreenAlarmViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    // 2026-11-07 12:00 em São Paulo.
    private val clock = FakeClock(Instant.parse("2026-11-07T15:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val mapper = FullScreenAlarmMapper(FakeStringProvider(), RecurrenceEngine(), clock)
    private val scheduleRepository = mockk<ScheduleRepository>()
    private val settingsRepository = FakeUserSettingsRepository()
    private val snoozeAlarmUseCase = mockk<SnoozeAlarmUseCase>()

    private val upcomingEvent =
        scheduleItem(id = "item-1", title = "Reunião", startDate = LocalDate.of(2026, 11, 7), startTime = LocalTime.of(12, 15))

    private fun createViewModel() =
        FullScreenAlarmViewModel(
            scheduleId = "item-1",
            scheduleRepository = scheduleRepository,
            userSettingsRepository = settingsRepository,
            snoozeAlarmUseCase = snoozeAlarmUseCase,
            mapper = mapper,
            dispatcher = mainRule.dispatcher,
        )

    private class EffectRecorder(
        val effects: MutableList<FullScreenAlarmUiEffect>,
        val job: Job,
    )

    private fun TestScope.recordEffects(viewModel: FullScreenAlarmViewModel): EffectRecorder {
        val effects = mutableListOf<FullScreenAlarmUiEffect>()
        return EffectRecorder(effects, launch { viewModel.uiEffect.collect { effects += it } })
    }

    private fun FullScreenAlarmViewModel.content(): FullScreenAlarmUiState.Content = uiState.value as FullScreenAlarmUiState.Content

    @Test
    fun `given existing item when view model starts then it shows loading and then the content`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("item-1") } returns upcomingEvent

            val viewModel = createViewModel()
            assertEquals(FullScreenAlarmUiState.Loading, viewModel.uiState.value)
            advanceUntilIdle()

            assertEquals(mapper.map(upcomingEvent, UserSettings.DEFAULT_SNOOZE_MINUTES), viewModel.content())
            assertEquals(15, viewModel.content().minutesUntilStart)
        }

    // Bloqueio do app (spec 16)
    @Test
    fun `given app lock on when view model starts then location and notes are hidden but title and time are kept`() =
        runTest(mainRule.dispatcher) {
            val detailed = upcomingEvent.copy(location = "Sala 3", notes = "Levar o notebook")
            coEvery { scheduleRepository.getById("item-1") } returns detailed
            settingsRepository.settings.value = UserSettings(appLockEnabled = true)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val content = viewModel.content()
            assertNull(content.location)
            assertNull(content.notes)
            assertEquals("Reunião", content.title)
            assertEquals(mapper.map(detailed, UserSettings.DEFAULT_SNOOZE_MINUTES).timeText, content.timeText)
        }

    @Test
    fun `given app lock off when view model starts then location and notes are shown`() =
        runTest(mainRule.dispatcher) {
            val detailed = upcomingEvent.copy(location = "Sala 3", notes = "Levar o notebook")
            coEvery { scheduleRepository.getById("item-1") } returns detailed

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals("Sala 3", viewModel.content().location)
            assertEquals("Levar o notebook", viewModel.content().notes)
        }

    @Test
    fun `given default snooze of five in settings when view model starts then the five minute chip is selected`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("item-1") } returns upcomingEvent
            settingsRepository.settings.value = UserSettings(defaultSnoozeMinutes = 5)

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(5, viewModel.content().selectedSnoozeMinutes)
            assertEquals(listOf(5, 10, 15), viewModel.content().snoozeOptions)
        }

    @Test
    fun `given default snooze outside the options when view model starts then it falls back to ten`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("item-1") } returns upcomingEvent
            settingsRepository.settings.value = UserSettings(defaultSnoozeMinutes = 7)

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(10, viewModel.content().selectedSnoozeMinutes)
        }

    @Test
    fun `given missing item when view model starts then it closes the screen cancelling the notification`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("item-1") } returns null

            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            assertEquals(listOf(FullScreenAlarmUiEffect.CloseScreen(cancelNotification = true)), recorder.effects)
            recorder.job.cancel()
        }

    @Test
    fun `given repository failure when view model starts then it closes without cancelling the notification`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("item-1") } throws IOException("disk")

            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            assertEquals(listOf(FullScreenAlarmUiEffect.CloseScreen(cancelNotification = false)), recorder.effects)
            recorder.job.cancel()
        }

    @Test
    fun `given another duration selected when snoozing then the use case receives it and the screen closes cancelling`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("item-1") } returns upcomingEvent
            coEvery { snoozeAlarmUseCase("item-1", 15) } returns Result.success(1L)
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(FullScreenAlarmUiEvent.OnSnoozeDurationSelected(15))
            viewModel.onEvent(FullScreenAlarmUiEvent.OnSnoozeClicked)
            advanceUntilIdle()

            coVerify(exactly = 1) { snoozeAlarmUseCase("item-1", 15) }
            assertEquals(listOf(FullScreenAlarmUiEffect.CloseScreen(cancelNotification = true)), recorder.effects)
            recorder.job.cancel()
        }

    @Test
    fun `given unsupported duration when selecting then the selection is unchanged`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("item-1") } returns upcomingEvent
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(FullScreenAlarmUiEvent.OnSnoozeDurationSelected(99))

            assertEquals(10, viewModel.content().selectedSnoozeMinutes)
        }

    @Test
    fun `given scheduler failure when snoozing then the screen stays with an error message`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("item-1") } returns upcomingEvent
            coEvery { snoozeAlarmUseCase(any(), any()) } returns Result.failure(SecurityException("denied"))
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(FullScreenAlarmUiEvent.OnSnoozeClicked)
            advanceUntilIdle()

            assertEquals(R.string.alarm_snooze_error, viewModel.content().errorMessageRes)
            assertFalse(viewModel.content().isProcessing)
            assertTrue(recorder.effects.isEmpty())
            recorder.job.cancel()
        }

    @Test
    fun `given a failed snooze when selecting another duration then the error is cleared`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("item-1") } returns upcomingEvent
            coEvery { snoozeAlarmUseCase(any(), any()) } returns Result.failure(IOException("x"))
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onEvent(FullScreenAlarmUiEvent.OnSnoozeClicked)
            advanceUntilIdle()

            viewModel.onEvent(FullScreenAlarmUiEvent.OnSnoozeDurationSelected(5))

            assertNull(viewModel.content().errorMessageRes)
            assertEquals(5, viewModel.content().selectedSnoozeMinutes)
        }

    @Test
    fun `given item deleted meanwhile when snoozing then the screen closes`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("item-1") } returns upcomingEvent
            coEvery { snoozeAlarmUseCase(any(), any()) } returns Result.failure(ScheduleNotFoundException("item-1"))
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(FullScreenAlarmUiEvent.OnSnoozeClicked)
            advanceUntilIdle()

            assertEquals(listOf(FullScreenAlarmUiEffect.CloseScreen(cancelNotification = true)), recorder.effects)
            recorder.job.cancel()
        }

    @Test
    fun `given a snooze in progress when tapping snooze again then the second tap is ignored`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("item-1") } returns upcomingEvent
            val gate = CompletableDeferred<Result<Long>>()
            coEvery { snoozeAlarmUseCase(any(), any()) } coAnswers { gate.await() }
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(FullScreenAlarmUiEvent.OnSnoozeClicked)
            viewModel.onEvent(FullScreenAlarmUiEvent.OnSnoozeClicked)
            advanceUntilIdle()
            assertTrue(viewModel.content().isProcessing)
            gate.complete(Result.success(1L))
            advanceUntilIdle()

            coVerify(exactly = 1) { snoozeAlarmUseCase(any(), any()) }
        }

    @Test
    fun `given a snooze in progress when dismissing or going back then nothing happens`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("item-1") } returns upcomingEvent
            coEvery { snoozeAlarmUseCase(any(), any()) } coAnswers { CompletableDeferred<Result<Long>>().await() }
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(FullScreenAlarmUiEvent.OnSnoozeClicked)
            viewModel.onEvent(FullScreenAlarmUiEvent.OnDismissClicked)
            viewModel.onEvent(FullScreenAlarmUiEvent.OnBackPressed)
            advanceUntilIdle()

            assertTrue(recorder.effects.isEmpty())
            recorder.job.cancel()
            mainRule.dispatcher.scheduler.advanceUntilIdle()
        }

    @Test
    fun `given content when dismissing then it closes cancelling the notification`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("item-1") } returns upcomingEvent
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(FullScreenAlarmUiEvent.OnDismissClicked)
            advanceUntilIdle()

            assertEquals(listOf(FullScreenAlarmUiEffect.CloseScreen(cancelNotification = true)), recorder.effects)
            coVerify(exactly = 0) { snoozeAlarmUseCase(any(), any()) }
            recorder.job.cancel()
        }

    @Test
    fun `given content when pressing back then it closes without cancelling the notification`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("item-1") } returns upcomingEvent
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(FullScreenAlarmUiEvent.OnBackPressed)
            advanceUntilIdle()

            assertEquals(listOf(FullScreenAlarmUiEffect.CloseScreen(cancelNotification = false)), recorder.effects)
            recorder.job.cancel()
        }

    @Test
    fun `given a new alarm for the open screen when reload is requested then the content is refreshed`() =
        runTest(mainRule.dispatcher) {
            coEvery { scheduleRepository.getById("item-1") } returns upcomingEvent
            val viewModel = createViewModel()
            advanceUntilIdle()

            coEvery { scheduleRepository.getById("item-1") } returns upcomingEvent.copy(title = "Reunião adiada")
            viewModel.onEvent(FullScreenAlarmUiEvent.OnReloadRequested)
            advanceUntilIdle()

            assertEquals("Reunião adiada", viewModel.content().title)
        }
}
