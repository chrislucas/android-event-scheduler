package com.br.androideventscheduler.feature.search.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.feature.search.domain.model.SearchFilter
import com.br.androideventscheduler.feature.search.presentation.effect.SearchUiEffect
import com.br.androideventscheduler.feature.search.presentation.event.SearchUiEvent
import com.br.androideventscheduler.feature.search.presentation.mapper.SearchResultMapper
import com.br.androideventscheduler.feature.search.presentation.state.SearchResultUi
import com.br.androideventscheduler.feature.search.presentation.state.SearchUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Andamento da consulta, privado ao ViewModel; [Done] e [Failed] lembram a que texto/filtro respondem. */
private sealed interface SearchResultsStatus {
    data object Idle : SearchResultsStatus

    data object InProgress : SearchResultsStatus

    data class Done(
        val query: String,
        val filter: SearchFilter,
        val results: List<SearchResultUi>,
    ) : SearchResultsStatus

    data class Failed(
        val query: String,
        val filter: SearchFilter,
    ) : SearchResultsStatus
}

/**
 * Pesquisa reativa. O `uiState` é **derivado** (`combine`) do texto e do filtro (no `SavedStateHandle`)
 * e do andamento da consulta; os eventos só escrevem nessas fontes. O *debounce* incide apenas sobre o
 * texto: trocar o filtro reconsulta na hora com o último texto estabilizado, e `flatMapLatest` cancela
 * a consulta anterior.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal class SearchViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val mapper: SearchResultMapper,
    private val savedStateHandle: SavedStateHandle,
    private val dispatcher: CoroutineDispatcher,
    private val debounceMillis: Long = DEBOUNCE_MILLIS,
) : ViewModel() {
    private val retryTrigger = MutableStateFlow(0)
    private val queryFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val filterFlow: Flow<SearchFilter> =
        savedStateHandle.getStateFlow(KEY_FILTER, SearchFilter.ALL.name).map { filterName ->
            SearchFilter.entries.firstOrNull { it.name == filterName } ?: SearchFilter.ALL
        }

    /** Últimos resultados exibidos; reaproveitados em [SearchUiState.Searching]. */
    private var lastResults: List<SearchResultUi> = emptyList()

    private val _uiState =
        MutableStateFlow(
            toUiState(
                query = queryFlow.value,
                filter = SearchFilter.entries.firstOrNull { it.name == savedStateHandle[KEY_FILTER] } ?: SearchFilter.ALL,
                status = SearchResultsStatus.Idle,
            ),
        )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<SearchUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<SearchUiEffect> = _uiEffect.receiveAsFlow()

    init {
        viewModelScope.launch(dispatcher) {
            combine(queryFlow, filterFlow, observeResultsStatus(), ::toUiState).collect { state -> _uiState.value = state }
        }
    }

    fun onEvent(event: SearchUiEvent) {
        when (event) {
            is SearchUiEvent.OnQueryChanged -> savedStateHandle[KEY_QUERY] = event.query
            SearchUiEvent.OnClearQueryClicked -> savedStateHandle[KEY_QUERY] = ""
            is SearchUiEvent.OnFilterSelected -> savedStateHandle[KEY_FILTER] = event.filter.name
            SearchUiEvent.OnRetryClicked -> retryTrigger.update { it + 1 }
            is SearchUiEvent.OnResultClicked -> sendEffect(SearchUiEffect.NavigateToDetail(event.scheduleId))
            SearchUiEvent.OnBackClicked -> sendEffect(SearchUiEffect.NavigateBack)
        }
    }

    private fun observeResultsStatus(): Flow<SearchResultsStatus> {
        val stableQuery =
            queryFlow
                .map { it.trim() }
                .distinctUntilChanged()
                .debounce { trimmedQuery -> if (trimmedQuery.isEmpty()) 0L else debounceMillis }
                .distinctUntilChanged()
        return combine(stableQuery, filterFlow, retryTrigger) { trimmedQuery, filter, _ -> trimmedQuery to filter }
            .flatMapLatest { (trimmedQuery, filter) -> search(trimmedQuery, filter) }
    }

    private fun search(
        trimmedQuery: String,
        filter: SearchFilter,
    ): Flow<SearchResultsStatus> {
        if (trimmedQuery.isEmpty()) return flowOf(SearchResultsStatus.Idle)
        val accountEmail = userSettingsRepository.observe().map { it.activeAccountEmail }.distinctUntilChanged()
        return combine(scheduleRepository.search(trimmedQuery, filter.targetType), accountEmail) { items, email ->
            SearchResultsStatus.Done(trimmedQuery, filter, mapper.map(items, email)) as SearchResultsStatus
        }.onStart { emit(SearchResultsStatus.InProgress) }
            .catch { emit(SearchResultsStatus.Failed(trimmedQuery, filter)) }
    }

    /**
     * Um resultado só vale se responde ao texto e ao filtro **atuais**; senão (debounce pendente, filtro
     * recém-trocado) a tela fica em `Searching` com os resultados anteriores.
     */
    private fun toUiState(
        query: String,
        filter: SearchFilter,
        status: SearchResultsStatus,
    ): SearchUiState {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            lastResults = emptyList()
            return SearchUiState.Idle(query, filter)
        }
        return when {
            status is SearchResultsStatus.Done && status.query == trimmedQuery && status.filter == filter -> {
                lastResults = status.results
                if (status.results.isEmpty()) {
                    SearchUiState.Empty(query, filter)
                } else {
                    SearchUiState.Results(query, filter, status.results)
                }
            }

            status is SearchResultsStatus.Failed && status.query == trimmedQuery && status.filter == filter -> {
                SearchUiState.Error(query, filter)
            }

            else -> {
                SearchUiState.Searching(query, filter, previousResults = lastResults)
            }
        }
    }

    private fun sendEffect(effect: SearchUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }

    companion object {
        const val DEBOUNCE_MILLIS = 250L
        private const val KEY_QUERY = "search_query"
        private const val KEY_FILTER = "search_filter"
    }
}
