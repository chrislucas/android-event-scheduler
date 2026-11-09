# Toque duplo na grade sem atrasar o toque simples (spec 18)

Decisão: por que a grade mensal da Home usa um `DayDoubleTapDetector` próprio, alimentado por um `pointerInput` passivo, em vez de `combinedClickable(onDoubleClick)` ou `detectTapGestures(onDoubleTap)`.

## 1. O problema

A spec 18 quer que **tocar duas vezes num dia** abra a criação de agendamento. O toque simples no dia, o gesto mais comum da tela, continua só selecionando o dia e **não pode ganhar atraso** (R2.1).

Os dois gestos competem pelo mesmo primeiro toque:

- Depois do primeiro toque, o app não sabe se é um toque simples ou o início de um toque duplo.
- Para saber, é preciso esperar o intervalo de toque duplo do sistema (`ViewConfiguration.doubleTapTimeoutMillis`, 300 ms no Android).

## 2. Por que a solução tradicional não serve

As duas APIs prontas do Compose resolvem a ambiguidade **esperando**:

| API | O que faz com o primeiro toque |
|---|---|
| `Modifier.combinedClickable(onClick, onDoubleClick)` | Se há `onDoubleClick`, o `onClick` só dispara depois que o intervalo de toque duplo acaba sem um segundo toque |
| `PointerInputScope.detectTapGestures(onTap, onDoubleTap)` | Com `onDoubleTap` definido, o `onTap` espera o intervalo antes de disparar |

Efeito na Home: o usuário toca num dia e a seleção (anel no dia, cabeçalho e agenda abaixo) só muda ~300 ms depois. Em toda seleção de dia, para um atalho que a maioria dos toques não usa. Dá a sensação de tela lenta, e o requisito R2.1 proíbe isso.

Esse atraso vem do contrato dessas APIs, não de um bug: o `onClick` só pode ser decidido depois do intervalo. Não há parâmetro que o desligue.

**Medido neste projeto** (Compose `ui-test` 1.9.5 no Robolectric, relógio de teste com `autoAdvance = false`): um toque único numa caixa de 48 dp, avançando o relógio de 10 em 10 ms até o callback do toque simples disparar.

| Modificador | `doubleTapTimeoutMillis` | Toque simples dispara após |
|---|---|---|
| `clickable(onClick)`, o que a grade usa | 300 ms | **0 ms** (no próprio quadro) |
| `combinedClickable(onClick, onDoubleClick)` | 300 ms | **300 ms** |
| `pointerInput { detectTapGestures(onTap, onDoubleTap) }` | 300 ms | **300 ms** |

A sonda foi um teste temporário e não ficou no repositório: medir o comportamento do framework não é responsabilidade do app. O que fica coberto pelos testes do projeto é a nossa garantia, ou seja, a seleção logo após um único clique, sem avançar o relógio.

Há ainda dois problemas que as APIs prontas não resolvem sozinhas:

1. **Deslizar para trocar de mês** (spec 05, R1.5) não pode contar como toque, mesmo que o dedo termine dentro de uma célula (R2.5).
2. **Toque duplo num dia de mês vizinho** (R3.1): o primeiro toque troca o mês, a grade é redesenhada e a mesma posição passa a mostrar **outra data**. Um detector chaveado pela data nunca vê o par.

## 3. A solução adotada

Separar **observar** de **decidir**:

1. O `clickable` da célula fica como está: o toque simples seleciona o dia na hora, com ripple e teclado.
2. Um `Modifier.pointerInput` **passivo**, antes do `clickable`, observa cada toque completo no passe `PointerEventPass.Initial` **sem consumir** o evento. O `clickable` continua recebendo tudo e dispara sem espera.
3. Cada toque completo vira uma chamada ao `DayDoubleTapDetector`, uma classe pura que recebe o instante do evento de soltar (`PointerInputChange.uptimeMillis`) e a posição da célula, e responde se aquele toque completa um duplo.
4. Só conta como toque o gesto cujo deslocamento entre tocar e soltar é menor que o `touchSlop` do sistema, e que não foi cancelado. Deslizes e arrastos não contam.
5. O detector é um só para a grade (toques em dias diferentes zeram a contagem) e é chaveado pela **posição da célula (0..41)**, não pela data. A grade guarda a data do **primeiro** toque do par e é essa data que vai para a criação. Assim o duplo num dia de mês vizinho funciona. A troca de mês por deslize zera o detector.
6. Três toques seguidos abrem a criação uma vez; depois do duplo a contagem recomeça.
7. Com TalkBack, o toque duplo é "ativar" e nunca chega à grade como dois toques. Por isso cada célula também expõe uma **ação de acessibilidade personalizada** (`Adicionar agendamento em 5 de novembro`) que dispara o mesmo evento.

O evento de soltar traz o horário do toque (`uptimeMillis`), então a regra não lê relógio nenhum.

## 4. Por que é melhor

| Critério | `combinedClickable` / `detectTapGestures` | Detector próprio + `pointerInput` passivo |
|---|---|---|
| Atraso do toque simples | ~300 ms em toda seleção | **Nenhum**: o `clickable` é intocado |
| Ripple, teclado, TalkBack ("ativar") | Reimplementados ou alterados pela API | Inalterados |
| Deslize de troca de mês | Precisa de tratamento à parte | Filtrado por `touchSlop` e cancelamento |
| Dia de mês vizinho | Não trata (a célula muda sob o dedo) | Chave por posição + data do 1º toque |
| Testabilidade | Depende do relógio do sistema | Regra pura com tempo injetado, testada na JVM (`DayDoubleTapDetectorTest`) |
| Custo | Zero código | ~40 linhas de regra + ~25 de gesto |

O preço é código próprio a manter. Ele é pequeno, isolado em `feature.home` e coberto por testes (regra pura, tela e ponta a ponta com a `MainActivity`).

## 5. O que não foi verificado

- A sensação do gesto com o intervalo real do sistema, em aparelho.
- TalkBack lendo e executando a ação personalizada.
- O atraso das APIs prontas foi medido no Robolectric (seção 2), não em aparelho. Em aparelho o valor é o `doubleTapTimeout` do sistema (300 ms por padrão; o usuário pode mudá-lo nas opções de acessibilidade).

## Referências

- `.kiro/specs/18-toque-duplo-criar-agendamento/` (requisitos R1–R6, design e tarefas)
- `feature/home/presentation/component/DayDoubleTapDetector.kt` e `MonthGridSection.kt`
