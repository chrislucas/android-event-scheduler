# 05 — Home: calendário mensal e agenda do dia — Design

## Pacotes
```
feature/home/
├── domain/calendar/   MonthGridBuilder.kt            # puro: YearMonth → List<LocalDate> (42)
├── presentation/
│   ├── state/    HomeUiState.kt, CalendarDayCell.kt
│   ├── event/    HomeUiEvent.kt
│   ├── effect/   HomeUiEffect.kt
│   ├── viewmodel/ HomeViewModel.kt
│   ├── component/ MonthGridSection.kt, MonthPickerSheetContent.kt, DayScheduleList.kt, ScheduleListItem.kt, QuickAddBar.kt
│   └── screen/   HomeScreen.kt (HomeScreen + HomeContent)
├── navigation/   HomeEntry.kt
└── di/           HomeModule.kt
```
(`data/` só surge na spec 06 com preferências da gaveta.)

## Estado, eventos, efeitos
```kotlin
data class CalendarDayCell(val date: LocalDate, val isInVisibleMonth: Boolean, val isToday: Boolean,
                           val isSelected: Boolean, val indicatorColors: List<Long>, val scheduleCount: Int)
sealed class HomeUiState {
    data object Loading : HomeUiState()                       // somente na 1ª carga; trocar de mês mantém o Content anterior
    data class Content(
        val visibleMonth: YearMonth, val selectedDate: LocalDate,
        val cells: List<CalendarDayCell>, val daySchedules: List<ScheduleOccurrence>,
        val isMonthPickerVisible: Boolean = false, val monthPickerYear: Int = visibleMonth.year,
    ) : HomeUiState()
    data class Error(@StringRes val messageRes: Int) : HomeUiState()
}
sealed interface HomeUiEvent {
    data class OnDayClicked(val date: LocalDate); data object OnPreviousMonthClicked; data object OnNextMonthClicked
    data object OnMonthHeaderClicked; data object OnMonthPickerDismissed
    data class OnMonthPickerYearChanged(val delta: Int); data class OnMonthPicked(val month: YearMonth)
    data object OnSearchClicked; data object OnQuickAddClicked; data class OnScheduleClicked(val scheduleId: String)
}
sealed interface HomeUiEffect { data object NavigateToSearch; data class NavigateToUpsert(val initialDateEpochDay: Long); data class NavigateToDetail(val scheduleId: String) }
```
`HomeUiEffect.NavigateToSettings` é adicionado pela spec 06.

## ViewModel
```kotlin
class HomeViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val monthGridBuilder: MonthGridBuilder,
    private val clock: Clock,
    private val savedStateHandle: SavedStateHandle,
    private val dispatcher: CoroutineDispatcher,
)
```
- O `uiState` é **derivado**: os eventos só alteram fontes privadas (`visibleMonthFlow: MutableStateFlow<YearMonth>`, `selectedDateFlow: MutableStateFlow<LocalDate>`, `monthPickerFlow: MutableStateFlow<MonthPickerState?>`, espelhadas em `SavedStateHandle` quando relevante) e o `combine` abaixo produz o `Content`. Não há mutação direta do estado.
- Pipeline (em `init`): 
  ```kotlin
  visibleMonthFlow.flatMapLatest { month ->
      val grid = monthGridBuilder.build(month)
      scheduleRepository.observeOccurrences(grid.first(), grid.last()).map { grid to it }
  }.combine(selectedDateFlow) { (grid, occurrences), selected -> grid to (occurrences to selected) }
   .combine(monthPickerFlow) { gridData, picker -> buildContent(gridData, picker) }       // → HomeUiState.Content
   .flowOn(dispatcher)
   .catch { _uiState.value = HomeUiState.Error(R.string.error_generic) }
   .collect { _uiState.value = it }
  ```
- `buildContent`: para cada célula, ocorrências cujo intervalo `[startDate, endDate]` contém a data (multi-dia); `indicatorColors = take(3).map(colorHex)` após ordenar por hora; `daySchedules` = ocorrências do `selectedDate` ordenadas (dia inteiro primeiro, depois `startTime`).
- Este ponto é onde a spec 06 injeta o filtro de calendários (`.map { occurrences.filter { it.item.calendarSource in enabled } }`) — manter `buildContent` recebendo a lista já pronta.
- `OnDayClicked(date)` fora do mês visível → também `visibleMonthFlow.value = YearMonth.from(date)`.
- `OnMonthPicked`: regra R3.2 (clamp com `coerceAtMost(month.lengthOfMonth())`).
- Efeitos: `viewModelScope.launch { _uiEffect.send(...) }` no `Channel` do VM.

## MonthGridBuilder
```kotlin
class MonthGridBuilder(private val firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY) {
    fun build(month: YearMonth): List<LocalDate>   // sempre 42 datas consecutivas
}
```
`gridStart = month.atDay(1).with(TemporalAdjusters.previousOrSame(firstDayOfWeek))`.

## UI
- `HomeScreen` coleta `uiState`, efeitos (`CollectEffects`) e inclui `NotificationPermissionEffect()`; `HomeContent(uiState, onEvent)` stateless com `Scaffold` (topBar = título clicável + busca; bottomBar = `QuickAddBar`; `contentWindowInsets` corretos).
- `MonthGridSection`: `Column` de 6 `Row`s de 7 `DayCell`s (`Modifier.weight(1f)`, `aspectRatio`); gesto `pointerInput(Unit){ detectHorizontalDragGestures }` com limiar de 80dp → `OnPrevious/NextMonthClicked`; células com `semantics { contentDescription; selected }` e `clickable(role = Role.Button)`.
- `MonthPickerSheetContent` dentro de `ModalBottomSheet`; meses via `Month.getDisplayName(SHORT)` em maiúsculas (`JAN.`).
- `ScheduleListItem`: barra lateral colorida, `Text` do emoji, título, intervalo; `LazyColumn` com `key = occurrence.item.id + date`.
- Formatação de data/hora em `core.ui.format` (spec 01). Strings pt-BR em `strings.xml`.
- Tags: `HOME_MONTH_HEADER`, `HOME_SEARCH_BUTTON`, `HOME_QUICK_ADD_BAR`, `HOME_SCHEDULE_LIST`, `HOME_DAY_HEADER`, `HOME_CALENDAR_GRID`, `HOME_DAY_CELL_PREFIX` (+ epochDay), `HOME_MONTH_PICKER_SHEET`, `HOME_MONTH_OPTION_PREFIX`, `HOME_PREVIOUS_MONTH_BUTTON`, `HOME_NEXT_MONTH_BUTTON`, `HOME_SCHEDULE_ITEM_PREFIX`, `HOME_EMPTY_DAY`.
- Previews (tablet não se aplica; usar phone): `HomeContentPreview` com dados / vazio / carregando / erro; `MonthPickerSheetContentPreview`; `ScheduleListItemPreview` (com e sem adesivo, dia inteiro).

## Navegação
`homeEntry(onNavigateToSearch, onNavigateToUpsert: (Long) -> Unit, onNavigateToDetail: (String) -> Unit, onNavigateToSettings)`; `AppNavHost` conecta aos `backStack.add(...)`.

## Decisões de UseCase
Nenhum: o ViewModel injeta `ScheduleRepository` (passa-direto) e a montagem da grade é um helper puro (`MonthGridBuilder`), não regra de negócio compartilhada. O filtro por calendário (spec 06) também é lógica de apresentação do próprio Home.

## Testes
- `MonthGridBuilderTest`: nov/2026 (começa domingo? validar), mês começando no sábado, fevereiro bissexto, 42 células sempre, `firstDayOfWeek` configurável.
- `HomeViewModelTest` (repositório fake com `MutableStateFlow`): estado inicial = hoje; dia clicado de mês vizinho muda o mês; recorrente aparece nas ocorrências; ≤ 3 indicadores; multi-dia marca dias intermediários; atualização reativa ao inserir; `OnMonthPicked` clamp (31 → 30); restauração via `SavedStateHandle`; efeitos de navegação; erro do repositório → `HomeUiState.Error`.
- `HomeScreenRobolectricTest`: título clicável abre o sheet; escolha de mês fecha e atualiza título; item da lista dispara `OnScheduleClicked`; estado vazio; `contentDescription` das células; botão de busca; barra de adição rápida.
