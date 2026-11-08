# Proposta de decisão: adotar uma `MviViewModel` abstrata?

> **Status:** proposta, aguardando decisão.
> **Contexto do projeto:** RememberMe (AndroidEventScheduler), arquitetura em `.kiro/steering/arquitetura.md` §5.
> **Decisão vigente hoje:** *não* há ViewModel base — cada ViewModel estende `androidx.lifecycle.ViewModel` e declara explicitamente `StateFlow` + `Channel`.
> **Objetivo deste documento:** explicar, com código, o que mudaria se adotássemos uma classe base `MviViewModel<S, E, F>`, como migrar o que já existe, e dar critérios objetivos para decidir.

---

## Sumário

1. [O problema que uma classe base tenta resolver](#1-o-problema-que-uma-classe-base-tenta-resolver)
2. [Como é hoje (padrão explícito)](#2-como-é-hoje-padrão-explícito)
3. [A proposta: `MviViewModel<S, E, F>`](#3-a-proposta-mviviewmodels-e-f)
4. [Migração passo a passo — feature de exemplo: Splash](#4-migração-passo-a-passo--feature-de-exemplo-splash)
5. [O caso difícil: formulário com `sealed class` de estado (Configurações)](#5-o-caso-difícil-formulário-com-sealed-class-de-estado-configurações)
6. [Impacto nos testes](#6-impacto-nos-testes)
7. [Vantagens e desvantagens](#7-vantagens-e-desvantagens)
8. [Alternativas intermediárias](#8-alternativas-intermediárias)
9. [Matriz de decisão](#9-matriz-de-decisão)
10. [Plano de migração (se aprovada)](#10-plano-de-migração-se-aprovada)
11. [Recomendação e perguntas para decidir](#11-recomendação-e-perguntas-para-decidir)
12. [Glossário](#12-glossário)

---

## 1. O problema que uma classe base tenta resolver

Toda tela do app segue o trio **State / Event / Effect**:

| Peça | Tipo | Como chega à UI | Exemplo |
|---|---|---|---|
| **State** — o que a tela mostra agora | `sealed class XxxUiState` | `StateFlow` (sempre tem um valor; a UI recompõe quando muda) | `Loading`, `Content(items)` |
| **Event** — o que o usuário fez | `sealed interface XxxUiEvent` | a UI chama `onEvent(event)` | `OnSaveClicked` |
| **Effect** — algo que acontece **uma vez** | `sealed interface XxxUiEffect` | `Channel` → `Flow` (entregue exatamente uma vez) | `NavigateBack`, `ShowMessage` |

Para isso, **todo** ViewModel repete as mesmas ~8 linhas:

```kotlin
private val _uiState = MutableStateFlow<XxxUiState>(XxxUiState.Loading)
val uiState: StateFlow<XxxUiState> = _uiState.asStateFlow()

private val _uiEffect = Channel<XxxUiEffect>(Channel.BUFFERED)
val uiEffect: Flow<XxxUiEffect> = _uiEffect.receiveAsFlow()

fun onEvent(event: XxxUiEvent) { when (event) { /* ... */ } }
```

Com 9 telas previstas (Splash, Home, Upsert, Detail, Search, Settings, Alarme em tela cheia, Autenticação, Backup), são ~70 linhas quase idênticas. A pergunta é: **vale a pena extrair essa repetição para uma classe base?**

Uma classe base não muda *o que* o app faz. Ela muda **quanto código cada ViewModel escreve** e **quanta liberdade** cada um tem.

---

## 2. Como é hoje (padrão explícito)

Código real da spec 02 (`feature/splash/presentation/viewmodel/SplashViewModel.kt`):

```kotlin
internal class SplashViewModel(
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Showing)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    // Channel BUFFERED: a navegação é entregue exatamente uma vez, mesmo se a tela estiver girando.
    private val _uiEffect = Channel<SplashUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<SplashUiEffect> = _uiEffect.receiveAsFlow()

    private var hasStarted = false

    fun onEvent(event: SplashUiEvent) {
        when (event) {
            SplashUiEvent.OnSplashStarted -> startOnce()
        }
    }

    private fun startOnce() {
        if (hasStarted) return
        hasStarted = true
        viewModelScope.launch(dispatcher) {
            delay(SPLASH_DELAY_MILLIS)
            _uiEffect.send(SplashUiEffect.NavigateToHome)
        }
    }
}
```

**O que é bom aqui:** tudo está à vista. Quem lê sabe exatamente qual é o tipo do estado, que o efeito é um `Channel` com buffer, e que `send` *suspende* (não perde efeito). Não há nada "escondido" numa classe pai.

**O que incomoda:** as 4 primeiras declarações são iguais em todo ViewModel; trocar a estratégia de efeitos (ex.: de `Channel` para outra coisa) exigiria editar todos.

---

## 3. A proposta: `MviViewModel<S, E, F>`

### 3.1 O código da classe base

Arquivo proposto: `core/ui/MviViewModel.kt`.

```kotlin
package com.br.androideventscheduler.core.ui

/**
 * Base para ViewModels no padrão State / Event / Effect.
 *
 * @param S tipo do estado (sealed class da tela)
 * @param E tipo dos eventos do usuário (sealed interface)
 * @param F tipo dos efeitos one-shot (sealed interface)
 */
abstract class MviViewModel<S : Any, E : Any, F : Any>(
    initialState: S,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    /** Estado observado pela UI. */
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _uiEffect = Channel<F>(Channel.BUFFERED)
    /** Efeitos one-shot, entregues exatamente uma vez (coletar com `CollectEffects`). */
    val uiEffect: Flow<F> = _uiEffect.receiveAsFlow()

    /** Valor atual do estado (atalho de leitura). */
    protected val currentState: S get() = _uiState.value

    /** Único ponto de entrada da UI. */
    abstract fun onEvent(event: E)

    /** Atualiza o estado de forma atômica a partir do estado atual. */
    protected fun setState(reducer: S.() -> S) {
        _uiState.update { it.reducer() }
    }

    /** Emite um efeito one-shot. Suspende se o buffer estiver cheio (nunca descarta). */
    protected suspend fun sendEffect(effect: F) {
        _uiEffect.send(effect)
    }

    /** Variante sem suspensão para chamadas fora de corrotina; lança se o buffer estiver cheio. */
    protected fun trySendEffect(effect: F) {
        check(_uiEffect.trySend(effect).isSuccess) { "Buffer de efeitos cheio: $effect" }
    }
}
```

> **Importante — duas correções em relação à primeira versão da arquitetura.**
> A primeira proposta tinha `protected fun sendEffect(effect: F) { _effect.trySend(effect) }`.
> 1. `trySend` **descarta silenciosamente** o efeito quando o buffer (64 itens) está cheio. Raro, mas é um bug invisível. Aqui `sendEffect` é `suspend` (usa `send`) e a variante não suspensa **falha alto** em vez de perder o efeito.
> 2. Os nomes públicos passam a ser `uiState`/`uiEffect` — os mesmos do padrão atual — para que **telas e testes não mudem** numa migração.

### 3.2 Como fica um ViewModel que usa a base

```kotlin
internal class XxxViewModel(
    private val dispatcher: CoroutineDispatcher,
) : MviViewModel<XxxUiState, XxxUiEvent, XxxUiEffect>(XxxUiState.Loading) {

    override fun onEvent(event: XxxUiEvent) {
        when (event) {
            XxxUiEvent.OnSaveClicked -> save()
        }
    }

    private fun save() = viewModelScope.launch(dispatcher) {
        setState { XxxUiState.Saving }
        sendEffect(XxxUiEffect.NavigateBack)
    }
}
```

O que **não** muda: dispatcher injetado, `viewModelScope`, `sealed` para estado/evento/efeito, `CollectEffects` na tela, conteúdo stateless `(uiState, onEvent)`.

---

## 4. Migração passo a passo — feature de exemplo: Splash

A Splash é a única tela implementada hoje (spec 02). Migrá-la mostra o processo completo, que se repete igual nas demais.

### Passo 1 — Criar a base e testá-la (uma vez para o projeto)

1. Criar `core/ui/MviViewModel.kt` (seção 3.1).
2. Criar `core/ui/MviViewModelTest.kt` cobrindo a base **uma única vez** (ver seção 6.1). Esse teste passa a "valer" para todos os ViewModels.

### Passo 2 — Trocar a herança e remover as declarações repetidas

```diff
-internal class SplashViewModel(
-    private val dispatcher: CoroutineDispatcher,
-) : ViewModel() {
-    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Showing)
-    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()
-
-    private val _uiEffect = Channel<SplashUiEffect>(Channel.BUFFERED)
-    val uiEffect: Flow<SplashUiEffect> = _uiEffect.receiveAsFlow()
+internal class SplashViewModel(
+    private val dispatcher: CoroutineDispatcher,
+) : MviViewModel<SplashUiState, SplashUiEvent, SplashUiEffect>(SplashUiState.Showing) {

     private var hasStarted = false

-    fun onEvent(event: SplashUiEvent) {
+    override fun onEvent(event: SplashUiEvent) {
         when (event) {
             SplashUiEvent.OnSplashStarted -> startOnce()
         }
     }

     private fun startOnce() {
         if (hasStarted) return
         hasStarted = true
         viewModelScope.launch(dispatcher) {
             delay(SPLASH_DELAY_MILLIS)
-            _uiEffect.send(SplashUiEffect.NavigateToHome)
+            sendEffect(SplashUiEffect.NavigateToHome)
         }
     }
```

Resultado: 6 linhas a menos e 6 imports a menos (`MutableStateFlow`, `StateFlow`, `asStateFlow`, `Channel`, `Flow`, `receiveAsFlow`).

### Passo 3 — Conferir o que **não** precisa mudar

| Arquivo | Muda? | Por quê |
|---|---|---|
| `SplashScreen.kt` | não | continua lendo `viewModel.uiEffect` e chamando `viewModel.onEvent` |
| `SplashUiState/Event/Effect.kt` | não | os tipos `sealed` são os mesmos |
| `SplashModule.kt` (Koin) | não | o construtor é o mesmo |
| `SplashViewModelTest.kt` | não | testa só a API pública (`uiEffect`, `uiState`, `onEvent`) |
| `SplashScreenRobolectricTest.kt` | não | testa o conteúdo stateless |

### Passo 4 — Visibilidade

`SplashViewModel` é `internal`; `MviViewModel` precisa ser **pública** (fica em `core`, usada por todas as features). Isso é aceitável: ela não expõe nada de nenhuma feature.

### Passo 5 — Verificar

`./gradlew ktlintCheck detekt testDebugUnitTest :app:assembleDebug` → os 5 testes do `SplashViewModelTest` devem passar **sem alteração**. Se algum teste precisasse mudar, a migração estaria alterando comportamento — sinal de erro.

### Passo 6 — Atualizar documentação

Steering (`arquitetura.md` §5), spec 00 (R5, design, tasks) e o `design.md` de cada spec com ViewModel (lista completa na seção 10).

---

## 5. O caso difícil: formulário com `sealed class` de estado (Configurações)

A Splash é fácil porque o estado nunca muda. A dificuldade aparece em telas cujo estado é uma `sealed class` **e** precisa ser editado campo a campo — como a tela de Configurações (spec 03):

```kotlin
internal sealed class SettingsUiState {
    data object Loading : SettingsUiState()
    data class Content(val draft: UserSettings, val isSaving: Boolean = false, val emailError: Boolean = false) :
        SettingsUiState()
}
```

### 5.1 Com a classe base: o `setState` genérico não "sabe" que o estado é `Content`

```kotlin
is SettingsUiEvent.OnEmailChanged -> setState {
    // `this` é SettingsUiState (a sealed inteira), não Content. É preciso checar o tipo:
    if (this is SettingsUiState.Content) copy(draft = draft.copy(activeAccountEmail = event.email), emailError = false)
    else this
}
```

Cada evento de edição repete o `if (this is Content) ... else this`. A solução usual é um helper **no próprio ViewModel**:

```kotlin
private inline fun updateContent(transform: SettingsUiState.Content.() -> SettingsUiState.Content) =
    setState { if (this is SettingsUiState.Content) transform() else this }

// uso
is SettingsUiEvent.OnEmailChanged -> updateContent { copy(draft = draft.copy(activeAccountEmail = event.email)) }
```

Dá para generalizar na base com um parâmetro de tipo extra, mas o custo de leitura cresce:

```kotlin
protected inline fun <reified C : S> updateIf(crossinline transform: C.() -> S) =
    setState { if (this is C) transform() else this }

// uso
updateIf<SettingsUiState.Content> { copy(emailError = false) }
```

### 5.2 Sem a classe base (como é hoje)

O ViewModel declara o `MutableStateFlow` e escreve o mesmo `updateContent` privado — **o mesmo código do 5.1**. Ou seja: **para estados `sealed`, a classe base economiza só as declarações do topo**; a parte trabalhosa (atualizar a variante certa) é igual nos dois modelos.

### 5.3 Telas com estado derivado (Home, Pesquisa)

Na Home e na Pesquisa o estado **não é editado por eventos**: ele é *derivado* de fontes (`combine(...)`) e coletado em `_uiState.value = it`. Com a classe base isso vira `setState { it }` ou exigiria expor um `protected fun replaceState(newState: S)`. É um ponto em que a base ajuda pouco e pode confundir ("por que `setState` com reducer se eu só quero substituir?").

---

## 6. Impacto nos testes

### 6.1 Um teste a mais (da base), escrito uma vez

```kotlin
class MviViewModelTest {
    private class FakeViewModel : MviViewModel<Int, String, String>(initialState = 0) {
        override fun onEvent(event: String) {
            setState { this + 1 }
            viewModelScope.launch { sendEffect("handled:$event") }
        }
    }

    @get:Rule val mainRule = MainDispatcherRule()

    @Test fun `given event when handled then state updates and effect is delivered once`() = runTest(mainRule.dispatcher) {
        val viewModel = FakeViewModel()
        val effects = mutableListOf<String>()
        val job = launch { viewModel.uiEffect.collect { effects += it } }

        viewModel.onEvent("a")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value)
        assertEquals(listOf("handled:a"), effects)
        job.cancel()
    }
}
```

### 6.2 Testes dos ViewModels: não mudam

Como a API pública (`uiState`, `uiEffect`, `onEvent`) é a mesma, **nenhum teste de ViewModel ou de tela muda** na migração. Esse é o principal critério de segurança da migração.

### 6.3 O que fica mais difícil de testar

Nada fica mais difícil. Mas há um detalhe: com `trySendEffect` lançando exceção quando o buffer enche, um bug que antes passaria despercebido agora derruba o teste — o que é desejável.

---

## 7. Vantagens e desvantagens

### 7.1 Vantagens

| # | Vantagem | Peso neste projeto |
|---|---|---|
| V1 | **Menos repetição**: ~6 linhas e ~6 imports a menos por ViewModel (≈ 55 linhas no total previsto) | Baixo — o volume é pequeno |
| V2 | **Uniformidade garantida pelo compilador**: impossível esquecer o `Channel.BUFFERED`, expor `MutableStateFlow` ou usar `StateFlow` para efeito | **Médio** — evita erros de quem chega ao projeto |
| V3 | **Ponto único de mudança**: trocar a estratégia de efeitos (ex.: adicionar log, métricas, `SharedFlow`) em um arquivo | Baixo-médio — improvável mudar, mas possível |
| V4 | **Contrato explícito**: `onEvent` é `abstract`; toda tela tem obrigatoriamente um ponto único de entrada | Médio |
| V5 | **Onboarding mais rápido** para quem conhece MVI (Orbit, Mavericks, "BaseViewModel") | Médio |
| V6 | **Teste da mecânica uma única vez** (6.1) | Baixo |

### 7.2 Desvantagens

| # | Desvantagem | Peso neste projeto |
|---|---|---|
| D1 | **Herança obrigatória**: a classe base ocupa a única superclasse do ViewModel; não dá para herdar de outra coisa (ex.: `AndroidViewModel`) sem uma segunda base | Baixo — não usamos `AndroidViewModel` |
| D2 | **Indireção**: quem lê `setState { }` precisa abrir outra classe para saber que é `update` atômico, ou que `sendEffect` suspende | **Médio** — foi o motivo da decisão atual |
| D3 | **Encaixe ruim com `sealed class` de estado**: `setState` recebe a sealed inteira; editar `Content` exige `updateContent`/`updateIf` (seção 5) | **Médio-alto** — todas as nossas telas usam sealed |
| D4 | **Encaixe ruim com estado derivado** (`combine` na Home/Pesquisa): exige `replaceState` ou uso estranho de `setState` | Médio |
| D5 | **"Classe base cresce"**: com o tempo surgem `launchWithLoading`, `handleError`, `showToast`... e a base vira um lugar de lógica genérica difícil de mudar | Médio — risco de governança |
| D6 | **Generics de três parâmetros** nas assinaturas (`MviViewModel<SettingsUiState, SettingsUiEvent, SettingsUiEffect>`) | Baixo |
| D7 | **Custo de migração** (pequeno hoje, cresce com cada spec implementada) | Baixo agora; vira médio após a spec 07 |
| D8 | **Acoplamento ao modularizar**: todo `:feature:*` passa a depender de `:core:ui` por herança (já depende para `CollectEffects`, então impacto pequeno) | Baixo |

### 7.3 Riscos e como mitigar

| Risco | Mitigação |
|---|---|
| Efeito perdido por `trySend` com buffer cheio | `sendEffect` suspenso; `trySendEffect` falha alto (seção 3.1) |
| Base acumulando utilidades | Regra no steering: a base só pode ter `uiState`, `uiEffect`, `currentState`, `onEvent`, `setState`, `sendEffect`/`trySendEffect`. Qualquer adição precisa de decisão registrada |
| Uso de `setState` em estados derivados | Documentar `replaceState(newState)` ou manter `combine(...).collect { replaceState(it) }` |
| Divergência entre telas já migradas e não migradas | Migrar todas de uma vez (são poucas hoje) |

---

## 8. Alternativas intermediárias

Não é uma escolha binária. Quatro opções:

### A. Manter o padrão explícito (status quo)
Nenhuma mudança. Repetição pequena, máxima clareza.

### B. Classe base `MviViewModel` (esta proposta)
Seções 3–5.

### C. Composição: pequenos "blocos" em vez de herança
Duas classes utilitárias em `core/ui`, usadas **por dentro** do ViewModel:

```kotlin
class UiStateHolder<S : Any>(initial: S) {
    private val mutable = MutableStateFlow(initial)
    val flow: StateFlow<S> = mutable.asStateFlow()
    val value: S get() = mutable.value
    fun update(reducer: S.() -> S) = mutable.update { it.reducer() }
    fun replace(newState: S) { mutable.value = newState }
}

class UiEffectChannel<F : Any> {
    private val channel = Channel<F>(Channel.BUFFERED)
    val flow: Flow<F> = channel.receiveAsFlow()
    suspend fun send(effect: F) = channel.send(effect)
}

// No ViewModel (continua estendendo ViewModel):
internal class SplashViewModel(private val dispatcher: CoroutineDispatcher) : ViewModel() {
    private val state = UiStateHolder<SplashUiState>(SplashUiState.Showing)
    private val effects = UiEffectChannel<SplashUiEffect>()
    val uiState = state.flow
    val uiEffect = effects.flow
    // ...
}
```
Elimina a configuração repetida (tipo de `Channel`, `asStateFlow`), mantém a herança livre e deixa os campos públicos visíveis no próprio ViewModel.

### D. Só um contrato (interface), sem implementação
```kotlin
interface MviContract<S, E, F> {
    val uiState: StateFlow<S>
    val uiEffect: Flow<F>
    fun onEvent(event: E)
}
```
Garante a forma pública (útil para telas genéricas e testes), mas não remove repetição nenhuma.

---

## 9. Matriz de decisão

Notas de 1 (ruim) a 5 (ótimo), considerando **este** projeto (app de módulo único, ~9 telas, todos os estados `sealed`, Home/Pesquisa com estado derivado, equipe pequena).

| Critério (peso) | A. Explícito | B. Classe base | C. Composição | D. Interface |
|---|---|---|---|---|
| Clareza ao ler um ViewModel (3) | **5** | 3 | 4 | 5 |
| Menos repetição (2) | 2 | **5** | 4 | 2 |
| Uniformidade/erros evitados (3) | 3 | **5** | 4 | 3 |
| Encaixe com `sealed` e estado derivado (3) | **5** | 2 | **5** | 5 |
| Flexibilidade de herança (1) | **5** | 2 | **5** | **5** |
| Custo de migração agora (1) | **5** | 4 | 4 | 4 |
| Risco de crescer sem controle (2) | **5** | 2 | 4 | 5 |
| **Total ponderado** (máx. 75) | **63** | 51 | **63** | 60 |

Leitura: **B** ganha em repetição e uniformidade, mas perde onde este projeto é mais sensível (estados `sealed`, estado derivado, clareza). **A** e **C** empatam; C troca um pouco de clareza por menos configuração repetida.

> Se o projeto crescer para dezenas de telas e várias pessoas (estágios 3–4 da arquitetura), o peso de "uniformidade" e "menos repetição" sobe e **B** ou **C** passam a liderar. Vale reavaliar nesse momento.

---

## 10. Plano de migração (se aprovada)

Válido para **B** (e, com nomes trocados, para **C**). Quanto antes, mais barato: hoje só existe o `SplashViewModel`.

| Passo | O que fazer | Arquivos |
|---|---|---|
| 1 | Criar a base e o teste da base | `core/ui/MviViewModel.kt`, `core/ui/MviViewModelTest.kt` (test) |
| 2 | Migrar o `SplashViewModel` (seção 4) e rodar a verificação sem mudar nenhum teste | `feature/splash/presentation/viewmodel/SplashViewModel.kt` |
| 3 | Atualizar o steering: §5 (exemplo, regras, "não há ViewModel base" → "use `MviViewModel`"), §5.1 e checklist §7 | `.kiro/steering/arquitetura.md` |
| 4 | Atualizar a spec 00 (R5, design, task 3) e o `specs/README.md` (convenção de presentation) | `.kiro/specs/00-fundacao-projeto/*`, `.kiro/specs/README.md` |
| 5 | Atualizar os `design.md` com ViewModel: 03, 05, 06, 07, 08, 09, 10, 11, 12, 13, 14, 16, 17 (trocar `_uiState.value =`/`_uiEffect.send` por `setState`/`sendEffect`/`replaceState`) | `.kiro/specs/*/design.md` |
| 6 | Registrar a decisão no histórico de `ordem-de-execucao.md` | `.kiro/specs/ordem-de-execucao.md` |
| 7 | Um PR dedicado (`refactor/mvi-viewmodel`), antes da próxima spec com tela | — |

**Estimativa:** 1 PR pequeno (≈ 1–2 h). Após a spec 07 (formulário grande), a migração passa a tocar ViewModels maiores e o custo cresce.

**Reversão:** como a API pública não muda, voltar ao padrão explícito é mecânico: copiar as declarações para cada ViewModel e apagar a base. Nenhum teste precisa mudar.

---

## 11. Recomendação e perguntas para decidir

**Recomendação técnica:** manter **A (padrão explícito)** enquanto o app estiver no estágio 1–2 (módulo único, ~9 telas). Se a repetição incomodar, preferir **C (composição)** a **B (herança)**, porque C mantém a clareza e encaixa melhor nos estados `sealed` e derivados que todas as nossas telas usam. Reavaliar **B** quando houver modularização ou equipe maior.

**Perguntas que decidem a escolha:**

1. A repetição de ~6 linhas por ViewModel incomoda mais do que ter de abrir outra classe para entender `setState`/`sendEffect`?
2. Haverá mais pessoas contribuindo em breve (o que valoriza uniformidade imposta pelo compilador)?
3. Aceitamos o helper `updateContent`/`updateIf` para editar estados `sealed` — com ou sem a base?
4. Queremos um ponto único para instrumentar efeitos (logs/analytics) no futuro?

Se as respostas forem "sim, sim, sim, sim" → **B**. Se forem majoritariamente "não" → **A**. Misto → **C**.

---

## 12. Glossário

| Termo | Significado |
|---|---|
| **MVVM** | Model–View–ViewModel: a View observa o ViewModel, que expõe dados prontos para exibir |
| **MVI** | Model–View–Intent: a View envia *intenções* (eventos) e recebe um *estado* único e imutável; efeitos pontuais à parte |
| **State / UiState** | Fotografia imutável do que a tela mostra agora |
| **Event / UiEvent** | Ação do usuário (clique, digitação) |
| **Effect / UiEffect (one-shot)** | Algo que deve acontecer uma única vez (navegar, mostrar Snackbar); não pode reexecutar ao girar a tela |
| **`StateFlow`** | Fluxo que sempre tem um valor atual; ideal para estado |
| **`Channel` (BUFFERED)** | Fila entre produtor e consumidor; cada item é recebido uma vez; o buffer guarda itens enquanto a tela não coleta |
| **`send` vs `trySend`** | `send` espera espaço no buffer (nunca perde); `trySend` falha na hora se não houver espaço |
| **Reducer** | Função `estadoAtual -> novoEstado` |
| **Estado derivado** | Estado calculado a partir de outras fontes (`combine`), em vez de editado por eventos |
| **`sealed class`** | Hierarquia fechada de tipos; o `when` sobre ela é exaustivo |
