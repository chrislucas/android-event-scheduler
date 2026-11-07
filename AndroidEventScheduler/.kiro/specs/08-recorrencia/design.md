# 08 — Recorrência (diálogo de repetição) — Design

## Mudanças em `feature.upsert`
```
presentation/component/RecurrencePickerDialog.kt     (novo)
presentation/component/RecurrenceDialogState.kt      (novo: estado local do diálogo, testável sem Compose)
presentation/{state,event,screen,viewmodel}           (alterados)
```

## Estado local do diálogo (puro)
```kotlin
enum class RecurrenceKind { NONE, DAILY, WEEKLY }
data class RecurrenceDraft(val kind: RecurrenceKind, val selectedDays: Set<DayOfWeek>, val countText: String) {
    fun withKind(kind: RecurrenceKind) = copy(kind = kind)
    fun toggleDay(day: DayOfWeek): RecurrenceDraft   // não remove o último
    fun withCountText(raw: String) = copy(countText = raw.filter(Char::isDigit).take(MAX_COUNT_DIGITS))
    fun toPattern(): RecurrencePattern               // regras R3.1
    companion object {
        const val MAX_COUNT_DIGITS = 4
        fun from(pattern: RecurrencePattern, startDate: LocalDate): RecurrenceDraft   // R1.2 e R2.3
    }
}
```
`from(None)` → `NONE`, `selectedDays = setOf(startDate.dayOfWeek)`, `countText = ""`. `from(Daily)`/`from(Weekly)` preenchem limite como texto. Ao trocar de `NONE` para `WEEKLY` mantém `selectedDays` já inicializado com o dia da data inicial.

## Estado/eventos da tela (acréscimos)
```kotlin
// UpsertScheduleUiState.Content
val isRecurrenceDialogVisible: Boolean = false
// UpsertScheduleUiEvent
data object OnRecurrenceRowClicked; data object OnRecurrenceDialogDismissed
data class OnRecurrencePatternConfirmed(val pattern: RecurrencePattern)
```
Reducers triviais no ViewModel (fecha o diálogo e grava `recurrencePattern`). **Sem UseCase.**

## Composable
```kotlin
@Composable
fun RecurrencePickerDialog(initialPattern: RecurrencePattern, startDate: LocalDate,
                           onConfirm: (RecurrencePattern) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier)
```
- `var draft by rememberSaveable(initialPattern, startDate, stateSaver = …) { mutableStateOf(RecurrenceDraft.from(...)) }` (Saver simples via `listSaver`).
- Container: `Dialog` → `Surface(shape = 24.dp, color = SurfaceElevated)`; opções dentro de `Column(Modifier.selectableGroup())`; cada `RecurrenceRadioRow` com `Modifier.selectable(role = Role.RadioButton)` e `RadioButton(onClick = null)`.
- `WeeklyDaySelectorRow`: 7 itens `Box` 40dp visuais com `minimumInteractiveComponentSize()` (alvo 48dp), `toggleable(role = Role.Checkbox)`, rótulos de dia em `strings.xml` (curto + completo).
- Campo de número: `OutlinedTextField(keyboardOptions = Number)`, `contentDescription = "Limite de repetições em número de vezes"`.
- Ações: `TextButton` Cancelar / Concluir.
- Previews: não repetir; diário; semanal ter+qui com 5000; semanal com 1 dia (último dia bloqueado).

## Integração na tela
Em `UpsertScheduleContent`, abaixo da linha de repetição: `if (content.isRecurrenceDialogVisible) RecurrencePickerDialog(content.recurrencePattern, content.startDate, onConfirm = { onEvent(OnRecurrencePatternConfirmed(it)) }, onDismiss = { onEvent(OnRecurrenceDialogDismissed) })`. Rótulo da linha: `state.recurrencePattern.toDisplayLabel(resources)` (spec 01).

## Decisões de UseCase
Nenhum: a lógica do diálogo (`RecurrenceDraft`) é local e pura; o cálculo de ocorrências já pertence ao `RecurrenceEngine`.

## Testes
- `RecurrenceDraftTest`: `from` para cada padrão; `toggleDay` não remove o último; filtro de dígitos e limite de 4; `toPattern` com contagem vazia/zero/5000; padrão inicial usa o dia da data inicial.
- `UpsertScheduleViewModelTest` (acréscimo): confirmar atualiza padrão e fecha; cancelar mantém padrão.
- `RecurrencePickerDialogRobolectricTest` (fluxo do PDF, adaptado): abrir pela linha da tela (`Repetição: Não repetir`) → "Toda semana" selecionada → dia inicial marcado → marcar terça → digitar `5000` → Concluir → `Repetição: Toda semana em ter., qui. (5000 vezes)` (iniciando numa quinta); fluxo "Diariamente" sem limite devolve `Daily(null)`; Cancelar não altera; semântica dos rádios e dos dias (`assertIsSelected`, `assertContentDescriptionEquals`).
- `RecurrenceSaveIntegrationTest` (Robolectric + Room em memória): salvar semanal e checar `ScheduleRepository.observeOccurrences` + alarme agendado para a 1ª ocorrência futura (liga as specs 01/04/07).
