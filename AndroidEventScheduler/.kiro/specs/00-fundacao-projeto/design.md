# 00 — Fundação do projeto — Design

## Estrutura criada
```
com.br.androideventscheduler/
├── app/
│   ├── RememberMeApplication.kt        # Application vazia (Koin vem do App Startup)
│   ├── KoinInitializer.kt              # Initializer<KoinApplication>: startKoin { androidContext; modules(appModules) }
│   ├── AppModules.kt                   # val appModules = listOf(coreModules..., featureModules...)
│   └── AppNavHost.kt                   # NavDisplay + entryProvider agregando as features
├── MainActivity.kt                     # enableEdgeToEdge + setContent { AndroidEventSchedulerTheme { AppNavHost() } }
└── core/
    ├── designsystem/ (Color.kt, Theme.kt, Type.kt)   # reaproveita ui/theme existente movendo para core.designsystem
    ├── ui/ (CollectEffects.kt, SemanticsTags.kt)
    ├── navigation/ AppRoute.kt
    ├── di/ DispatchersModule.kt
    └── testing/ MainDispatcherRule.kt, FakeClock.kt
```
O pacote `ui.theme` atual é **movido** para `core.designsystem` (rename com IDE/`rename_symbol`, sem duplicar).

## Contratos

Padrão de ViewModel (sem classe base; ver `arquitetura.md` §5.1):
```kotlin
class XxxViewModel(private val dispatcher: CoroutineDispatcher) : ViewModel() {
    private val _uiState = MutableStateFlow<XxxUiState>(XxxUiState.Loading)   // sealed class
    val uiState: StateFlow<XxxUiState> = _uiState.asStateFlow()
    private val _uiEffect = Channel<XxxUiEffect>(Channel.BUFFERED)            // one-shot: entregue exatamente uma vez
    val uiEffect: Flow<XxxUiEffect> = _uiEffect.receiveAsFlow()
    fun onEvent(event: XxxUiEvent) { when (event) { /* sealed interface, when exaustivo */ } }
}
```
- Nenhuma abstração compartilhada: a spec 00 entrega só o helper de UI abaixo, o `SemanticsTags` e as convenções.
- `MutableSharedFlow(replay = 0)` fica reservado a efeitos descartáveis; nenhum previsto.

```kotlin
@Composable
fun <F> CollectEffects(effects: Flow<F>, onEffect: suspend (F) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { effects.collect { onEffect(it) } }
    }
}
```

```kotlin
// core/navigation/AppRoute.kt
@Serializable data object Splash : NavKey
@Serializable data object Home : NavKey
@Serializable data object Search : NavKey
@Serializable data object Settings : NavKey
@Serializable data class ScheduleDetail(val scheduleId: String) : NavKey
@Serializable data class UpsertSchedule(val scheduleId: String? = null, val initialDateEpochDay: Long? = null) : NavKey
```

## Koin + App Startup
- `KoinInitializer : Initializer<KoinApplication>`; registrado no manifest dentro de `androidx.startup.InitializationProvider` (`tools:node="merge"`).
- Receivers (boot, alarme) obtêm dependências com `GlobalContext.get()`/`KoinComponent` — seguro porque o provider roda antes.
- Dispatchers: `single<CoroutineDispatcher>(named("io")) { Dispatchers.IO }` etc.
- Teste do grafo: `KoinModulesVerificationTest` com `appModules.verify()` (ou `koinApplication { modules(...) }.checkModules()` com `androidContext` mock); cresce a cada spec.

## Tema
Dark fixo; `Color.kt` com constantes nomeadas (`BackgroundBlack = 0xFF0A0A0A` …). Tipografia padrão do Material 3. `AndroidEventSchedulerTheme` aplica `darkColorScheme` e `MaterialTheme`.

## Navegação
`AppNavHost` usa `rememberNavBackStack(Splash)` e `NavDisplay(backStack, entryDecorators = [rememberSaveableStateHolderNavEntryDecorator, rememberViewModelStoreNavEntryDecorator], entryProvider = entryProvider { … })`. Cada spec de feature adiciona uma linha `splashEntry(...)`, `homeEntry(...)`. Callbacks de navegação são lambdas (`backStack.add(...)`, `backStack.removeLastOrNull()`), fechando sobre o `backStack` no `AppNavHost` — as features nunca enxergam o back stack. Consultar a skill `navigation-3` para nomes exatos das APIs da versão adotada.

## Build
- Desugaring: `compileOptions.isCoreLibraryDesugaringEnabled = true` + `coreLibraryDesugaring(libs.desugar.jdk.libs)`.
- Room precisa de KSP compatível com Kotlin 2.2.x e AGP 9 (Kotlin embutido): se houver incompatibilidade, usar a skill `agp-9-upgrade` e escolher a versão de KSP/Room compatível; documentar no PR.
- **Qualidade (já implementada via script)** — `docs/project-foundation/bootstrap-foundation.sh` gerou:
  ```
  build-logic/{settings.gradle.kts, convention/build.gradle.kts, convention/src/main/kotlin/CodeQualityConventionPlugin.kt}
  config/detekt/detekt.yml · .editorconfig · .githooks/pre-commit · scripts/setup-git-hooks.sh
  ../.github/workflows/ci-validation.yml          (raiz do git; working-directory: AndroidEventScheduler)
  ```
  e editou `settings.gradle.kts` (`includeBuild`), `build.gradle.kts` (`id("androideventscheduler.code.quality") apply false` + `subprojects { pluginManager.apply(...) }`), `gradle/libs.versions.toml` (detekt, ktlintGradle, ktlint) e `.gitignore`.
- **Detekt 2.0 alpha** (`dev.detekt`) porque o projeto usa JDK 25 e o detekt 1.23.x não roda nele. Ver `docs/project-foundation/README.md`.
- Gate real: sem `ignoreFailures`. Violações legadas: `./gradlew detektBaseline`.
- **CI**: dois jobs paralelos — `quality` (ktlint + detekt) e `build` (testes + `assembleDebug`); não gera APK. Secret scan e build de APK ficam fora desta spec.

## Decisões de UseCase
Nenhum UseCase nesta spec.

## Riscos
- Combinação AGP 9.5 + KSP + Room: validar cedo (task 1).
- Navigation 3 muda rápido: fixar versão estável e conferir a skill.
