package com.br.androideventscheduler.feature.search.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.testing.FakeUserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.MainDispatcherRule
import com.br.androideventscheduler.feature.search.domain.model.SearchFilter
import com.br.androideventscheduler.feature.search.presentation.effect.SearchUiEffect
import com.br.androideventscheduler.feature.search.presentation.event.SearchUiEvent
import com.br.androideventscheduler.feature.search.presentation.mapper.SearchResultMapper
import com.br.androideventscheduler.feature.search.presentation.state.SearchUiState
import com.br.androideventscheduler.feature.search.testing.FakeScheduleRepository
import com.br.androideventscheduler.feature.search.testing.FakeScheduleRepository.SearchCall
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR")
class SearchViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val scheduleRepository = FakeScheduleRepository()
    private val userSettingsRepository = FakeUserSettingsRepository(UserSettings(activeAccountEmail = "bia@exemplo.com"))
    private val meeting = scheduleItem(id = "meeting", title = "Reunião")
    private val lunch = scheduleItem(id = "lunch", title = "Almoço", type = ScheduleType.REMINDER)

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        SearchViewModel(
            scheduleRepository = scheduleRepository,
            userSettingsRepository = userSettingsRepository,
            mapper = SearchResultMapper(context.resources),
            savedStateHandle = savedStateHandle,
            dispatcher = mainRule.dispatcher,
        )

    @Test
    fun `given a new screen when created then state is idle and nothing is queried`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()

            runCurrent()

            assertEquals(SearchUiState.Idle("", SearchFilter.ALL), viewModel.uiState.value)
            assertTrue(scheduleRepository.searchCalls.isEmpty())
        }

    @Test
    fun `given blank text when typing spaces then state stays idle and nothing is queried`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.onEvent(SearchUiEvent.OnQueryChanged("   "))
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(SearchUiState.Idle("   ", SearchFilter.ALL), viewModel.uiState.value)
            assertTrue(scheduleRepository.searchCalls.isEmpty())
        }

    @Test
    fun `given fast typing when under debounce then a single query runs with the final text`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.onEvent(SearchUiEvent.OnQueryChanged("r"))
            advanceTimeBy(100)
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("re"))
            advanceTimeBy(100)
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("reu"))
            advanceTimeBy(249)
            runCurrent()

            assertTrue(scheduleRepository.searchCalls.isEmpty())
            advanceTimeBy(1)
            runCurrent()
            assertEquals(listOf(SearchCall("reu", null)), scheduleRepository.searchCalls)
        }

    @Test
    fun `given typing when waiting for debounce then the field text is immediate and state is searching`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.onEvent(SearchUiEvent.OnQueryChanged("reu"))
            runCurrent()

            assertEquals(SearchUiState.Searching("reu", SearchFilter.ALL, previousResults = emptyList()), viewModel.uiState.value)
        }

    @Test
    fun `given text with surrounding spaces when querying then the repository receives the trimmed text`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.onEvent(SearchUiEvent.OnQueryChanged("  reu "))
            advanceTimeBy(250)
            runCurrent()

            assertEquals(listOf(SearchCall("reu", null)), scheduleRepository.searchCalls)
            assertEquals("  reu ", viewModel.uiState.value.query)
        }

    @Test
    fun `given matching items when query settles then state is results with mapped items`() =
        runTest(mainRule.dispatcher) {
            scheduleRepository.searchHandler = { _, _ -> flowOf(listOf(meeting)) }
            val viewModel = createViewModel()
            runCurrent()

            viewModel.onEvent(SearchUiEvent.OnQueryChanged("reu"))
            advanceTimeBy(250)
            runCurrent()

            val state = viewModel.uiState.value as SearchUiState.Results
            assertEquals(listOf("meeting"), state.results.map { it.id })
            assertEquals("bia@exemplo.com", state.results.single().calendarLabel)
        }

    @Test
    fun `given no matches when query settles then state is empty`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.onEvent(SearchUiEvent.OnQueryChanged("zzz"))
            advanceTimeBy(250)
            runCurrent()

            assertEquals(SearchUiState.Empty("zzz", SearchFilter.ALL), viewModel.uiState.value)
        }

    @Test
    fun `given results shown when filter changes then it requeries immediately without waiting for debounce`() =
        runTest(mainRule.dispatcher) {
            scheduleRepository.searchHandler = { _, type -> flowOf(listOfNotNull(lunch.takeIf { type != ScheduleType.EVENT })) }
            val viewModel = createViewModel()
            runCurrent()
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("a"))
            advanceTimeBy(250)
            runCurrent()

            viewModel.onEvent(SearchUiEvent.OnFilterSelected(SearchFilter.EVENTS))
            runCurrent()

            assertEquals(listOf(SearchCall("a", null), SearchCall("a", ScheduleType.EVENT)), scheduleRepository.searchCalls)
            assertEquals(SearchUiState.Empty("a", SearchFilter.EVENTS), viewModel.uiState.value)
        }

    @Test
    fun `given filter changes while typing is pending when settled then only one query uses the new filter`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.onEvent(SearchUiEvent.OnQueryChanged("reu"))
            advanceTimeBy(100)
            viewModel.onEvent(SearchUiEvent.OnFilterSelected(SearchFilter.REMINDERS))
            advanceTimeBy(250)
            runCurrent()

            assertEquals(listOf(SearchCall("reu", ScheduleType.REMINDER)), scheduleRepository.searchCalls.distinct())
        }

    @Test
    fun `given an old query still pending when a newer one answers then the late old response is discarded`() =
        runTest(mainRule.dispatcher) {
            val oldResponse = MutableSharedFlow<List<ScheduleItem>>()
            scheduleRepository.searchHandler = { query, _ -> if (query == "a") oldResponse else flowOf(listOf(meeting)) }
            val viewModel = createViewModel()
            runCurrent()
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("a"))
            advanceTimeBy(250)
            runCurrent()

            viewModel.onEvent(SearchUiEvent.OnQueryChanged("ab"))
            advanceTimeBy(250)
            runCurrent()
            oldResponse.emit(listOf(lunch))
            runCurrent()

            val state = viewModel.uiState.value as SearchUiState.Results
            assertEquals(listOf("meeting"), state.results.map { it.id })
            assertEquals("ab", state.query)
        }

    @Test
    fun `given results shown when typing more then previous results are kept while searching`() =
        runTest(mainRule.dispatcher) {
            scheduleRepository.searchHandler = { _, _ -> flowOf(listOf(meeting)) }
            val viewModel = createViewModel()
            runCurrent()
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("reu"))
            advanceTimeBy(250)
            runCurrent()

            viewModel.onEvent(SearchUiEvent.OnQueryChanged("reun"))
            runCurrent()

            val state = viewModel.uiState.value as SearchUiState.Searching
            assertEquals(listOf("meeting"), state.previousResults.map { it.id })
        }

    @Test
    fun `given results shown when clearing then field and list are emptied and no new query runs`() =
        runTest(mainRule.dispatcher) {
            scheduleRepository.searchHandler = { _, _ -> flowOf(listOf(meeting)) }
            val viewModel = createViewModel()
            runCurrent()
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("reu"))
            advanceTimeBy(250)
            runCurrent()

            viewModel.onEvent(SearchUiEvent.OnClearQueryClicked)
            runCurrent()

            assertEquals(SearchUiState.Idle("", SearchFilter.ALL), viewModel.uiState.value)
            assertEquals(1, scheduleRepository.searchCalls.size)
        }

    @Test
    fun `given saved text and filter when recreated then the state is restored and queried`() =
        runTest(mainRule.dispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("search_query" to "reu", "search_filter" to SearchFilter.EVENTS.name))

            val viewModel = createViewModel(savedStateHandle)

            assertEquals(SearchUiState.Searching("reu", SearchFilter.EVENTS, emptyList()), viewModel.uiState.value)
            advanceTimeBy(250)
            runCurrent()
            assertEquals(listOf(SearchCall("reu", ScheduleType.EVENT)), scheduleRepository.searchCalls)
        }

    @Test
    fun `given a repository failure when searching then state is error and retry recovers`() =
        runTest(mainRule.dispatcher) {
            var shouldFail = true
            scheduleRepository.searchHandler = { _, _ ->
                if (shouldFail) flow { throw IOException("db down") } else flowOf(listOf(meeting))
            }
            val viewModel = createViewModel()
            runCurrent()
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("reu"))
            advanceTimeBy(250)
            runCurrent()
            assertEquals(SearchUiState.Error("reu", SearchFilter.ALL), viewModel.uiState.value)

            shouldFail = false
            viewModel.onEvent(SearchUiEvent.OnRetryClicked)
            runCurrent()

            assertTrue(viewModel.uiState.value is SearchUiState.Results)
        }

    @Test
    fun `given the account email changes when results are shown then calendar labels update`() =
        runTest(mainRule.dispatcher) {
            scheduleRepository.searchHandler = { _, _ -> flowOf(listOf(meeting)) }
            val viewModel = createViewModel()
            runCurrent()
            viewModel.onEvent(SearchUiEvent.OnQueryChanged("reu"))
            advanceTimeBy(250)
            runCurrent()

            userSettingsRepository.settings.value = UserSettings(activeAccountEmail = "novo@exemplo.com")
            runCurrent()

            val state = viewModel.uiState.value as SearchUiState.Results
            assertEquals("novo@exemplo.com", state.results.single().calendarLabel)
        }

    @Test
    fun `given result clicked when handling event then emits NavigateToDetail`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            val effects = mutableListOf<SearchUiEffect>()
            val collectJob = launch { viewModel.uiEffect.toList(effects) }

            viewModel.onEvent(SearchUiEvent.OnResultClicked("42"))
            runCurrent()

            assertEquals(listOf<SearchUiEffect>(SearchUiEffect.NavigateToDetail("42")), effects)
            collectJob.cancel()
        }

    @Test
    fun `given back clicked when handling event then emits NavigateBack`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            val effects = mutableListOf<SearchUiEffect>()
            val collectJob = launch { viewModel.uiEffect.toList(effects) }

            viewModel.onEvent(SearchUiEvent.OnBackClicked)
            runCurrent()

            assertEquals(listOf<SearchUiEffect>(SearchUiEffect.NavigateBack), effects)
            collectJob.cancel()
        }
}
