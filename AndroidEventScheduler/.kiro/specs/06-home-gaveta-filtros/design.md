# 06 — Home: gaveta lateral e filtros de calendário — Design

## Mudanças em `feature.home`
```
domain/repository/HomePreferencesRepository.kt
data/repository/HomePreferencesRepositoryImpl.kt   (internal; usa DataStore<Preferences> do core.datastore)
presentation/component/CalendarDrawerSheet.kt      (novo)
presentation/{state,event,effect,viewmodel,screen}  (alterados)
```

## Domínio / dados
```kotlin
interface HomePreferencesRepository {
    fun observeEnabledCalendars(): Flow<Set<CalendarSource>>
    suspend fun setCalendarEnabled(source: CalendarSource, enabled: Boolean)
}
```
- Chave `enabled_calendar_ids` (`stringSetPreferencesKey`); ausente → `CalendarSource.entries.toSet()`; mapeia ids → enum ignorando desconhecidos (R4.2). `setCalendarEnabled` usa `dataStore.edit` lendo o conjunto atual dentro da transação (evita corrida entre toques rápidos).
- Spec 09 acrescentará `observeDaySticker/setDaySticker` nesta mesma interface.

## Estado / eventos / efeitos (acréscimos)
```kotlin
// HomeUiState.Content (novos campos)
val isDrawerOpen: Boolean = false
val enabledSources: Set<CalendarSource> = CalendarSource.entries.toSet()
val personalAccountLabel: String = ""
// HomeUiEvent
data object OnMenuClicked; data object OnDrawerDismissed
data class OnCalendarSourceToggled(val source: CalendarSource); data object OnSettingsClicked
// HomeUiEffect
data object NavigateToSettings
```
- `isDrawerOpen` vem de uma fonte privada `drawerOpenFlow` (espelhada no `SavedStateHandle`, R1.3) combinada ao `Content`, como as demais fontes da spec 05. O `ModalNavigationDrawer` usa `rememberDrawerState` sincronizado com `state.isDrawerOpen` via `LaunchedEffect(isDrawerOpen) { if (open) drawerState.open() else drawerState.close() }` e `snapshotFlow { drawerState.isClosed }` → `OnDrawerDismissed` (para deslize/scrim). Evitar laço: só emitir quando `state.isDrawerOpen && drawerState.isClosed`.
- `OnCalendarSourceToggled` → `homePreferencesRepository.setCalendarEnabled(source, source !in current)` (a UI atualiza pelo fluxo, não por set local → fonte única).
- `OnSettingsClicked` → `isDrawerOpen = false` + `_uiEffect.send(NavigateToSettings)`.

## ViewModel (alteração do pipeline da spec 05)
```kotlin
combine(
    occurrencesFlow,                                  // grade + ocorrências (spec 05)
    selectedDateFlow,
    homePreferencesRepository.observeEnabledCalendars(),
    userSettingsRepository.observe().map { it.activeAccountEmail }.distinctUntilChanged(),
) { (grid, all), selected, enabled, email ->
    val visible = all.filter { it.item.calendarSource in enabled }
    buildState(grid, visible, selected).copy(enabledSources = enabled, personalAccountLabel = email)
}
```
Novo parâmetro do construtor: `HomePreferencesRepository`, `UserSettingsRepository`.

## UI
`CalendarDrawerSheet(enabledSources, personalAccountLabel, onToggle, onSettingsClick)` dentro de `ModalDrawerSheet`; cada linha `Row(Modifier.toggleable(value, role = Role.Checkbox, onValueChange))` com `Checkbox(onCheckedChange = null)`, bolinha na cor `CalendarSource.colorHex`, texto; `semantics { stateDescription = … }`. Rótulos: `FAMILY → "Family"`, `MY_REMINDERS → "Meus lembretes"`, `PERSONAL_ACCOUNT → email`, via mapper de apresentação `CalendarSource.label(personalAccountLabel, resources)` em `core.ui.format` (reutilizado pelo Detail).
Previews: todos visíveis; um oculto; todos ocultos; e-mail longo (elipse).

## Decisões de UseCase
Nenhum: filtragem é apresentação da Home; persistência é passa-direto do repositório próprio da Home.

## Testes
- `HomePreferencesRepositoryImplTest`: padrão = todos; round-trip; id desconhecido ignorado; dois toggles consecutivos preservam ambos.
- `HomeViewModelTest` (acrescentar, `given/when/then`): ocultar `FAMILY` remove itens do mês e do dia; reexibir restaura; todos ocultos → listas vazias sem erro; e-mail atualizado propaga; `OnSettingsClicked` fecha e emite `NavigateToSettings`; estado do drawer restaurado.
- `CalendarHomeDrawerRobolectricTest`: abrir pelo menu; checkbox com `Role.Checkbox`/`stateDescription`; alternar dispara evento; botão Configurações.
