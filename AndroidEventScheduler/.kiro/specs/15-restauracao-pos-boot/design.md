# 15 — Restauração de alarmes após reinício — Design

## Pacotes
```
core/alarm/
├── domain/usecase/RescheduleAllAlarmsUseCase.kt    (novo)
├── domain/model/RescheduleSummary.kt               (novo)
└── data/BootCompletedReceiver.kt                   (novo — trata boot, atualização do app, fuso/hora e permissão de alarme exato)
```
O nome `BootCompletedReceiver` mantém o termo do PDF.

## Domínio
```kotlin
data class RescheduleSummary(val scheduled: Int, val skipped: Int, val failed: Int)

class RescheduleAllAlarmsUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val scheduleNextAlarm: ScheduleNextAlarmUseCase,
) {
    suspend operator fun invoke(): Result<RescheduleSummary> = runCatching {
        var scheduled = 0; var skipped = 0; var failed = 0
        for (item in scheduleRepository.getAll()) {
            runCatching { scheduleNextAlarm(item) }
                .onSuccess { triggerMillis -> if (triggerMillis != null) scheduled++ else skipped++ }
                .onFailure { failed++ }
        }
        RescheduleSummary(scheduled, skipped, failed)
    }
}
```
- `getAll()` falhar → `Result.failure` (nada a fazer, log).
- **Diferença do PDF:** não "avança" nem regrava o item recorrente; a próxima ocorrência é calculada a partir de `RecurrenceEngine` (spec 01) dentro de `ScheduleNextAlarmUseCase`. Por isso o `RecurrenceEngine` não aparece aqui.
- **Por que UseCase:** é o único lugar que sabe "reagendar tudo"; usado por receiver (boot/atualização/fuso/hora) e passível de reuso ao restaurar backup; lógica de agregação de resultado testável sem Android.

## Receiver
```kotlin
class BootCompletedReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in SUPPORTED_ACTIONS) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + get<CoroutineDispatcher>(named("io"))).launch {
            try { get<RescheduleAllAlarmsUseCase>()().onFailure { Log.w(TAG, "reschedule failed", it) } }
            finally { pendingResult.finish() }
        }
    }
    companion object {
        val SUPPORTED_ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED, "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED")
    }
}
```
- `Intent.ACTION_TIME_CHANGED` = `android.intent.action.TIME_SET`.
- `exported`: ações protegidas do sistema não precisam de `exported=true` para receiver estático desde que `BOOT_COMPLETED`… na prática receivers de broadcasts do sistema devem ser `exported="true"` em API 31+ quando recebem ações do sistema que são enviadas por processo do sistema; **verificar na documentação atual** e validar com `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p <pacote>` no emulador.
- Manifest:
  ```xml
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
  <receiver android:name=".core.alarm.data.BootCompletedReceiver" android:enabled="true" android:exported="true">
      <intent-filter>
          <action android:name="android.intent.action.BOOT_COMPLETED"/>
          <action android:name="android.intent.action.MY_PACKAGE_REPLACED"/>
          <action android:name="android.intent.action.TIMEZONE_CHANGED"/>
          <action android:name="android.intent.action.TIME_SET"/>
          <action android:name="android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"/>
      </intent-filter>
  </receiver>
  ```
- Koin: `factory { RescheduleAllAlarmsUseCase(get(), get()) }` em `AlarmModule`.

## Decisões de UseCase
- `RescheduleAllAlarmsUseCase`: justificado (ver acima).

## Testes
- `RescheduleAllAlarmsUseCaseTest` (MockK + `FakeClock`, `ScheduleNextAlarmUseCase` real com `AlarmScheduler` mockado): único futuro → agendado; único passado → ignorado; diário perdido enquanto desligado → próximo dia; semanal ter/qui; série encerrada; `reminder NONE`; um item lançando exceção não impede os outros; banco vazio → `0/0/0`; idempotência (duas execuções = mesmos `AlarmRequest`); `getAll` lançando → `Result.failure`.
- `BootCompletedReceiverRobolectricTest`: ação `BOOT_COMPLETED` aciona o use case (substituir módulo Koin por mock); `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`, `TIME_SET` idem; ação desconhecida ou `null` não aciona; `PendingResult` finalizado mesmo com falha.
- `ManifestReceiverTest` (Robolectric `PackageManager`): receiver declarado com as ações esperadas e sem `directBootAware`.
- `BootRestoreIntegrationTest` (Room em memória + `ShadowAlarmManager`): popular itens, limpar alarmes, enviar broadcast, conferir alarmes agendados.
