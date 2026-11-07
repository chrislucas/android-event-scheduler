# 02 — Splash — Design

## Pacotes
```
feature/splash/
├── presentation/
│   ├── state/SplashUiState.kt          # sealed class SplashUiState { data object Showing }
│   ├── event/SplashUiEvent.kt          # OnSplashStarted
│   ├── effect/SplashUiEffect.kt        # NavigateToHome
│   ├── viewmodel/SplashViewModel.kt
│   └── screen/SplashScreen.kt          # SplashScreen(viewModel, onNavigateToHome) + SplashContent()
├── navigation/SplashEntry.kt           # fun EntryProviderScope<NavKey>.splashEntry(onNavigateToHome: () -> Unit)
└── di/SplashModule.kt                  # viewModel { SplashViewModel(get(named("default")), SPLASH_DELAY_MILLIS) }
```

## ViewModel
```kotlin
sealed class SplashUiState { data object Showing : SplashUiState() }
sealed interface SplashUiEvent { data object OnSplashStarted : SplashUiEvent }
sealed interface SplashUiEffect { data object NavigateToHome : SplashUiEffect }

class SplashViewModel(
    private val dispatcher: CoroutineDispatcher,
    private val splashDelayMillis: Long = SPLASH_DELAY_MILLIS,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Showing)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()
    private val _uiEffect = Channel<SplashUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<SplashUiEffect> = _uiEffect.receiveAsFlow()
    private var hasStarted = false

    fun onEvent(event: SplashUiEvent) {
        when (event) { SplashUiEvent.OnSplashStarted -> startOnce() }
    }
    private fun startOnce() {
        if (hasStarted) return           // evita reemissão ao recompor/rotacionar
        hasStarted = true
        viewModelScope.launch(dispatcher) { delay(splashDelayMillis); _uiEffect.send(SplashUiEffect.NavigateToHome) }
    }
}
```
Como o `ViewModel` sobrevive à rotação, `hasStarted` + Channel garantem entrega única.

## Tela
- `SplashContent()` stateless: `Box` centralizado, `Icon` em círculo ciano (usar `Icons.Outlined.Notifications` ou vetor próprio simples), `Text("RememberMe")`.
- `SplashScreen` chama `onEvent(OnSplashStarted)` em `LaunchedEffect(Unit)` e usa `CollectEffects`.
- Previews: `SplashContentPreview` (única).

## Navegação
`splashEntry(onNavigateToHome)` em `app.AppNavHost`: `backStack.remove(Splash); backStack.add(Home)` — troca atômica (`backStack.clear(); add(Home)` também aceitável). Remover o placeholder da spec 00.

## SplashScreen do sistema
`core-splashscreen` + `installSplashScreen()` na `MainActivity`; `themes.xml` com `windowSplashScreenBackground=#0A0A0A` e ícone do app.

## Decisões de UseCase
Nenhum (sem regra de negócio; o tempo de espera é detalhe de apresentação).

## Testes
- `SplashViewModelTest`: `given started event when delay elapses then emits NavigateToHome` (usa `StandardTestDispatcher`, `advanceTimeBy`); `given started twice when delay elapses then emits only one effect`; `given no event then no effect`.
- `SplashScreenRobolectricTest`: logo e título existem com tags, role Image e heading; `assertContentDescriptionEquals`.
