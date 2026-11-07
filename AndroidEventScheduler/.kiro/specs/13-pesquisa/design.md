# 13 — Pesquisa de tarefas e lembretes — Design

## Pacotes
```
feature/search/
├── domain/model/SearchFilter.kt                 # enum ALL(null) / EVENTS(EVENT) / REMINDERS(REMINDER) com targetType
├── presentation/
│   ├── state/    SearchUiState.kt, SearchResultUi.kt
│   ├── event/    SearchUiEvent.kt
│   ├── effect/   SearchUiEffect.kt
│   ├── viewmodel/SearchViewModel.kt
│   ├── mapper/   SearchResultMapper.kt
│   ├── component/ SearchFilterChips.kt, SearchResultItem.kt, SearchEmptyState.kt
│   └── screen/   SearchScreen.kt (+ SearchContent)
├── navigation/SearchEntry.kt
└── di/SearchModule.kt
```
`SearchFilter` é do domínio de busca (mapeia para `ScheduleType?`); o rótulo pt-BR é recurso de apresentação.

## Estado / eventos / efeitos
```kotlin
sealed class SearchUiState {
    abstract val query: String                      // texto bruto, sempre atualizado na hora (campo responsivo)
    abstract val selectedFilter: SearchFilter
    data class Idle(override val query: String, override val selectedFilter: SearchFilter) : SearchUiState()          // dica "Digite para pesquisar"
    data class Searching(override val query: String, override val selectedFilter: SearchFilter,
                         val previousResults: List<SearchResultUi>) : SearchUiState()                                 // mantém resultados anteriores
    data class Results(override val query: String, override val selectedFilter: SearchFilter,
                       val results: List<SearchResultUi>) : SearchUiState()
    data class Empty(override val query: String, override val selectedFilter: SearchFilter) : SearchUiState()
    data class Error(override val query: String, override val selectedFilter: SearchFilter) : SearchUiState()
}
sealed interface SearchUiEvent { OnQueryChanged(String); OnClearQueryClicked; OnFilterSelected(SearchFilter); OnResultClicked(String); OnRetryClicked; OnBackClicked }
sealed interface SearchUiEffect { NavigateBack; NavigateToDetail(String) }
```

## ViewModel
```kotlin
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val mapper: SearchResultMapper,
    private val savedStateHandle: SavedStateHandle,
    private val dispatcher: CoroutineDispatcher,
    private val debounceMillis: Long = DEBOUNCE_MILLIS,   // 250
)
```
Pipeline (em `init`) — o `uiState` é derivado de fontes privadas, sem mutação direta por evento:
```kotlin
val queryFlow  = savedStateHandle.getStateFlow(KEY_QUERY, "")                              // texto bruto, imediato
val filterFlow = savedStateHandle.getStateFlow(KEY_FILTER, SearchFilter.ALL.name).map(SearchFilter::valueOf)
val debouncedQuery = queryFlow.map(String::trim).distinctUntilChanged()
    .debounce { if (it.isBlank()) 0L else debounceMillis }
val resultsFlow: Flow<SearchResultsStatus> = combine(debouncedQuery, filterFlow, retryTrigger) { q, f, _ -> q to f }
    .flatMapLatest { (q, f) ->
        if (q.isBlank()) flowOf(SearchResultsStatus.Idle)
        else scheduleRepository.search(q, f.targetType).map<_, SearchResultsStatus> { SearchResultsStatus.Done(mapper.map(it)) }
            .onStart { emit(SearchResultsStatus.InProgress) }
            .catch { emit(SearchResultsStatus.Failed) }
    }
combine(queryFlow, filterFlow, resultsFlow) { query, filter, status -> toUiState(query, filter, status) }  // Idle/Searching/Results/Empty/Error
    .flowOn(dispatcher).collect { _uiState.value = it }
```
(`SearchResultsStatus` é um tipo privado do ViewModel; `toUiState` mantém os últimos resultados em `Searching`.)
- **Filtro imediato (R2.2):** o *debounce* só incide sobre o texto; o `combine` reage ao filtro sem espera porque o último texto debounced é reutilizado. 
- `OnQueryChanged` apenas escreve em `savedStateHandle` (valor exibido é o texto bruto; a consulta usa o `trim`).
- `OnRetryClicked`: incrementa `retryTrigger` (já incluído no `combine`).
- `OnResultClicked(id)` → `NavigateToDetail(id)`.
- `SearchResultMapper`: `ScheduleItem → SearchResultUi(id, colorHex, sticker, title, dateTimeText, typeText, calendarLabel, recurrenceText?)` com `core.ui.format`; o e-mail da conta vem do mesmo fluxo de `UserSettingsRepository` (combinar ou mapear no momento da exibição).

## UI
- `Scaffold` com `TopAppBar` contendo `OutlinedTextField`/`SearchBar`-like (usar `TextField` em `TopAppBar.title` para simplicidade) e `FocusRequester` + `LaunchedEffect(Unit)`; `KeyboardActions(onSearch = hideKeyboard)`.
- `SearchFilterChips`: `Row` com `FilterChip(selected, Modifier.semantics { role = RadioButton })`.
- `LazyColumn` de `SearchResultItem` (`key = id`), `SearchEmptyState` (hint/sem resultado/erro com botão).
- Previews: dica inicial; resultados; vazio; carregando; erro; filtro Eventos.
- Tags: `SEARCH_QUERY_INPUT`, `SEARCH_CLEAR_BUTTON`, `SEARCH_BACK_BUTTON`, `SEARCH_FILTER_CHIP_PREFIX`, `SEARCH_RESULT_ITEM_PREFIX`, `SEARCH_EMPTY_STATE`, `SEARCH_HINT`, `SEARCH_RETRY_BUTTON`.

## Navegação
`searchEntry(onBack, onOpenDetail)`; `AppNavHost` liga Home→Search e Search→Detail (empilha `ScheduleDetail`, mantendo a busca no back stack).

## Decisões de UseCase
Nenhum: o PDF tinha `SearchSchedulesUseCase`, mas ele só validava texto vazio e filtrava por tipo — ambos agora feitos no SQL/ViewModel. Sem regra compartilhada nem orquestração.

## Testes
- `SearchViewModelTest` (`StandardTestDispatcher`, `advanceTimeBy`): não consulta em texto vazio; debounce de 250 ms agrupa digitação rápida; filtro reconsulta sem esperar; `flatMapLatest` descarta resposta antiga; limpar zera resultados; restauração do `SavedStateHandle`; erro → estado `Error` e retry; clique → efeito.
- `SearchResultMapperTest`: recorrente exibe repetição; adesivo; rótulos de tipo/calendário.
- `SearchScreenRobolectricTest`: foco inicial; chips com `Role.RadioButton`/`selected`; digitar mostra resultados; vazio e dica; limpar; clique no resultado.
- `SearchIntegrationTest` (Room em memória): termos com acento/maiúsculas via repositório real.
