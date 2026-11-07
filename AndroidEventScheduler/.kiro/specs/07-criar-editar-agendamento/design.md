# 07 — Criar e editar agendamento — Design

## Pacotes
```
feature/upsert/
├── domain/
│   ├── usecase/SaveScheduleUseCase.kt
│   └── model/SaveScheduleError.kt                # BlankTitle, EndBeforeStart
├── presentation/
│   ├── state/    UpsertScheduleUiState.kt, ActiveInlinePicker.kt
│   ├── event/    UpsertScheduleUiEvent.kt
│   ├── effect/   UpsertScheduleUiEffect.kt
│   ├── viewmodel/UpsertScheduleViewModel.kt
│   ├── component/ ModeSegmentedPill.kt, TitleRow.kt, DateTimeSection.kt, InlineDatePicker.kt, InlineTimePicker.kt,
│   │              QuickChipsRow.kt, SettingRow.kt, ReminderOffsetRow.kt, CalendarSourceRow.kt
│   └── screen/   UpsertScheduleScreen.kt (+ UpsertScheduleContent)
├── navigation/UpsertEntry.kt                     # entry<UpsertSchedule>(onBack)
└── di/UpsertModule.kt
```

## Domínio — `SaveScheduleUseCase`
```kotlin
class SaveScheduleUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val scheduleNextAlarm: ScheduleNextAlarmUseCase,
) {
    suspend operator fun invoke(item: ScheduleItem): Result<ScheduleItem> = runCatching {
        val normalized = item.copy(title = item.title.trim())
        if (normalized.title.isBlank()) throw SaveScheduleError.BlankTitle
        if (endsBeforeStart(normalized)) throw SaveScheduleError.EndBeforeStart
        scheduleRepository.upsert(normalized)
        runCatching { scheduleNextAlarm(normalized) }   // R5.5: alarme não derruba a gravação (logar)
        normalized
    }
}
```
- `endsBeforeStart`: compara `LocalDateTime` de fim vs início (dia inteiro compara só datas). `ReminderOffset` e demais campos são confiados ao ViewModel.
- A geração do `id` fica no ViewModel (`idProvider: () -> String = { UUID.randomUUID().toString() }`, injetável) para manter o use case determinístico.
- **Por que UseCase:** orquestra repositório + alarme (2 fontes consistentes) e concentra a validação, reutilizada em testes isolados.

## Estado
```kotlin
enum class ActiveInlinePicker { NONE, START_DATE, START_TIME, END_DATE, END_TIME }
sealed class UpsertScheduleUiState {
    data object Loading : UpsertScheduleUiState()
    data class Content(
        val scheduleId: String? = null,
        val mode: ScheduleType = ScheduleType.REMINDER,
        val title: String = "", val isAllDay: Boolean = false,
        val startDate: LocalDate, val startTime: LocalTime, val endDate: LocalDate, val endTime: LocalTime,
        val activePicker: ActiveInlinePicker = ActiveInlinePicker.NONE,
        val reminderOffset: ReminderOffset, val calendarSource: CalendarSource,
        val recurrencePattern: RecurrencePattern = RecurrencePattern.None,
        val colorHex: Long = EventColorOption.LIGHT_BLUE.hex, val stickerEmoji: String? = null,   // usados pela spec 09
        val location: String = "", val notes: String = "",
        val isSaving: Boolean = false, val titleError: Boolean = false,
    ) : UpsertScheduleUiState()
}
sealed interface UpsertScheduleUiEvent {
    OnModeSelected(ScheduleType); OnTitleChanged(String); OnAllDayToggled(Boolean)
    OnPickerToggled(ActiveInlinePicker); OnDateSelected(target: ActiveInlinePicker, LocalDate)
    OnTimeChanged(target: ActiveInlinePicker, LocalTime)
    OnQuickDateChipClicked(daysOffset: Int); OnQuickTimeChipClicked(QuickTime)   // QuickTime: InOneHour, At(LocalTime)
    OnReminderOffsetSelected(ReminderOffset); OnCalendarSourceSelected(CalendarSource)
    OnLocationChanged(String); OnNotesChanged(String); OnSaveClicked; OnCancelClicked
}
sealed interface UpsertScheduleUiEffect { NavigateBack; ShowMessage(@StringRes Int) }
```
Eventos de recorrência/cor/emoji (specs 08/09) são acrescentados nelas.

## ViewModel
`UpsertScheduleViewModel(savedStateHandle/route args, scheduleRepository, userSettingsRepository, saveScheduleUseCase, clock, idProvider, dispatcher)`.
- `init`: edição → `scheduleRepository.getById(id)` → preenche; criação → `userSettingsRepository.current().defaultReminderOffset` + data da rota. Erro/nulo → efeito `ShowMessage` + `NavigateBack` (o estado permanece `Loading`). As alterações de campo usam `updateContent { it.copy(...) }`, helper **privado** do ViewModel (só aplica se o estado for `Content`).
- Regras de data/hora isoladas em um helper puro `ScheduleDraftTimeRules` (`feature.upsert.domain`): `withNewStart(draft, newStart)` preservando duração; `clampEnd`; `quickDate(daysOffset, today)`; `inOneHour(now)`; `mode switch`. Facilita TDD sem VM. (Não é UseCase: sem I/O e usado só aqui.)
- `OnSaveClicked`: monta `ScheduleItem` do `Content` (Lembrete: `endDate=startDate`, `endTime=null`, `isAllDay=false`, `location=""`, `calendarSource=MY_REMINDERS`), `isSaving=true`, chama o use case; `BlankTitle` → `titleError=true`; `EndBeforeStart` → mensagem; sucesso → `NavigateBack`.
- Uma edição de `title` limpa `titleError`.

## UI
- Lista rolável (`LazyColumn`) com `imePadding`; `TopAppBar` (X cancelar + título + "Salvar").
- `InlineDatePicker`: M3 `DatePicker(state, showModeToggle = false, title = null, headline = null)`. **Atenção:** o `DatePickerState` trabalha em milissegundos UTC; converter com `Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()` e na ida `date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()`.
- `InlineTimePicker`: M3 `TimePicker(state)` 24 h (`is24Hour = true`); mudança do estado emite `OnTimeChanged` (via `snapshotFlow` com `distinctUntilChanged`).
- `QuickChipsRow`: `FlowRow` de `AssistChip` com tag `UPSERT_QUICK_CHIP_PREFIX + id`.
- `AnimatedVisibility` nos seletores inline; `stateDescription = Expandido/Recolhido` nas linhas.
- Tags: `UPSERT_MODE_EVENT_TAB`, `UPSERT_MODE_REMINDER_TAB`, `UPSERT_TITLE_INPUT`, `UPSERT_ALL_DAY_SWITCH`, `UPSERT_DATE_SELECTOR_ROW`, `UPSERT_END_DATE_SELECTOR_ROW`, `UPSERT_INLINE_CALENDAR`, `UPSERT_TIME_SELECTOR_ROW`, `UPSERT_END_TIME_SELECTOR_ROW`, `UPSERT_INLINE_TIME_PICKER`, `UPSERT_QUICK_CHIP_PREFIX`, `UPSERT_REMINDER_ROW`, `UPSERT_CALENDAR_SOURCE_ROW`, `UPSERT_LOCATION_INPUT`, `UPSERT_NOTES_INPUT`, `UPSERT_RECURRENCE_ROW`, `UPSERT_SAVE_BUTTON`, `UPSERT_CANCEL_BUTTON`.
- `TitleRow` desta spec contém apenas o campo de título; a spec 09 acrescenta os botões de emoji e cor.
- Previews: lembrete novo; evento novo; evento dia inteiro; calendário aberto; hora aberta; erro de título; salvando; edição.

## Navegação
`upsertEntry(onBack)`: `entry<UpsertSchedule> { key -> UpsertScheduleScreen(scheduleId = key.scheduleId, initialDateEpochDay = key.initialDateEpochDay, onBack) }`; ViewModel via `koinViewModel { parametersOf(...) }`. Detalhe → "Editar" usa a mesma rota com `scheduleId`.

## Decisões de UseCase
- `SaveScheduleUseCase`: justificado (orquestra persistência + alarme; validação própria).
- Sem UseCase para carregar o item (passa-direto) nem para as regras de data (helper puro local).

## Testes
- `SaveScheduleUseCaseTest` (MockK): título em branco; fim antes do início; sucesso persiste e agenda; falha do alarme ainda retorna sucesso; falha do repositório retorna falha e não agenda; `trim` aplicado.
- `ScheduleDraftTimeRulesTest`: preservar duração; clamp do fim; chip "Daqui a 1 hora" cruzando meia-noite; troca de modo.
- `UpsertScheduleViewModelTest`: valores iniciais (padrão das configurações, data da rota); carregar edição; item inexistente → mensagem+voltar; salvar válido → `NavigateBack`; `BlankTitle` → `titleError`; duplo toque não salva duas vezes; um seletor por vez.
- `UpsertScheduleScreenRobolectricTest`: abas com `selected`; digitar título; abrir/fechar calendário e hora (um por vez); chips; dia inteiro oculta horas; salvar dispara evento; linha de repetição com descrição `Repetição: Não repetir`.
