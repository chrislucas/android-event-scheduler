# 16 — Autenticação com o bloqueio do dispositivo — Design

## Visão geral
```
Processo inicia ──► AppLockController.lockState = Pending ──► lê preferências ──► Locked | Unlocked
                         │
App em background ──► registra elapsedRealtime ──► ao voltar: AppLockPolicy.shouldLock(...) ──► Locked
                         │
AppNavHost: Pending → fundo neutro · Locked → LockScreen · Unlocked → NavDisplay   ← nada da agenda é composto sem desbloquear
                         │
LockScreen ──► DeviceCredentialAuthenticator.authenticate() ──► Success → controller.unlock()
```

## Pacotes
```
core/security/
├── domain/
│   ├── AppLockState.kt                # Pending | Locked | Unlocked
│   ├── AppLockTimeout.kt              # IMMEDIATE(0), ONE_MINUTE(60_000), FIVE_MINUTES(300_000)
│   ├── AppLockPolicy.kt               # regra pura: shouldLock(enabled, backgroundedAtMillis, nowMillis, timeout)
│   └── DeviceCredentialAuthenticator.kt  # interface + AuthResult (Success, Canceled, LockedOut, NotAvailable, Error)
├── data/
│   ├── AppLockController.kt           # estado em memória (StateFlow<Boolean>) + observa ProcessLifecycleOwner
│   ├── DeviceSecurityChecker.kt       # KeyguardManager.isDeviceSecure
│   ├── BiometricPromptAuthenticator.kt     # API 30+
│   └── KeyguardIntentAuthenticator.kt      # API 24–29 (Activity Result)
└── di/SecurityModule.kt
feature/auth/
├── presentation/{state,event,effect,viewmodel,screen}/   # LockScreen (stateless LockContent + previews)
└── di/AuthModule.kt
```
`core.security` é usado por `app` (gate), `feature.auth` (tela), `feature.settings` (switch) e `feature.alarm` (ocultar local/notas) → precisa ficar em `core`. A tela de bloqueio **não é uma rota** do Navigation 3: é um gate acima do `NavDisplay`, para não poluir o back stack nem permitir "voltar" para o conteúdo.

## Domínio
```kotlin
enum class AppLockTimeout(val durationMillis: Long) { IMMEDIATE(0), ONE_MINUTE(60_000), FIVE_MINUTES(300_000) }

object AppLockPolicy {
    fun shouldLock(enabled: Boolean, backgroundedAtMillis: Long?, nowMillis: Long, timeout: AppLockTimeout): Boolean =
        enabled && backgroundedAtMillis != null && nowMillis - backgroundedAtMillis >= timeout.durationMillis
}

sealed interface AuthResult { data object Success; data object Canceled; data object LockedOut;
                              data object NotAvailable; data class Error(val code: Int) }

interface DeviceCredentialAuthenticator {
    /** Mostra a confirmação de credencial do sistema. Chamado a partir da Activity em primeiro plano. */
    suspend fun authenticate(activity: FragmentActivity, title: String, subtitle: String): AuthResult
}
```
`UserSettings` (spec 03) ganha `appLockEnabled: Boolean = false` e `appLockTimeout: AppLockTimeout = ONE_MINUTE`; chaves `app_lock_enabled`, `app_lock_timeout` no DataStore.

## AppLockController (estado em memória)
```kotlin
class AppLockController(
    private val settingsRepository: UserSettingsRepository,
    private val deviceSecurity: DeviceSecurityChecker,
    private val elapsedRealtime: () -> Long,          // SystemClock::elapsedRealtime (injetado; fake em testes)
    applicationScope: CoroutineScope,
) : DefaultLifecycleObserver {
    val lockState: StateFlow<AppLockState>             // Pending | Locked | Unlocked
    val lockDisabledBecauseDeviceInsecure: Flow<Unit>   // evento one-shot para a mensagem de R3.6
    fun unlock()
    fun suppressNextLock()                               // antes de abrir share sheet / seletor de arquivo (R2.3)
    override fun onStop(owner: LifecycleOwner)          // registra backgroundedAt
    override fun onStart(owner: LifecycleOwner)         // aplica AppLockPolicy
}
```
- Registrado em `ProcessLifecycleOwner.get().lifecycle` (inicia no `KoinInitializer`, após `startKoin`). `ProcessLifecycleOwner` ignora rotação (despacha `onStop` com atraso de ~700 ms), cumprindo R2.3.
- Estado inicial: o DataStore é assíncrono e **não** deve ser lido com `runBlocking` na inicialização. Por isso o controller expõe três estados — `Pending` (preferências ainda não lidas), `Locked`, `Unlocked` — e o gate mostra um fundo neutro, sem conteúdo, em `Pending`. Na primeira emissão: `appLockEnabled == false` → `Unlocked`; `true` → `Locked`. Assim a agenda nunca "pisca" antes de bloquear.
- R3.6: na primeira leitura, `appLockEnabled && !deviceSecurity.isDeviceSecure()` → grava `appLockEnabled = false`, desbloqueia e emite o aviso.
- `suppressNextLock()`: share sheet (spec 12), seletor/compartilhamento de arquivo (spec 17) e a própria tela de credencial levam o app a `onStop`; a supressão vale para **uma** volta e expira em 2 min.

## Autenticadores
- `BiometricPromptAuthenticator` (API ≥ 30): `BiometricPrompt(activity, executor, callback)` com `PromptInfo.Builder().setTitle().setSubtitle().setAllowedAuthenticators(DEVICE_CREDENTIAL)` (sem `setNegativeButtonText`, proibido com `DEVICE_CREDENTIAL`). Converte callback em `suspendCancellableCoroutine`. Códigos: `ERROR_USER_CANCELED`/`ERROR_CANCELED`/`ERROR_NEGATIVE_BUTTON` → `Canceled`; `ERROR_LOCKOUT(_PERMANENT)` → `LockedOut`; `ERROR_NO_DEVICE_CREDENTIAL` → `NotAvailable`.
- `KeyguardIntentAuthenticator` (API 24–29): `keyguardManager.createConfirmDeviceCredentialIntent(title, subtitle)`; `null` → `NotAvailable`. Lançado com um `ActivityResultLauncher` registrado na `MainActivity` (`registerForActivityResult(StartActivityForResult())` precisa ser registrado antes de `STARTED`) — por isso há um `CredentialLauncherRegistry` que a `MainActivity` preenche em `onCreate`. `RESULT_OK` → `Success`, senão `Canceled`.
- `SecurityModule`: `single<DeviceCredentialAuthenticator> { if (SDK_INT >= R) BiometricPromptAuthenticator() else KeyguardIntentAuthenticator(get()) }` — único `if` de versão (R5.3).
- `MainActivity` passa a estender **`FragmentActivity`** (exigência do `BiometricPrompt`); `ComponentActivity` → `FragmentActivity` não muda Compose (`setContent` funciona igual).

## Gate no `AppNavHost` e `FLAG_SECURE`
```kotlin
val lockState by appLockController.lockState.collectAsStateWithLifecycle()
when (lockState) {
    AppLockState.Pending -> LockPendingBackground()       // fundo neutro, sem dados
    AppLockState.Locked -> LockScreen()
    AppLockState.Unlocked -> NavDisplay(...)             // back stack em rememberNavBackStack: preservado ao desbloquear (R3.3)
}
```
- `rememberNavBackStack` fica **acima** do `if`, para não ser descartado quando o gate troca de ramo.
- Deep link (spec 04, `DeepLinkState`) e arquivo recebido (spec 17, `IncomingFileState`): só são consumidos em `Unlocked` → R4.2 sem código extra.
- `FLAG_SECURE`: `MainActivity` observa `settings.appLockEnabled` e chama `window.setFlags/clearFlags(FLAG_SECURE)`.
- `BackHandler(enabled = lockState != Unlocked) { activity.finish() }` no gate (R3.7).

## feature.auth — LockScreen
```kotlin
sealed class LockUiState { data object Idle : LockUiState(); data object Authenticating : LockUiState()
                           data class Message(@StringRes val messageRes: Int) : LockUiState() }
sealed interface LockUiEvent { data object OnScreenShown; data object OnUnlockClicked
                               data class OnAuthResult(val result: AuthResult) }
sealed interface LockUiEffect { data object LaunchAuthentication }
```
- `LockViewModel(appLockController, dispatcher)`: `OnScreenShown` → efeito `LaunchAuthentication` **uma vez** (flag); `OnUnlockClicked` → `LaunchAuthentication`; `OnAuthResult(Success)` → `controller.unlock()`; `Canceled` → `Idle`; `LockedOut` → `Message(lock_locked_out)`; `NotAvailable` → `Message(lock_not_available)`.
- A tela coleta `LaunchAuthentication`, chama o autenticador com a `FragmentActivity` (`LocalActivity`/`LocalContext` como `FragmentActivity`) e devolve `OnAuthResult`. A Activity nunca vai ao ViewModel.
- Previews: Idle, Authenticating, LockedOut, NotAvailable.

## Settings (alteração da spec 03)
- `SettingsUiState.Content` ganha `appLockEnabled`, `appLockTimeout`, `isDeviceSecure`.
- Eventos `OnAppLockToggled(Boolean)`, `OnAppLockTimeoutSelected(AppLockTimeout)`, `OnAuthResultForToggle(AuthResult)`, `OnOpenSecuritySettingsClicked`; efeitos `RequestAuthentication`, `OpenSecuritySettings`.
- Alternar o switch **não** salva direto: emite `RequestAuthentication`; só com `Success` grava (R1.2/R1.3). Ligar o switch chama `appLockController.unlock()` após salvar (o usuário acabou de se autenticar).
- `isDeviceSecure` relido em `ON_RESUME` (o usuário pode ter configurado um PIN e voltado).

## Alarme em tela cheia (alteração da spec 14)
`FullScreenAlarmViewModel` lê `appLockEnabled`; se `true`, `Content.location = null` e `Content.notes = null` (R4.3).

## Decisões de UseCase
Nenhum. `AppLockPolicy` é regra pura (objeto de domínio, testado isoladamente); o controle de estado é infraestrutura (`AppLockController`); persistência é passa-direto do `UserSettingsRepository`.

## Testes mínimos
- `AppLockPolicyTest`: desligado nunca bloqueia; `IMMEDIATE` bloqueia com 0 ms; 59 s × 60 s; exatamente no limite bloqueia; sem `backgroundedAt` não bloqueia.
- `AppLockControllerTest` (fake de settings, relógio e device security): inicia bloqueado só se habilitado; `onStop`+`onStart` respeitam o timeout; `suppressNextLock` vale uma vez e expira; desligar desbloqueia; dispositivo inseguro → desativa e emite aviso; nada é persistido sobre "bloqueado".
- `BiometricPromptAuthenticatorTest` (mapeamento de códigos de erro → `AuthResult`, função pura extraída).
- `LockViewModelTest`: dispara autenticação uma vez ao mostrar; sucesso desbloqueia; cancelar volta a `Idle`; lockout mostra mensagem.
- `LockScreenRobolectricTest`: textos por resource, botão com `Role.Button`, mensagens por estado.
- `AppLockGateRobolectricTest` (`MainActivity`, controller fake): bloqueado não compõe a Home (`assertDoesNotExist`); desbloquear mostra a tela anterior com o back stack intacto; voltar no bloqueio finaliza a Activity; `FLAG_SECURE` ligado/desligado conforme a preferência.
- `SettingsViewModelTest` (acréscimos): ligar sem autenticar não grava; `Success` grava; dispositivo inseguro desabilita o switch.
- `FullScreenAlarmViewModelTest` (acréscimo): com proteção ligada, local e notas ocultos.
- **Não verificável em JVM** (declarar no PR): diálogo real do sistema (PIN/padrão), `KeyguardManager` em API 24–29 em aparelho, aparência em "Recentes".
