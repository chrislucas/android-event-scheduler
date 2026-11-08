package com.br.androideventscheduler.feature.detail.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.common.testing.FakeStringProvider
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.testing.FakeUserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleNotFoundException
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.MainDispatcherRule
import com.br.androideventscheduler.feature.detail.domain.usecase.DeleteScheduleUseCase
import com.br.androideventscheduler.feature.detail.domain.usecase.DuplicateScheduleUseCase
import com.br.androideventscheduler.feature.detail.presentation.effect.ScheduleDetailUiEffect
import com.br.androideventscheduler.feature.detail.presentation.event.ScheduleDetailUiEvent
import com.br.androideventscheduler.feature.detail.presentation.mapper.ScheduleDetailMapper
import com.br.androideventscheduler.feature.detail.presentation.state.ScheduleDetailUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleDetailViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val mapper = ScheduleDetailMapper(FakeStringProvider())
    private val itemFlow = MutableStateFlow<ScheduleItem?>(null)
    private val scheduleRepository =
        mockk<ScheduleRepository> { every { observeById("item-1") } returns itemFlow }
    private val settingsRepository = FakeUserSettingsRepository(UserSettings(activeAccountEmail = "ana@exemplo.com"))
    private val duplicateSchedule = mockk<DuplicateScheduleUseCase>()
    private val deleteSchedule = mockk<DeleteScheduleUseCase>()

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        ScheduleDetailViewModel(
            scheduleId = "item-1",
            scheduleRepository = scheduleRepository,
            userSettingsRepository = settingsRepository,
            duplicateSchedule = duplicateSchedule,
            deleteSchedule = deleteSchedule,
            strings = FakeStringProvider(),
            savedStateHandle = savedStateHandle,
            mapper = mapper,
            dispatcher = mainRule.dispatcher,
        )

    /** Coleta os efeitos numa lista; cancele [job] ao final do teste (o coletor é um filho do `runTest`). */
    private class EffectRecorder(
        val effects: MutableList<ScheduleDetailUiEffect>,
        val job: Job,
    )

    private fun TestScope.recordEffects(viewModel: ScheduleDetailViewModel): EffectRecorder {
        val effects = mutableListOf<ScheduleDetailUiEffect>()
        return EffectRecorder(effects, launch { viewModel.uiEffect.collect { effects += it } })
    }

    private fun ScheduleDetailViewModel.content(): ScheduleDetailUiState.Content = uiState.value as ScheduleDetailUiState.Content

    @Test
    fun `given existing item when view model starts then it shows loading and then the mapped content`() =
        runTest(mainRule.dispatcher) {
            val item = scheduleItem(id = "item-1", title = "Aula")
            itemFlow.value = item

            val viewModel = createViewModel()
            assertEquals(ScheduleDetailUiState.Loading, viewModel.uiState.value)
            advanceUntilIdle()

            assertEquals(ScheduleDetailUiState.Content(mapper.map(item, "ana@exemplo.com")), viewModel.uiState.value)
        }

    @Test
    fun `given missing item when view model starts then it shows not found with a message and navigates back`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)

            advanceUntilIdle()

            assertEquals(ScheduleDetailUiState.NotFound, viewModel.uiState.value)
            assertEquals(
                listOf(ScheduleDetailUiEffect.ShowMessage(R.string.detail_not_found), ScheduleDetailUiEffect.NavigateBack),
                recorder.effects,
            )
            recorder.job.cancel()
        }

    @Test
    fun `given loaded item when it is deleted then it navigates back without an error message`() =
        runTest(mainRule.dispatcher) {
            itemFlow.value = scheduleItem(id = "item-1")
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            itemFlow.value = null
            advanceUntilIdle()

            assertEquals(listOf<ScheduleDetailUiEffect>(ScheduleDetailUiEffect.NavigateBack), recorder.effects)
            recorder.job.cancel()
        }

    @Test
    fun `given loaded item when it is edited elsewhere then the content is updated`() =
        runTest(mainRule.dispatcher) {
            itemFlow.value = scheduleItem(id = "item-1", title = "Antes")
            val viewModel = createViewModel()
            advanceUntilIdle()

            itemFlow.value = scheduleItem(id = "item-1", title = "Depois")
            advanceUntilIdle()

            assertEquals("Depois", viewModel.content().schedule.title)
        }

    @Test
    fun `given personal calendar item when account email changes then the calendar label is updated`() =
        runTest(mainRule.dispatcher) {
            itemFlow.value = scheduleItem(id = "item-1").copy(calendarSource = CalendarSource.PERSONAL_ACCOUNT)
            val viewModel = createViewModel()
            advanceUntilIdle()

            settingsRepository.settings.value = UserSettings(activeAccountEmail = "bia@exemplo.com")
            advanceUntilIdle()

            assertEquals("bia@exemplo.com", viewModel.content().schedule.calendarLabel)
        }

    @Test
    fun `given loaded item when edit is clicked then it navigates to edit with the schedule id`() =
        runTest(mainRule.dispatcher) {
            itemFlow.value = scheduleItem(id = "item-1")
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(ScheduleDetailUiEvent.OnEditClicked)
            advanceUntilIdle()

            assertEquals(listOf<ScheduleDetailUiEffect>(ScheduleDetailUiEffect.NavigateToEdit("item-1")), recorder.effects)
            recorder.job.cancel()
        }

    @Test
    fun `given loaded item when back is clicked then it navigates back`() =
        runTest(mainRule.dispatcher) {
            itemFlow.value = scheduleItem(id = "item-1")
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(ScheduleDetailUiEvent.OnBackClicked)
            advanceUntilIdle()

            assertEquals(listOf<ScheduleDetailUiEffect>(ScheduleDetailUiEffect.NavigateBack), recorder.effects)
            recorder.job.cancel()
        }

    @Test
    fun `given not found item when account email changes later then back is not emitted twice`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            settingsRepository.settings.value = UserSettings(activeAccountEmail = "bia@exemplo.com")
            advanceUntilIdle()

            assertEquals(2, recorder.effects.size)
            recorder.job.cancel()
        }

    private fun loadedViewModel(
        item: ScheduleItem = scheduleItem(id = "item-1"),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): ScheduleDetailViewModel {
        itemFlow.value = item
        return createViewModel(savedStateHandle)
    }

    @Test
    fun `given recurring item when loaded then the content is flagged as recurring`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedViewModel(scheduleItem(id = "item-1", recurrencePattern = RecurrencePattern.Daily()))

            advanceUntilIdle()

            assertTrue(viewModel.content().schedule.isRecurring)
        }

    @Test
    fun `given loaded item when duplicate succeeds then it emits copy created with the copy id and unlocks the actions`() =
        runTest(mainRule.dispatcher) {
            val copy = scheduleItem(id = "copy-1", title = "Reunião string(${R.string.detail_copy_title_suffix})")
            coEvery { duplicateSchedule("item-1", "string(${R.string.detail_copy_title_suffix})") } returns Result.success(copy)
            val viewModel = loadedViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(ScheduleDetailUiEvent.OnDuplicateClicked)
            advanceUntilIdle()

            assertEquals(listOf<ScheduleDetailUiEffect>(ScheduleDetailUiEffect.ShowCopyCreated("copy-1")), recorder.effects)
            assertFalse(viewModel.content().isProcessingAction)
            recorder.job.cancel()
        }

    @Test
    fun `given loaded item when duplicate fails then it shows the generic error and unlocks the actions`() =
        runTest(mainRule.dispatcher) {
            coEvery { duplicateSchedule(any(), any()) } returns Result.failure(IOException("disk full"))
            val viewModel = loadedViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(ScheduleDetailUiEvent.OnDuplicateClicked)
            advanceUntilIdle()

            assertEquals(
                listOf<ScheduleDetailUiEffect>(ScheduleDetailUiEffect.ShowMessage(R.string.detail_duplicate_error)),
                recorder.effects,
            )
            assertFalse(viewModel.content().isProcessingAction)
            recorder.job.cancel()
        }

    @Test
    fun `given original removed when duplicating then it shows the not found message`() =
        runTest(mainRule.dispatcher) {
            coEvery { duplicateSchedule(any(), any()) } returns Result.failure(ScheduleNotFoundException("item-1"))
            val viewModel = loadedViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(ScheduleDetailUiEvent.OnDuplicateClicked)
            advanceUntilIdle()

            assertEquals(
                listOf<ScheduleDetailUiEffect>(ScheduleDetailUiEffect.ShowMessage(R.string.detail_not_found)),
                recorder.effects,
            )
            recorder.job.cancel()
        }

    @Test
    fun `given duplicate in progress when duplicate is clicked again then the second tap is ignored`() =
        runTest(mainRule.dispatcher) {
            val gate = CompletableDeferred<Unit>()
            coEvery { duplicateSchedule(any(), any()) } coAnswers {
                gate.await()
                Result.success(scheduleItem(id = "copy-1"))
            }
            val viewModel = loadedViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(ScheduleDetailUiEvent.OnDuplicateClicked)
            viewModel.onEvent(ScheduleDetailUiEvent.OnDuplicateClicked)
            advanceUntilIdle()
            assertTrue(viewModel.content().isProcessingAction)
            gate.complete(Unit)
            advanceUntilIdle()

            coVerify(exactly = 1) { duplicateSchedule(any(), any()) }
            assertEquals(1, recorder.effects.size)
            recorder.job.cancel()
        }

    @Test
    fun `given loaded item when delete is clicked then the dialog opens and dismiss closes it without deleting`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleDetailUiEvent.OnDeleteClicked)
            assertTrue(viewModel.content().isDeleteDialogVisible)
            viewModel.onEvent(ScheduleDetailUiEvent.OnDismissDeleteDialog)

            assertFalse(viewModel.content().isDeleteDialogVisible)
            coVerify(exactly = 0) { deleteSchedule(any()) }
        }

    @Test
    fun `given saved dialog flag when view model is recreated then the delete dialog is restored`() =
        runTest(mainRule.dispatcher) {
            val savedStateHandle = SavedStateHandle()
            val first = loadedViewModel(savedStateHandle = savedStateHandle)
            advanceUntilIdle()
            first.onEvent(ScheduleDetailUiEvent.OnDeleteClicked)

            val recreated = createViewModel(savedStateHandle)
            advanceUntilIdle()

            assertTrue(recreated.content().isDeleteDialogVisible)
        }

    @Test
    fun `given open dialog when delete is confirmed then it deletes closes the dialog and does not navigate by itself`() =
        runTest(mainRule.dispatcher) {
            coEvery { deleteSchedule("item-1") } returns Result.success(Unit)
            val viewModel = loadedViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()
            viewModel.onEvent(ScheduleDetailUiEvent.OnDeleteClicked)

            viewModel.onEvent(ScheduleDetailUiEvent.OnConfirmDeleteClicked)
            advanceUntilIdle()

            coVerify(exactly = 1) { deleteSchedule("item-1") }
            assertFalse(viewModel.content().isDeleteDialogVisible)
            assertTrue(recorder.effects.isEmpty())
            recorder.job.cancel()
        }

    @Test
    fun `given confirmed delete when the observed item disappears then back is emitted exactly once`() =
        runTest(mainRule.dispatcher) {
            coEvery { deleteSchedule("item-1") } coAnswers {
                itemFlow.value = null
                Result.success(Unit)
            }
            val viewModel = loadedViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()
            viewModel.onEvent(ScheduleDetailUiEvent.OnDeleteClicked)

            viewModel.onEvent(ScheduleDetailUiEvent.OnConfirmDeleteClicked)
            advanceUntilIdle()

            assertEquals(listOf<ScheduleDetailUiEffect>(ScheduleDetailUiEffect.NavigateBack), recorder.effects)
            recorder.job.cancel()
        }

    @Test
    fun `given open dialog when delete fails then it shows the error keeps the screen and unlocks the actions`() =
        runTest(mainRule.dispatcher) {
            coEvery { deleteSchedule("item-1") } returns Result.failure(IOException("db locked"))
            val viewModel = loadedViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()
            viewModel.onEvent(ScheduleDetailUiEvent.OnDeleteClicked)

            viewModel.onEvent(ScheduleDetailUiEvent.OnConfirmDeleteClicked)
            advanceUntilIdle()

            assertEquals(
                listOf<ScheduleDetailUiEffect>(ScheduleDetailUiEffect.ShowMessage(R.string.detail_delete_error)),
                recorder.effects,
            )
            assertFalse(viewModel.content().isProcessingAction)
            recorder.job.cancel()
        }

    @Test
    fun `given closed dialog when confirm arrives then nothing is deleted`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ScheduleDetailUiEvent.OnConfirmDeleteClicked)
            advanceUntilIdle()

            coVerify(exactly = 0) { deleteSchedule(any()) }
        }

    @Test
    fun `given delete in progress when confirm is tapped again then the use case runs once`() =
        runTest(mainRule.dispatcher) {
            val gate = CompletableDeferred<Unit>()
            coEvery { deleteSchedule("item-1") } coAnswers {
                gate.await()
                Result.success(Unit)
            }
            val viewModel = loadedViewModel()
            advanceUntilIdle()
            viewModel.onEvent(ScheduleDetailUiEvent.OnDeleteClicked)

            viewModel.onEvent(ScheduleDetailUiEvent.OnConfirmDeleteClicked)
            viewModel.onEvent(ScheduleDetailUiEvent.OnConfirmDeleteClicked)
            viewModel.onEvent(ScheduleDetailUiEvent.OnDeleteClicked)
            advanceUntilIdle()

            assertFalse(viewModel.content().isDeleteDialogVisible)
            gate.complete(Unit)
            advanceUntilIdle()
            coVerify(exactly = 1) { deleteSchedule("item-1") }
        }

    @Test
    fun `given copy snackbar action when open copy is clicked then it navigates to the copy`() =
        runTest(mainRule.dispatcher) {
            val viewModel = loadedViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(ScheduleDetailUiEvent.OnOpenCopyClicked("copy-1"))
            advanceUntilIdle()

            assertEquals(listOf<ScheduleDetailUiEffect>(ScheduleDetailUiEffect.NavigateToCopy("copy-1")), recorder.effects)
            recorder.job.cancel()
        }
}
