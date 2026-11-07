# 14 — Alarme em tela cheia e botão Adiar — Design

## Pacotes
```
core/alarm/domain/usecase/SnoozeAlarmUseCase.kt         (novo)
feature/alarm/
├── presentation/
│   ├── state/    FullScreenAlarmUiState.kt
│   ├── event/    FullScreenAlarmUiEvent.kt
│   ├── effect/   FullScreenAlarmUiEffect.kt
│   ├── viewmodel/FullScreenAlarmViewModel.kt
│   ├── component/ SnoozeChipsRow.kt, AlarmActionButtons.kt
│   └── screen/   FullScreenAlarmActivity.kt, FullScreenAlarmScreen.kt (+ Content)
└── di/AlarmFeatureModule.kt
```
`FullScreenAlarmActivity` é a `entry point` do sistema e fica em `presentation/screen` (é a "tela").

## Domínio — `SnoozeAlarmUseCase`
```kotlin
class SnoozeAlarmUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock,
) {
    suspend operator fun invoke(scheduleId: String, snoozeMinutes: Int): Result<Long> = runCatching {
        require(snoozeMinutes > 0) { "snoozeMinutes must be positive" }
        val item = scheduleRepository.getById(scheduleId) ?: throw ScheduleNotFoundException(scheduleId)
        val snoozedTriggerMillis = clock.millis() + snoozeMinutes * MILLIS_PER_MINUTE
        alarmScheduler.schedule(AlarmRequest(item.id, snoozedTriggerMillis, AlertPresentationMode.forType(item.type)))
        snoozedTriggerMillis
    }
}
```
- `AlarmScheduler.schedule` **substitui** o alarme pendente do item (spec 04 R1.5). No momento do adiamento esse alarme pendente é o da próxima ocorrência (reagendada no disparo). Ele é restaurado quando o alarme adiado dispara, pois o receiver sempre chama `ScheduleNextAlarmUseCase`, que recalcula a próxima ocorrência futura. Documentar com teste de integração leve.
- Não depende de formatação nem de `AlarmRequest` extra (diferente do PDF, que reconstruía título/notas/local).
- **Por que UseCase:** regra de adiamento (validação, substituição do alarme da série, tipo→modo) com repositório+agendador; isolada e testável. Hoje há um único ponto de entrada (a tela); a notificação de lembrete com ação "Adiar" é um possível segundo ponto, fora do escopo.

## Contrato (State / Event / Effect)
```kotlin
sealed class FullScreenAlarmUiState {
    data object Loading : FullScreenAlarmUiState()
    data class Content(
        val title: String, val timeText: String, val location: String?, val notes: String?,
        val colorHex: Long, val minutesUntilStart: Int,
        val snoozeOptions: List<Int> = listOf(5, 10, 15), val selectedSnoozeMinutes: Int = 10,
        val isProcessing: Boolean = false, val errorMessageRes: Int? = null,
    ) : FullScreenAlarmUiState()
}
sealed interface FullScreenAlarmUiEvent { OnSnoozeDurationSelected(Int); OnSnoozeClicked; OnDismissClicked; OnBackPressed }
sealed interface FullScreenAlarmUiEffect { CloseScreen(cancelNotification: Boolean) }
```

## ViewModel
`FullScreenAlarmViewModel(scheduleId, scheduleRepository, userSettingsRepository, snoozeAlarmUseCase, formatter, clock, dispatcher)`:
- `init`: carrega item (+`defaultSnoozeMinutes`; se fora de `snoozeOptions`, usar 10). Item `null` → `CloseScreen(cancelNotification = true)`.
- `minutesUntilStart` = `Duration.between(now, startDateTime of next/current occurrence)`; para recorrente usar a ocorrência cujo início é a mais próxima de `agora` (via `RecurrenceEngine.occurrenceDates(item, today)` ⇒ primeira ocorrência com `início + 1h ≥ agora`). Clamp ≥ 0.
- `OnSnoozeClicked`: ignora se `Content.isProcessing`; chama use case; sucesso → `CloseScreen(cancelNotification = true)`; falha `ScheduleNotFoundException` → fechar; outras → `errorMessageRes`.
- `OnDismissClicked` → `CloseScreen(true)`; `OnBackPressed` → `CloseScreen(false)`.

## Activity
- `ComponentActivity`, `setShowWhenLocked(true)`/`setTurnScreenOn(true)` (API 27+) e `window.addFlags(FLAG_KEEP_SCREEN_ON | FLAG_SHOW_WHEN_LOCKED | FLAG_TURN_SCREEN_ON | FLAG_DISMISS_KEYGUARD)` condicionais a API < 27; `enableEdgeToEdge()`; `scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID) ?: finish()`.
- `onNewIntent` (novo alarme com a tela aberta): atualizar `scheduleId` e recarregar (ViewModel com chave por id: `koinViewModel(key = scheduleId) { parametersOf(scheduleId) }`).
- Efeito `CloseScreen(cancel)`: `if (cancel) NotificationManagerCompat.from(this).cancel(AlarmIntents.notificationId(scheduleId))`; `finish()`.
- Manifest:
  ```xml
  <activity android:name=".feature.alarm.presentation.screen.FullScreenAlarmActivity"
      android:exported="false" android:launchMode="singleTop" android:taskAffinity=""
      android:excludeFromRecents="true" android:showWhenLocked="true" android:turnScreenOn="true" android:theme="@style/Theme.AndroidEventScheduler">
      <intent-filter><action android:name="com.br.androideventscheduler.action.SHOW_FULL_SCREEN_ALARM"/><category android:name="android.intent.category.DEFAULT"/></intent-filter>
  </activity>
  ```
- Constantes (`ACTION_SHOW_FULL_SCREEN_ALARM`, `EXTRA_SCHEDULE_ID`) ficam em `core.alarm` (`AlarmIntents`) — o receiver as usa sem importar a feature.

## UI
Fundo escuro, selo ciano, título grande (`headlineLarge`), card de informações; `SnoozeChipsRow` com `FilterChip` e `selectableGroup`; botões grandes (≥ 56dp). Tudo `verticalScroll` para fonte grande. Previews: evento simples; com local/notas; "agora"; processando; erro; paisagem.

## Decisões de UseCase
- `SnoozeAlarmUseCase`: justificado (regra + orquestração), ver acima.
- Nenhum UseCase para carregar/formatar.

## Testes
- `SnoozeAlarmUseCaseTest` (`FakeClock`, MockK): agenda `agora + N`; modo EVENT→full-screen, REMINDER→notificação; N ≤ 0 falha sem agendar; item inexistente falha; não altera o item (`coVerify(exactly = 0) { upsert }`); falha do scheduler → `Result.failure`.
- `FullScreenAlarmViewModelTest`: carga; chip padrão vindo das configurações (e fallback 10); item inexistente fecha; snooze sucesso → `CloseScreen(true)`; falha → mensagem sem fechar; duplo toque; dispensar; voltar → `CloseScreen(false)`; "agora" quando início passou.
- `FullScreenAlarmActivityRobolectricTest` (`ActivityScenario`): semântica das tags (`Role.RadioButton`, descrições), clicar Adiar chama o use case e finaliza; notificação cancelada; item inexistente finaliza; flags de tela (`shadowOf(window)`) quando aplicável.
- `SnoozeThenFireIntegrationTest`: adiar → disparar o receiver (spec 04) → próximo alarme da série reagendado.
