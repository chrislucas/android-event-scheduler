# 10 — Detalhes do agendamento — Design

## Pré-requisito em `core.schedule`
Adicionar `fun observeById(id: String): Flow<ScheduleItem?>` ao `ScheduleRepository` (spec 01 já o declara; ver R4.1 lá) — necessário para refletir edição/exclusão (R2.3).

## Pacotes
```
feature/detail/
├── presentation/
│   ├── state/    ScheduleDetailUiState.kt
│   ├── event/    ScheduleDetailUiEvent.kt
│   ├── effect/   ScheduleDetailUiEffect.kt
│   ├── viewmodel/ScheduleDetailViewModel.kt
│   ├── mapper/   ScheduleDetailMapper.kt     # ScheduleItem → ScheduleDetailUi (strings prontas)
│   ├── component/ DetailInfoRow.kt, ScheduleActionBar.kt
│   └── screen/   ScheduleDetailScreen.kt (+ ScheduleDetailContent)
├── navigation/DetailEntry.kt                 # entry<ScheduleDetail>(onBack, onEdit)
└── di/DetailModule.kt
```

## Estado
```kotlin
sealed class ScheduleDetailUiState {
    data object Loading : ScheduleDetailUiState()
    data class Content(val schedule: ScheduleDetailUi) : ScheduleDetailUiState()   // UI model já formatado; specs 11/12 acrescentam campos
    data object NotFound : ScheduleDetailUiState()
}
data class ScheduleDetailUi(val id: String, val title: String, val stickerEmoji: String?, val colorHex: Long,
    val dateRangeText: String, val calendarLabel: String, val calendarColorHex: Long, val reminderText: String,
    val recurrenceText: String, val location: String?, val notes: String?)
sealed interface ScheduleDetailUiEvent { data object OnBackClicked; data object OnEditClicked }   // 11/12 acrescentam
sealed interface ScheduleDetailUiEffect { data object NavigateBack; data class NavigateToEdit(val scheduleId: String); data class ShowMessage(@StringRes val messageRes: Int) }
```
`ScheduleDetailUi` evita formatar strings no Composable e facilita teste do mapper.

## ViewModel
```kotlin
class ScheduleDetailViewModel(
    private val scheduleId: String,
    private val scheduleRepository: ScheduleRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val mapper: ScheduleDetailMapper,
    private val dispatcher: CoroutineDispatcher,
)
```
`init`: `combine(scheduleRepository.observeById(scheduleId), userSettingsRepository.observe().map { it.activeAccountEmail })` → se item `null`: se nunca carregado ⇒ estado `NotFound` + `ShowMessage(not_found)` + `NavigateBack`; se já havia item (foi excluído) ⇒ `NavigateBack` sem mensagem de erro. Usar flag interna `hasLoadedOnce`. `scheduleId` vem da rota via `parametersOf`.

## Mapper
`ScheduleDetailMapper(resources/StringProvider, zone-less formatters pt-BR)`; usa `core.ui.format` (spec 01) para recorrência/alerta/data. Formatadores: `DateTimeFormatter.ofPattern("EEE., d 'de' MMM.", Locale("pt","BR"))` — validar abreviações reais do JDK/desugar (`qui.`, `nov.`); se divergirem, mapear manualmente e cobrir com teste.

## UI
- `Scaffold(topBar = back, bottomBar = ScheduleActionBar)`; conteúdo rolável em `Column` com `DetailInfoRow(icon, text, tag)`; cartão com faixa lateral na cor do item.
- `ScheduleActionBar(actions: List<ScheduleAction>)` — `data class ScheduleAction(@StringRes label, icon, tag, onClick, enabled)`. O mapeamento ação→botão é uma lista montada em `ScheduleDetailContent`; as specs 11 e 12 só acrescentam itens à lista (ordem final: Duplicar, Editar, Compartilhar, Excluir).
- Previews: evento multi-dia; lembrete; dia inteiro; sem local/notas; carregando; não encontrado; item longo (elipse/quebra).
- Tags: `DETAIL_TITLE`, `DETAIL_DATE_RANGE`, `DETAIL_ACCOUNT_EMAIL`, `DETAIL_REMINDER_LABEL`, `DETAIL_RECURRENCE_LABEL`, `DETAIL_LOCATION`, `DETAIL_NOTES`, `DETAIL_BACK_BUTTON`, `DETAIL_EDIT_BUTTON`.

## Navegação
`detailEntry(onBack, onEdit: (String) -> Unit)`: `entry<ScheduleDetail> { key -> ScheduleDetailScreen(key.scheduleId, …) }`; `AppNavHost` liga `onEdit` a `backStack.add(UpsertSchedule(scheduleId = it))`.

## Decisões de UseCase
Nenhum: leitura reativa passa-direto + formatação (mapper de apresentação).

## Testes
- `ScheduleDetailMapperTest`: todos os formatos de intervalo (R1.2), rótulos de calendário (e-mail), omissão de local/notas vazios, recorrência e alerta.
- `ScheduleDetailViewModelTest`: carrega e emite UI model; item inexistente → mensagem + voltar; item removido depois de carregado → voltar sem erro; edição externa atualiza a UI; `OnEditClicked` → `NavigateToEdit(id)`; e-mail alterado nas configurações atualiza o rótulo.
- `ScheduleDetailScreenRobolectricTest`: textos e `contentDescription` por tag; botão Editar dispara evento; local/notas ausentes não existem na árvore (`assertDoesNotExist`); carregando.
