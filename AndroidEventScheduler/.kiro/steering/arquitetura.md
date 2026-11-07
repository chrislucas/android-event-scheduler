---
inclusion: always
---

# Arquitetura obrigatória do app Android (RememberMe)

> Arquitetura de referência para **todo** código do projeto AndroidEventScheduler (app RememberMe), implementado conforme as specs em `.kiro/specs/` (índice em `.kiro/specs/README.md`). Inspirada em Clean Architecture + MVVM com elementos de MVI (State/Event/Effect), **organizada por pacotes dentro de um único módulo Gradle (`:app`)** — não por múltiplos módulos. Toda tarefa das specs deve seguir este documento.

## 1. Princípios

- **Módulo único (`:app`), organização por PACOTES.** Nada de `:core:*`/`:feature:*` como módulos Gradle nesta fase.
- **Pronto para modularizar sem reescrever**: a estrutura de pacotes espelha o que seriam módulos. Cada feature é autocontida (data/domain/presentation) e o `core` é compartilhado, de modo que mover um pacote para um módulo Gradle no futuro seja só recortar e colar + ajustar o `build.gradle.kts`. Valem as mesmas regras de dependência que valeriam entre módulos.
- **Clean Architecture + MVVM por feature** (feature-first), em camadas `data/ domain/ presentation/`.
- **Fluxo de dados unidirecional (UDF)**: `View → ViewModel → (UseCase, quando existir) → Repository → Data Source → volta ao ViewModel → View`.
- **UseCase só quando necessário** (política do projeto). Criar UseCase apenas se houver ao menos um destes motivos, registrado na seção "Decisões de UseCase" do `design.md` da spec:
  1. regra compartilhada por 2+ pontos de entrada (ViewModels, receivers);
  2. orquestração de 2+ fontes que precisam ficar consistentes (ex.: banco + alarme);
  3. regra de negócio com lógica própria que merece teste isolado.
  Delegação pura a repositório (observar, buscar por id, salvar) **não** vira UseCase: o ViewModel injeta a interface do repositório. Lógica local e pura de uma única tela vai para um helper/mapper do próprio pacote.
- **Exponha só o necessário** entre pacotes (prefira `internal`). A **API pública de uma feature** é só: a entrada de navegação (`navigation/<Feature>Entry.kt`) e o módulo Koin (`di/<Feature>Module.kt`) — o que `app` precisa para montá-la. Telas, ViewModels, repositórios e modelos da feature são `internal`/não referenciados fora dela. Ao virar módulo Gradle, esses dois pontos viram o pacote `api` do módulo.
- **Código compartilhado** vai para `core`; cada feature é independente das outras (uma feature **não** importa pacotes de outra feature).
- **UI "burra"**: nenhuma regra de negócio na UI.
- **Repositórios por interface** no domínio; implementação na camada de dados.
- **Nomes de variáveis sugestivos e autoexplicativos.** Prefira o nome que revela a intenção ao nome curto. Vale especialmente para cálculos de data/recorrência/alarme: nada de `a`, `b`, `d`, `t` soltos — use `occurrenceStartDate`, `nextTriggerMillis`, `offsetMinutes`, `durationDays`, etc. Siglas consagradas e o índice de laço `i`/`index` são aceitáveis. Constantes mágicas viram `const val` com nome (ex.: `SPLASH_DELAY_MILLIS = 1500L`, `ALL_DAY_ALERT_HOUR = 9`).

## 2. Tech stack (fixa)

| Camada | Tecnologia |
|---|---|
| UI | Jetpack Compose + Material 3 (tema escuro fixo) |
| DI | **Koin** |
| Inicialização | **Jetpack App Startup** (`androidx.startup`) iniciando o Koin antes de `Application.onCreate`, receivers e activities |
| Database | Room (+ KSP), schema exportado, sem migração destrutiva |
| Storage | DataStore Preferences |
| Async | Kotlin Coroutines + Flow |
| Datas/horas | `java.time` com *core library desugaring* (minSdk 24); `Clock`/fuso **injetados** |
| Navegação | Navigation 3 (rotas em `core.navigation`, host em `app`) |
| Alarmes | `AlarmManager` (exato quando permitido) + `NotificationManager` |
| Testes | JUnit4 + MockK + kotlinx-coroutines-test (UI com Compose UI Test + Robolectric). **Sem Turbine** |
| Qualidade | ktlint + detekt via `build-logic` (convention plugin), CI no GitHub Actions; gerados por `docs/project-foundation/bootstrap-foundation.sh` |

Não há `docs/tech_stack.md`; as versões vivem em `gradle/libs.versions.toml`.

## 3. Pacotes e camadas

Pacote base: `com.br.androideventscheduler`, com três áreas de topo: `app` (composição), `core` (compartilhado) e `feature` (N features).

```
com.br.androideventscheduler/
├── app/                        # único lugar que conhece todas as features
│   ├── RememberMeApplication.kt, KoinInitializer.kt, AppModules.kt
│   ├── AppNavHost.kt           # NavDisplay; agrega os entryProviders das features
│   └── DeepLinkState.kt
├── MainActivity.kt
├── core/
│   ├── designsystem/           # AndroidEventSchedulerTheme, cores, tipografia
│   ├── ui/                     # CollectEffects, SemanticsTags, component/ (diálogos compartilhados), format/
│   ├── navigation/             # rotas (NavKey) @Serializable
│   ├── common/                 # StringProvider, utilitários
│   ├── di/                     # dispatchers (io/default/main)
│   ├── database/               # RememberMeDatabase
│   ├── datastore/              # DataStore<Preferences> único
│   ├── schedule/               # domain (modelos, RecurrenceEngine, repositório) + data (Room)
│   ├── alarm/                  # AlarmScheduler, receivers, notificações, use cases de alarme
│   ├── preferences/            # UserSettings compartilhado
│   ├── permissions/            # NotificationPermissionEffect
│   └── testing/                # MainDispatcherRule, FakeClock, fakes
└── feature/
    ├── splash/  home/  upsert/  detail/  search/  settings/  alarm/
    └── <feature>/
        ├── data/        # (quando houver) local/ (Room/DataStore), remote/ (APIs, DTOs — só se houver rede), repository/, mapper/
        ├── domain/      # model/, repository/ (interfaces), usecase/ (só quando justificado)
        ├── presentation/# screen/, component/, state/, event/, effect/, viewmodel/, mapper/
        ├── navigation/  # <Feature>Entry.kt: extensão que registra a rota da feature
        └── di/          # módulo Koin da feature
```

Fora dos pacotes `com.br.androideventscheduler`, na raiz do projeto Gradle: `build-logic/` (convention plugins; substitui o `buildSrc/` da arquitetura de referência por ser um composite build com melhor cache), `config/` (detekt), `docs/` e `.github/` (CI na raiz do git).

**Regra de ouro para modularizar depois**: cada subpacote de `feature/<nome>` e o `core` devem ser autossuficientes:
- uma feature **nunca** importa de `feature.<outra>`; o que for comum sobe para `core`;
- cada feature expõe uma API mínima (tela + entry de navegação) e traz seu próprio módulo Koin, agregado em `app.AppModules`;
- `core` **não** importa `feature` nem `app`. Por isso o `AppNavHost` fica em `app`.

**Regras de dependência entre camadas** (nunca invertê-las):
- `presentation` → `domain`
- `data` → `domain`
- `domain` **não** depende de `data` nem de `presentation`, e **não** importa `android.*`/Compose/SDKs (`java.time` é permitido).

Pontos de entrada do sistema (receivers, activities) são detalhe de plataforma: ficam na camada `data` do `core.alarm` ou em `presentation` da feature, e chamam o domínio por Koin.

## 4. Pacote `core` (compartilhado)

`core` reúne o que mais de uma feature usa. Há dois tipos de conteúdo, e a diferença importa ao modularizar:

| Tipo | Pacotes | Observação |
|---|---|---|
| **Infraestrutura genérica** (sem regra de negócio do app) | `designsystem`, `ui`, `navigation` (rotas), `common`, `di`, `database`, `datastore`, `permissions`, `testing` | Viram `:core:designsystem`, `:core:ui`, `:core:database`… |
| **Domínio compartilhado** (agregados usados por 2+ features) | `schedule`, `alarm`, `preferences` | Têm `domain/` + `data/` internos, como uma feature, mas sem tela. Viram `:core:schedule` etc. (ou `:core:model` + `:core:data`). Só entra aqui o que mais de uma feature precisa; o resto fica na feature |

Não existe `core.network` hoje: o app é 100% offline. Se surgir rede, criar `core/network` (Retrofit/OkHttp/interceptors) e `data/remote` nas features que a usarem — só nesse momento, nunca vazio.

### 4.1 Evolução para multi-módulo (como escalar)

A estrutura segue os quatro estágios da arquitetura de referência; **o projeto está no estágio 1**:

| Estágio | Tamanho | Estrutura | Quando |
|---|---|---|---|
| 1. Pequeno | 5–10 telas | `:app` único, pastas `core/` + `feature/` | **Atual** (~8 telas) |
| 2. Médio | 20–50 telas | `:app` único, mesmas pastas, mais features | Enquanto o build e os conflitos forem toleráveis |
| 3. Grande | 50+ telas, vários devs | `:core:*` (designsystem, database, navigation…) + `:feature:*` (um módulo por feature) | Build lento, conflitos frequentes ou necessidade de impor fronteiras |
| 4. Enterprise | muito grande | módulos agrupados (`core/…`, `feature/…`) e módulos de API/implementação | Times independentes, entrega modular |

Para extrair uma feature (estágio 2 → 3), em ordem: (1) garantir que ela não importa outra feature e que o que for comum já está em `core`; (2) criar o módulo com `build.gradle.kts` aplicando o convention plugin de qualidade; (3) mover o pacote inteiro (`feature/<nome>`); (4) expor apenas `navigation` + `di` como API pública; (5) `app` passa a depender do módulo; (6) rodar a verificação completa. Pro tip: **não modularize antes da dor** — as regras de dependência já dão o isolamento.

### 4.2 Alinhamento com a arquitetura de referência e desvios intencionais

O steering segue a arquitetura de referência "Scalable Android App Architecture for Jetpack Compose" (Clean Architecture + MVVM + modularização por feature: `core/` compartilhado, `feature/` por domínio, camadas `data/domain/presentation`, fluxo unidirecional, UI burra, repositórios por interface, API mínima, testes de use cases e repositórios). Desvios **deliberados**:

| Referência | Aqui | Motivo |
|---|---|---|
| Hilt | **Koin** + App Startup | Decisão do projeto: DI simples, sem kapt/KSP extra; receivers acessam o grafo via Koin |
| Navigation Compose | **Navigation 3** | Back stack como estado e rotas serializáveis; alinhado ao Compose atual |
| UseCase sempre entre ViewModel e Repository | **UseCase só quando justificado** (seção 1) | Evita classes de delegação pura; o fluxo UDF permanece |
| `NavHost` dentro de `core/navigation` | `core.navigation` só tem as rotas; o host fica em `app` | O host precisa conhecer todas as telas; `core` não pode importar `feature` |
| `core/network` | ausente | App offline |
| `buildSrc/` | `build-logic/` (composite build) | Melhor cache incremental; mesmo propósito |
| `core` só com infraestrutura | `core` também tem domínio compartilhado (`schedule`, `alarm`, `preferences`) | Uma feature não importa outra; agregados usados por várias features sobem para `core` |
| `data/remote` obrigatório | `data/local` (+ `remote` se houver rede) | Persistência local |
| `presentation/` sem mapper | `presentation/mapper/` opcional | Formatação de UI model fora do Composable |
| — | pacote `app` (composição) e `navigation/`, `di/` por feature | Necessários para Koin/Navigation 3 sem `core` conhecer features |

## 5. Contrato da camada de presentation (State / Event / Effect)

Toda tela segue o trio **State + Event + Effect**. **Não há ViewModel base** nem helpers `setState`/`sendEffect`: cada ViewModel estende `androidx.lifecycle.ViewModel` e gerencia o próprio estado e efeitos de forma explícita.

- **UiState** — `sealed class` imutável com as variantes da tela (tipicamente `Loading`, `Content(...)` e, quando fizer sentido, `Error`/`NotFound`/`Empty`). Exposto como `StateFlow<XxxUiState>`. A UI só lê e faz `when` exaustivo.
- **UiEvent** — `sealed interface` com as ações do usuário. A UI envia ao ViewModel por `onEvent(event)`.
- **UiEffect** — `sealed interface` de efeitos **one-shot** (navegar, Snackbar/Toast, abrir share sheet, fechar tela). **Nunca** vão no `UiState`.

### 5.1 Exemplo

```kotlin
// presentation/state
sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Content(val items: List<ItemUi>) : HomeUiState()
    data class Error(@StringRes val messageRes: Int) : HomeUiState()
}

// presentation/event
sealed interface HomeUiEvent {
    data class ItemClicked(val id: String) : HomeUiEvent
    data object RetryClicked : HomeUiEvent
}

// presentation/effect
sealed interface HomeUiEffect {
    data class NavigateToDetail(val id: String) : HomeUiEffect
}

// presentation/viewmodel — dispatcher injetado; DI por Koin (viewModel { HomeViewModel(get(), get(named("default"))) })
class HomeViewModel(
    private val scheduleRepository: ScheduleRepository,   // passa-direto: sem UseCase
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Channel BUFFERED: cada efeito é entregue exatamente uma vez, mesmo se a UI estiver girando/em segundo plano.
    private val _uiEffect = Channel<HomeUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<HomeUiEffect> = _uiEffect.receiveAsFlow()

    init { loadItems() }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.ItemClicked -> viewModelScope.launch { _uiEffect.send(HomeUiEffect.NavigateToDetail(event.id)) }
            HomeUiEvent.RetryClicked -> loadItems()
        }
    }

    private fun loadItems() = viewModelScope.launch(dispatcher) {
        _uiState.value = HomeUiState.Loading
        runCatching { scheduleRepository.getAll() }
            .onSuccess { _uiState.value = HomeUiState.Content(it.map(::toUi)) }
            .onFailure { _uiState.value = HomeUiState.Error(R.string.error_generic) }
    }
}
```

```kotlin
// presentation/screen — efeitos coletados uma vez, conteúdo stateless
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel(), onNavigateToDetail: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectEffects(viewModel.uiEffect) { effect ->
        when (effect) { is HomeUiEffect.NavigateToDetail -> onNavigateToDetail(effect.id) }
    }
    HomeContent(uiState = uiState, onEvent = viewModel::onEvent)
}
```

`CollectEffects` (em `core.ui`) é apenas um helper **de UI** (`repeatOnLifecycle(STARTED)` + `collect`), não uma abstração de ViewModel.

**Regras do padrão:**
- **Atualização de estado**: `_uiState.value = ...` ou `_uiState.update { ... }` diretamente. Quando só uma variante é editável (ex.: `Content` de um formulário), o ViewModel pode ter um `private inline fun updateContent(transform: (Content) -> Content)` local — não um helper compartilhado.
- **Eventos únicos**: `Channel(Channel.BUFFERED)` + `receiveAsFlow()` para efeitos críticos (navegação, Snackbar, diálogos, share): entrega exatamente uma vez e retém no buffer enquanto a tela gira ou está em segundo plano. `MutableSharedFlow(replay = 0)` só é aceitável para efeitos descartáveis (ex.: pulso de animação); nenhuma spec atual o usa.
- Um `Channel` de efeitos por ViewModel; coletado **uma vez** na tela.
- Estado contínuo → `UiState`/`StateFlow`. Ação pontual → `UiEffect`/`Channel`. Nunca o contrário.
- O `@Composable` de conteúdo é **stateless**: recebe `uiState` + `onEvent`.
- Dispatcher **injetado** no ViewModel (nunca fixo), para o teste trocar por `StandardTestDispatcher`.
- Mensagens para o usuário trafegam como `@StringRes`/tipos de erro, não como texto cru de exceção.
- Textos de UI seguem a regra da seção 5.5 (nada de string literal em componente).

### 5.2 Testes

**O que testar em cada camada** (todo código novo traz os testes junto): UseCases e repositórios (regra de negócio e persistência — prioridade máxima), ViewModels (estado e efeitos), mappers/formatadores, e telas/componentes por semântica. A UI é "burra", então a cobertura de regra está no domínio.

> **Padrão de nome dos testes unitários — Given/When/Then em inglês.** Todo teste unitário nomeia o método como ``fun `given <contexto> when <ação> then <resultado esperado>`()``, com o texto **em inglês**. Ex.: ``fun `given blank title when saving then fails with BlankTitle`()``. Vale para domínio, data, ViewModel e UI. O corpo segue o arranjo preparação → ação → asserção. Classes de teste: `XxxTest`.

O projeto **não usa Turbine**. Para `StateFlow`/`Channel`, use `kotlinx-coroutines-test` com coleta direta:

```kotlin
class HomeViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    @Test fun `given item clicked when handling event then emits NavigateToDetail`() = runTest {
        val viewModel = HomeViewModel(scheduleRepository = fakeRepository, dispatcher = mainRule.dispatcher)
        val effects = mutableListOf<HomeUiEffect>()
        val collectJob = launch { viewModel.uiEffect.toList(effects) }

        viewModel.onEvent(HomeUiEvent.ItemClicked("42"))
        runCurrent()

        assertEquals(HomeUiEffect.NavigateToDetail("42"), effects.first())
        collectJob.cancel()
    }
}
```

Equivalentes ao Turbine: `flow.first()`, `flow.take(n).toList()`, `launch { flow.toList(dest) }` + `cancel()`. Para `StateFlow`, ler `.value` após `advanceUntilIdle()`/`runCurrent()`. Tempo (debounce, delay) com tempo virtual (`advanceTimeBy`); relógio com `FakeClock`.

Telas e componentes: Compose UI Test + Robolectric (`isIncludeAndroidResources = true`), asserções por `testTag` e semântica. Room: banco em memória sob Robolectric. Receivers/AlarmManager/notificações: shadows do Robolectric.

### 5.3 Preview para todo componente visual (obrigatório)

**Todo componente visual — qualquer função `@Composable` que emita UI (tela, conteúdo, componente, linha, diálogo, barra) — tem uma função `@Preview`.** É requisito de pronto: código com `@Composable` visual sem preview não é aceito no PR.

- Mínimo: **uma** preview por composable; ideal: **uma por estado relevante**.
- Sem preview só nas exceções listadas abaixo, sempre com comentário `// Sem preview: <motivo>` no arquivo.

- A preview fica **no mesmo arquivo**, é `private` e se chama `<Composable>Preview`.
- Sempre dentro do `AndroidEventSchedulerTheme`.
- **Um preview por estado relevante**: vazio, carregando, erro, selecionado, texto longo.
- **Telas com ViewModel**: a preview é do conteúdo **stateless** `(uiState, onEvent)`, nunca da tela que injeta o ViewModel.
- **Dependências** (Koin, repositórios, Context do sistema) entram por parâmetro; a preview passa dados fixos. Nunca chamar Koin, banco ou `AlarmManager` dentro de uma preview.
- **Exceções** (sem preview, com o motivo num comentário): wrappers sem UI própria (tema), `Activity`s e implementações cuja preview já existe no componente que as usa.

### 5.4 Acessibilidade e semântica

- Todo elemento interativo tem `testTag` (constantes em `core.ui.SemanticsTags`), `contentDescription`/`role`/`stateDescription` quando aplicável e alvo de toque ≥ 48dp.
- Diálogos e listas de opções usam `selectableGroup` + `Role.RadioButton`/`Role.Checkbox`/`Role.Switch` conforme o caso; seleção nunca é comunicada só por cor.

### 5.5 Textos: sempre de resources

- **Texto fixo vem de `strings.xml`** (pt-BR), nunca escrito direto no componente: `Text`, `label`, `placeholder`, `contentDescription`, `stateDescription`, título de diálogo/Snackbar/notificação, `Toast` etc. Em Compose use `stringResource(R.string.x)`; fora de Compose, `Resources`/`StringProvider`.
- **Texto com variável usa resource com formatação** sempre que possível, nunca concatenação nem string template no código:
  - `<string name="alarm_badge_in_minutes">Evento em %1$d minutos</string>` → `stringResource(R.string.alarm_badge_in_minutes, minutes)`;
  - placeholders posicionais (`%1$s`, `%2$d`), para a tradução poder reordenar;
  - quantidade → `<plurals>` + `pluralStringResource` (ex.: `1 vez` / `5000 vezes`), não `if (n == 1)`.
- **Ordem de preferência para o texto chegar à tela**:
  1. o ViewModel/mapper expõe **dados** (número, data, enum) e a UI formata com `stringResource`;
  2. quando o texto precisa ser montado fora da UI (mapper, Snackbar via efeito), usar `@StringRes` + argumentos (ex.: `ShowMessage(@StringRes messageRes: Int, args: List<Any> = emptyList())`) ou `StringProvider`, sempre apontando para um resource.
- **Nunca** no `domain`: o domínio não conhece `R` nem `Resources`; devolve tipos (enum/erro tipado) e a apresentação mapeia para resource.
- **Texto de exceção** (`e.message`) não vai para a tela.
- **Não são texto de UI** e podem ficar no código: `testTag` e outras constantes técnicas, chaves de DataStore, ids, nomes de canal de log/`Log` e mensagens de `require`/`check`. Já o **nome do canal de notificação** e qualquer texto que o usuário veja vêm de resource.
- **Previews e testes**: previews usam os mesmos resources; asserções de UI preferem `testTag`/semântica ou `context.getString(R.string.x)` a texto literal copiado.
- Strings de recurso têm nome por contexto (`<tela>_<elemento>`, ex.: `settings_save_button`, `home_empty_day`); sem reaproveitar um resource só porque o texto atual coincide.

## 6. Mapeamento spec → pacotes

| Spec | Pacotes principais |
|---|---|
| 00 fundação | `app`, `core.designsystem/ui/navigation/di/testing` |
| 01 núcleo de agendamentos | `core.schedule`, `core.database`, `core.ui.format` |
| 02 splash | `feature.splash` |
| 03 configurações | `core.datastore`, `core.preferences`, `feature.settings` |
| 04 alarmes e notificações | `core.alarm`, `core.permissions` |
| 05–06, 09 (adesivo do dia) | `feature.home` |
| 07–08, 09 (cor/emoji) | `feature.upsert`, `core.ui.component` |
| 10–12 | `feature.detail` |
| 13 | `feature.search` |
| 14 | `feature.alarm`, `core.alarm.domain.usecase` |
| 15 | `core.alarm` |

## 7. Checklist de arquitetura (parte do DoD de toda tarefa)

- [ ] Código no pacote certo: `core.*` ou `feature.<nome>.{data,domain,presentation,navigation,di}`; composição só em `app`.
- [ ] Nenhum import de `feature.<outra>`; `core` não importa `feature`/`app`; o que for comum está em `core`.
- [ ] `domain` sem `android.*`; repositórios por interface no domínio; API pública da feature = só `navigation` + `di`.
- [ ] UseCase somente se previsto/justificado no `design.md` da spec; sem UseCase de delegação pura.
- [ ] ViewModel estende `ViewModel` direto (sem base/helpers); `UiState` é `sealed class` exposto por `StateFlow`, `UiEvent` é `sealed interface`, `UiEffect` sai por `Channel` (`receiveAsFlow`); dispatcher e relógio injetados.
- [ ] UI coleta efeitos uma vez (`CollectEffects`); conteúdo stateless `(uiState, onEvent)`.
- [ ] Todo `@Composable` visual novo tem `@Preview` (seção 5.3, obrigatório) — ou o comentário `// Sem preview: <motivo>` numa das exceções permitidas.
- [ ] Nenhum texto literal na UI: fixo via `strings.xml`, com variável via resource formatado (`%1$s`) ou `plurals` (5.5); tags e semântica de acessibilidade presentes (5.4).
- [ ] Testes de UseCases, repositórios, ViewModels e mappers presentes; `given/when/then` em inglês, sem Turbine; módulo Koin novo coberto pelo `KoinModulesVerificationTest`.
