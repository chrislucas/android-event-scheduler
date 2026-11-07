# 00 — Fundação do projeto — Requisitos

## Introdução
Prepara o `:app` (hoje um template Compose vazio) para receber as features: dependências, DI com Koin inicializado por App Startup, tema escuro do RememberMe, convenções de presentation (sem ViewModel base), tags de semântica, navegação (Navigation 3), utilitários de teste e lint. Nenhuma tela de negócio é criada aqui.

## Requisitos

### R1 — Dependências e build
**User story:** como dev, quero as bibliotecas da stack fixa no catálogo de versões para que as demais specs só consumam.
1. O `gradle/libs.versions.toml` SHALL declarar (versões estáveis atuais, verificadas no momento da execução): Koin (core, android, compose, androidx-startup se existir), Room (runtime, ktx, compiler via KSP), DataStore Preferences, Coroutines (+test), Navigation 3 (runtime, ui, lifecycle-viewmodel), kotlinx-serialization, Lifecycle (viewmodel-compose, runtime-compose), App Startup, MockK, Robolectric, Compose UI Test, `desugar_jdk_libs`.
2. O `app/build.gradle.kts` SHALL habilitar *core library desugaring*, `testOptions.unitTests.isIncludeAndroidResources = true` e `isReturnDefaultValues = true`, KSP e o plugin de serialização.
3. `./gradlew :app:assembleDebug` SHALL continuar passando em minSdk 24 / compileSdk atual.

### R2 — Estrutura de pacotes
1. O código SHALL seguir `core.*` e `feature.*` sob `com.br.androideventscheduler`, com pacote `app` para `Application`, `KoinInitializer` e `AppNavHost`.
2. Pacotes SHALL ser criados apenas quando houver código (sem `.gitkeep`/pastas vazias).

### R3 — Injeção de dependência
1. WHEN o processo inicia THEN o Koin SHALL estar inicializado por `androidx.startup.Initializer` **antes** de `Application.onCreate` e de qualquer `Receiver`/`Activity`.
2. O módulo `core.di` SHALL prover os dispatchers `named("io")`, `named("default")`, `named("main")`.
3. Cada feature SHALL ter seu módulo Koin, agregado em uma lista única no `KoinInitializer`.
4. Um teste SHALL verificar o grafo Koin (`checkModules`/`verify`) e falhar se faltar definição.

### R4 — Design system e tema
1. `AndroidEventSchedulerTheme` SHALL usar tema **escuro** fixo com paleta: fundo `#0A0A0A`, superfície `#171717`, superfície elevada `#1C1C1E`, destaque ciano `#26C6DA`, texto `#FFFFFF`/`#B0B0B0`.
2. Cores dinâmicas (Material You) SHALL ser desativadas.
3. WHEN a Activity abre THEN SHALL usar `enableEdgeToEdge()` e `Scaffold` com insets corretos (ver skill `edge-to-edge`).

### R5 — Convenções de presentation e semântica
1. **Não SHALL existir ViewModel base nem helpers `setState`/`sendEffect`.** Cada ViewModel estende `ViewModel` diretamente, expõe `uiState: StateFlow<XxxUiState>` (`sealed class`), recebe eventos por `onEvent(XxxUiEvent)` (`sealed interface`) e expõe efeitos one-shot por `uiEffect: Flow<XxxUiEffect>` (`Channel(BUFFERED).receiveAsFlow()`), como em `arquitetura.md` §5; dispatcher injetado.
2. `core.ui.SemanticsTags` SHALL existir como `object` de `const val` (as demais specs adicionam suas tags).
3. Um helper de **UI** `CollectEffects(viewModel.uiEffect) { … }` SHALL coletar efeitos uma única vez com `repeatOnLifecycle(STARTED)`.

### R6 — Navegação
1. `core.navigation` SHALL declarar as rotas `@Serializable` (`NavKey`): `Splash`, `Home`, `Search`, `Settings`, `ScheduleDetail(scheduleId)`, `UpsertSchedule(scheduleId: String?, initialDateEpochDay: Long?)`.
2. `app.AppNavHost` SHALL montar o `NavDisplay` com back stack salva em `rememberNavBackStack`, começando em `Splash`; cada feature contribui um `entryProvider` próprio (nas specs seguintes).
3. Esta spec SHALL entregar o `AppNavHost` com uma tela placeholder temporária para `Splash` removida na spec 02.

### R7 — Testes e qualidade
1. `core.testing` SHALL prover `MainDispatcherRule` e `FakeClock` (`java.time.Clock` fixo/avançável).
2. **Análise estática via `build-logic`**: o projeto SHALL ter o composite build `build-logic` (incluído em `pluginManagement { includeBuild("build-logic") }`) com o convention plugin `androideventscheduler.code.quality`, que aplica **detekt** e **ktlint** a todos os `subprojects`, lê a versão do ktlint do `libs.versions.toml`, usa `config/detekt/detekt.yml` e `.editorconfig`, e **reprova o build** em violação (`-Pquality.ignoreFailures=true` só para adoção gradual). `./gradlew ktlintCheck detekt` SHALL passar.
3. **CI**: SHALL existir `.github/workflows/ci-validation.yml` (na raiz do git, com `working-directory: AndroidEventScheduler`) com um job `quality` (`ktlintCheck detekt`) e um job `build` (`testDebugUnitTest` + `:app:assembleDebug`), disparados em push/PR para `main` e `workflow_dispatch`, com cache do Gradle, validação do wrapper e upload de relatórios.
4. **Reprodutibilidade**: a fundação (build-logic + config + CI + hooks opcionais) SHALL ser gerada pelo script `docs/project-foundation/bootstrap-foundation.sh`, documentado em `docs/project-foundation/README.md`, reutilizável em outros projetos Android/KMP.
5. O pre-commit opcional (`.githooks/pre-commit`, ativado por `scripts/setup-git-hooks.sh`) SHALL rodar `ktlintFormat` nos arquivos staged e `detekt`.
6. SHALL existir um teste Robolectric de smoke: `MainActivity` sobe com o tema aplicado.
