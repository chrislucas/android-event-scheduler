# 18 — Toque duplo numa data cria evento ou lembrete — Design

## Visão geral
A mudança fica toda em `feature.home`, mais uma guarda pequena no `AppNavHost`. A criação já existe (spec 07) e já recebe a data pela rota `UpsertSchedule(initialDateEpochDay)`. A Home já emite `HomeUiEffect.NavigateToUpsert(initialDateEpochDay)` para a adição rápida. Esta spec só acrescenta o gesto que dispara esse mesmo efeito.

```
feature/home/
├── presentation/component/DayDoubleTapDetector.kt   (novo: regra pura do toque duplo)
├── presentation/component/MonthGridSection.kt       (alterado: observa os toques e expõe a ação de acessibilidade)
├── presentation/component/DayScheduleList.kt        (alterado: dica no estado vazio)
├── presentation/event/HomeUiEvent.kt                (alterado: família DayCellEvent)
├── presentation/viewmodel/HomeViewModel.kt          (alterado: onDayCellEvent)
└── presentation/screen/HomeScreen.kt                (alterado: repassa o novo callback)
app/AppNavHost.kt                                    (alterado: guarda contra criação duplicada)
```
Nenhuma rota, módulo Koin, permissão ou dependência nova.

## Decisões

| Decisão | Escolha | Alternativa rejeitada e motivo |
|---|---|---|
| Como escolher Evento × Lembrete | Abrir a criação; o usuário escolhe nas **abas existentes** (padrão Lembrete, como na adição rápida) | *Bottom sheet* "Evento / Lembrete" antes de abrir: um toque a mais e uma segunda forma de escolher o que as abas já oferecem. Passar o modo pela rota exigiria mudar `UpsertSchedule` e o ViewModel da 07 sem ganho |
| Seleção no primeiro toque | **Imediata**: o toque simples continua no `clickable` | `combinedClickable(onDoubleClick)` / `detectTapGestures(onDoubleTap)`: os dois **atrasam** o `onClick` até o intervalo de toque duplo acabar (~300 ms), o que piora a seleção do dia, o gesto mais comum da tela |
| Onde fica a regra | `DayDoubleTapDetector`, classe pura na apresentação da Home, com o tempo recebido por parâmetro | Contar toques no ViewModel com relógio: o intervalo é de UI (`ViewConfiguration`), e o ViewModel passaria a depender de tempo de toque |
| Origem do tempo do toque | `PointerInputChange.uptimeMillis` do evento de soltar, observado sem consumir | `SystemClock.uptimeMillis()` no `onClick`: não avança com o relógio de teste do Compose, o que deixa o teste não determinístico |
| TalkBack | Ação personalizada na célula (`customActions`) | Depender do gesto: com TalkBack o toque duplo é "ativar", e a grade nunca vê dois toques |
| Toque duplo × navegação pendente | Guarda no `AppNavHost`: só empilha `UpsertSchedule` se o topo ainda não for `UpsertSchedule` | Debounce no ViewModel: não cobre a adição rápida nem toques durante a animação de transição |

### Decisões de UseCase
Nenhum: não há regra de negócio nem orquestração; o gesto só reaproveita o efeito de navegação existente.

## Contratos

### Regra pura
```kotlin
/** Não é thread-safe; uma instância por grade (remember). */
internal class DayDoubleTapDetector(private val timeoutMillis: Long) {
    private var lastDate: LocalDate? = null
    private var lastUpMillis: Long = 0L

    /** Devolve `true` quando [date] completa um toque duplo; depois disso a contagem recomeça (R2.4). */
    fun onTap(date: LocalDate, upMillis: Long): Boolean {
        val isDouble = date == lastDate && upMillis - lastUpMillis in 0..timeoutMillis
        if (isDouble) { lastDate = null } else { lastDate = date; lastUpMillis = upMillis }
        return isDouble
    }
}
```

### Eventos (família nova, steering `arquitetura.md`, item UiEvent)
O `OnDayClicked` sai do topo do `HomeUiEvent` e entra na família `DayCellEvent`, **com o mesmo nome**, então os call sites e os testes que o usam continuam iguais.
```kotlin
sealed interface HomeUiEvent {
    /** Toques na célula do dia: seleção (spec 05) e toque duplo/ação de acessibilidade para criar (spec 18). */
    sealed interface DayCellEvent : HomeUiEvent
    data class OnDayClicked(val date: LocalDate) : DayCellEvent
    data class OnDayDoubleTapped(val date: LocalDate) : DayCellEvent
    // ... demais eventos e famílias inalterados
}
```

### ViewModel
```kotlin
is HomeUiEvent.DayCellEvent -> onDayCellEvent(event)

private fun onDayCellEvent(event: HomeUiEvent.DayCellEvent) {
    when (event) {
        is HomeUiEvent.OnDayClicked -> selectDate(event.date)
        is HomeUiEvent.OnDayDoubleTapped -> {
            selectDate(event.date)                                   // R3.1: muda o mês se for vizinho
            sendEffect(HomeUiEffect.NavigateToUpsert(event.date.toEpochDay()))
        }
    }
}
```
`selectDate` já trata dia de mês vizinho (spec 05). Não há estado novo e nada vai para o `SavedStateHandle`.

### Grade (`MonthGridSection`)
- O `clickable(role = Role.Button) { onDayClick(date) }` da célula fica como está: seleção imediata, ripple e teclado (R2.1, R4.2).
- Um `Modifier.pointerInput(date)` **passivo**, aplicado antes do `clickable`, observa o evento de soltar sem consumi-lo (`awaitEachGesture { … waitForUpOrCancellation() }`, `PointerEventPass.Initial`). Para cada toque completo cujo deslocamento entre tocar e soltar seja menor que `LocalViewConfiguration.current.touchSlop` (R2.5: o deslize de troca de mês, tratado por um `pointerInput` da grade, não pode virar toque), chama `detector.onTap(date, up.uptimeMillis)` e, se `true`, `onDayDoubleTap(date)`. O detector é um só para a grade inteira (`remember(timeout) { DayDoubleTapDetector(timeout) }`, com `timeout = LocalViewConfiguration.current.doubleTapTimeoutMillis`), para que toques em dias diferentes zerem a contagem (R2.2). Cancelamento (arrastar ou deslizar para trocar de mês) não conta como toque.
- Semântica: dentro do `semantics {}` da célula, `customActions = listOf(CustomAccessibilityAction(label = stringResource(R.string.home_day_add_action, dayLabel)) { onDayDoubleTap(date); true })`. `dayLabel` usa o mesmo formatador do `contentDescription` (`5 de novembro`).
- `MonthGridSection` ganha o parâmetro `onDayDoubleTap: (LocalDate) -> Unit`. Previews e call sites mudam **no mesmo commit** (regra "todo commit compila").

### Estado vazio
`DayScheduleList`: abaixo do texto `home_empty_day`, um texto secundário `home_empty_day_hint` com tag `HOME_EMPTY_DAY_HINT` (R5.1).

### `AppNavHost`
```kotlin
// Toque duplo / adição rápida (spec 18): não empilhar duas criações (R6.1)
onNavigateToUpsert = { dayEpoch ->
    if (backStack.lastOrNull() !is UpsertSchedule) backStack.add(UpsertSchedule(initialDateEpochDay = dayEpoch))
},
```

## Strings e tags
- `home_day_add_action` = `Adicionar agendamento em %1$s`
- `home_empty_day_hint` = `Toque duas vezes no dia para adicionar`
- `SemanticsTags.HOME_EMPTY_DAY_HINT`

## Testes
- `DayDoubleTapDetectorTest` (JVM, TDD):
  - mesmo dia dentro do intervalo → `true`;
  - fora do intervalo → `false`;
  - exatamente no limite → `true`;
  - dias diferentes → `false`;
  - triplo → `true` uma vez só (`false`, `true`, `false`);
  - quádruplo → dois duplos;
  - tempo que volta para trás → `false`.
- `HomeViewModelTest` (acréscimos):
  - `OnDayDoubleTapped(hoje)` → seleciona o dia e emite `NavigateToUpsert(hoje.toEpochDay())`;
  - dia de mês vizinho → muda o mês visível e emite o efeito com essa data;
  - `OnDayClicked` não emite efeito.
- `HomeScreenRobolectricTest` (acréscimos, `-w411dp-h891dp`):
  - `performTouchInput { doubleClick() }` numa célula → callback de criação com a data;
  - `performClick()` uma vez → só seleciona (`assertIsSelected` imediato, sem `mainClock.advanceTimeBy`) e não chama a criação;
  - dois cliques em células diferentes → nenhuma criação;
  - dois deslizes horizontais curtos (`swipeLeft`/`swipeRight` maiores que o `touchSlop`) começando e terminando na mesma célula → nenhuma criação (R2.5);
  - a ação personalizada (`performCustomAccessibilityActionWithLabel("Adicionar agendamento em …")`) → criação com a data;
  - o estado vazio mostra `HOME_EMPTY_DAY_HINT`.
- Ponta a ponta `DoubleTapDayCreatesScheduleRobolectricTest` (`MainActivity`, grafo real, `-w411dp-h2400dp`):
  1. Splash → Home → toque duplo no dia +3 → a tela de criação abre com essa data → trocar para **Evento** → título → Salvar.
  2. Volta para a Home com o dia +3 selecionado e o item na agenda. O Room tem um item `EVENT` com `startDate` = dia +3.
  3. Variação: toque duplo → Cancelar → nada gravado e o dia continua selecionado.
- Guarda do `AppNavHost`, no mesmo ponta a ponta: toque duplo e, antes de drenar o looper, clique na adição rápida (dois `NavigateToUpsert` seguidos). Ao voltar **uma vez** (`activity.onBackPressedDispatcher.onBackPressed()`, sem Espresso), a Home deve aparecer, o que prova que só um `UpsertSchedule` foi empilhado.
