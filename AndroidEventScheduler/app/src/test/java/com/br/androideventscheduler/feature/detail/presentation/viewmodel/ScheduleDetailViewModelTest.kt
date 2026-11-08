package com.br.androideventscheduler.feature.detail.presentation.viewmodel

import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.common.testing.FakeStringProvider
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.testing.FakeUserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.MainDispatcherRule
import com.br.androideventscheduler.feature.detail.presentation.effect.ScheduleDetailUiEffect
import com.br.androideventscheduler.feature.detail.presentation.event.ScheduleDetailUiEvent
import com.br.androideventscheduler.feature.detail.presentation.mapper.ScheduleDetailMapper
import com.br.androideventscheduler.feature.detail.presentation.state.ScheduleDetailUiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleDetailViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val mapper = ScheduleDetailMapper(FakeStringProvider())
    private val itemFlow = MutableStateFlow<ScheduleItem?>(null)
    private val scheduleRepository =
        mockk<ScheduleRepository> { every { observeById("item-1") } returns itemFlow }
    private val settingsRepository = FakeUserSettingsRepository(UserSettings(activeAccountEmail = "ana@exemplo.com"))

    private fun createViewModel() =
        ScheduleDetailViewModel(
            scheduleId = "item-1",
            scheduleRepository = scheduleRepository,
            userSettingsRepository = settingsRepository,
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
}
