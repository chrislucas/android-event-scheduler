# 04 — Alarmes e notificações — Design

## Pacotes
```
core/alarm/
├── domain/
│   ├── model/      AlarmRequest, AlertPresentationMode
│   ├── scheduler/  AlarmScheduler (interface)
│   └── usecase/    ScheduleNextAlarmUseCase      # (Snooze → spec 14; RescheduleAll → spec 15)
├── data/
│   ├── AndroidAlarmScheduler.kt   # AlarmManager
│   ├── ScheduleAlarmReceiver.kt   # BroadcastReceiver (KoinComponent)
│   └── notification/ NotificationChannels.kt, ScheduleNotifier.kt, AlarmIntents.kt
└── di/AlarmModule.kt
core/permissions/NotificationPermissionEffect.kt
app/DeepLinkState.kt             # scheduleId pendente (StateFlow<String?>), lido pelo AppNavHost
```
`domain` não importa Android. `data` aqui contém os pontos de entrada do sistema (receiver) por serem detalhe de plataforma.

Constantes e helpers públicos de `core.alarm` usados por outras specs: `AlarmIntents.ACTION_SHOW_FULL_SCREEN_ALARM`, `AlarmIntents.EXTRA_SCHEDULE_ID`, `AlarmIntents.notificationId(scheduleId)` (usado ao notificar e ao cancelar a notificação na spec 14) e `AlertPresentationMode.forType(ScheduleType)` (`EVENT → FULL_SCREEN_EVENT`, `REMINDER → NOTIFICATION_ONLY`). `ScheduleNotFoundException` mora em `core.schedule.domain.model` (criada na spec 01 ou onde for primeiro necessária).

## Domínio
```kotlin
enum class AlertPresentationMode { NOTIFICATION_ONLY, FULL_SCREEN_EVENT }
data class AlarmRequest(val scheduleId: String, val triggerAtEpochMillis: Long, val presentationMode: AlertPresentationMode)
interface AlarmScheduler { fun schedule(request: AlarmRequest); fun cancel(scheduleId: String) }

class ScheduleNextAlarmUseCase(
    private val recurrenceEngine: RecurrenceEngine,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock,           // java.time.Clock (ZoneId embutido)
) {
    operator fun invoke(item: ScheduleItem): Long? {
        val offsetMinutes = item.reminderOffset.minutes ?: return cancelAndNull(item.id)
        val nowMillis = clock.millis()
        val nextTriggerMillis = recurrenceEngine.occurrenceDates(item, onOrAfter = today.minusDays(1))
            .map { occurrenceDate -> triggerMillis(occurrenceDate, item, offsetMinutes) }
            .firstOrNull { it > nowMillis }
            ?: return cancelAndNull(item.id)
        alarmScheduler.schedule(AlarmRequest(item.id, nextTriggerMillis, modeFor(item.type)))
        return nextTriggerMillis
    }
}
```
- `onOrAfter = hoje − 1 dia` cobre ocorrência que começou tarde da noite com offset grande. Para `offset` de 1 h e evento às 00:30, o trigger é 23:30 do dia anterior: a busca ordenada e `firstOrNull` é correta pois a sequência é crescente.
- `triggerMillis` = `LocalDateTime.of(occurrenceDate, startTimeOrAllDay).atZone(clock.zone) − offset`.
- Não é `suspend` (sem I/O: o scheduler é síncrono). `Result` desnecessário; falhas do `AlarmManager` (SecurityException) são tratadas dentro do `AndroidAlarmScheduler` (fallback inexato + log).

## AndroidAlarmScheduler
```kotlin
internal class AndroidAlarmScheduler(private val context: Context, private val alarmManager: AlarmManager) : AlarmScheduler {
    override fun schedule(request: AlarmRequest) {
        val pendingIntent = AlarmIntents.alarmPendingIntent(context, request.scheduleId, request.presentationMode)
        val canUseExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canUseExact) alarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, request.triggerAtEpochMillis, pendingIntent)
            else alarmManager.setAndAllowWhileIdle(RTC_WAKEUP, request.triggerAtEpochMillis, pendingIntent)
        } catch (securityException: SecurityException) { alarmManager.setAndAllowWhileIdle(…) }
    }
    override fun cancel(scheduleId: String) { alarmManager.cancel(AlarmIntents.alarmPendingIntent(…)) ; pendingIntent.cancel() }
}
```
Para `cancel`, usar `PendingIntent.getBroadcast(..., FLAG_NO_CREATE or FLAG_IMMUTABLE)`; se `null`, nada a fazer.

## Receiver e notificações
```kotlin
class ScheduleAlarmReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.data?.lastPathSegment ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + get<CoroutineDispatcher>(named("io"))).launch {
            try {
                val item = get<ScheduleRepository>().getById(scheduleId) ?: return@launch
                val settings = get<UserSettingsRepository>().current()
                runCatching { get<ScheduleNotifier>().show(item, settings, requestedMode(intent)) }
                get<ScheduleNextAlarmUseCase>()(item)
            } finally { pendingResult.finish() }
        }
    }
}
```
- `ScheduleNotifier(context, notificationManager)`: decide `effectiveMode` (regras R3), monta `NotificationCompat.Builder` com `CATEGORY_REMINDER`/`CATEGORY_EVENT`, `setAutoCancel(true)`, `contentIntent` abre `MainActivity` com extra `EXTRA_SCHEDULE_ID` (`FLAG_IMMUTABLE`). `notify(notificationId = scheduleId.hashCode(), …)`.
- Full-screen: `AlarmIntents.fullScreenIntent(context, scheduleId)` = `Intent(ACTION_SHOW_FULL_SCREEN_ALARM).setPackage(packageName).putExtra(...)`; `canResolve = packageManager.resolveActivity(…) != null`; `NotificationManager.canUseFullScreenIntent()` (API 34+).
- `NotificationChannels.ensureCreated(context)` chamado no `KoinInitializer` (após `startKoin`) ou numa `Initializer` própria do App Startup.
- Texto da notificação: `"%1$s • %2$s"` horário + local; sem local só horário.

## Permissões
`NotificationPermissionEffect()` usa `rememberLauncherForActivityResult(RequestPermission)`; a recusa fica registrada em `rememberSaveable` + DataStore opcional (se registrar, via `UserSettingsRepository`? **Não** — usar `SharedPreferences` simples dentro de `core.permissions` para não acoplar). Decisão: apenas `rememberSaveable` + pedir no máximo uma vez por sessão; simples e suficiente.

## Deep link
`MainActivity.onCreate/onNewIntent` → `DeepLinkState.set(intent.getStringExtra(EXTRA_SCHEDULE_ID))`. `AppNavHost` observa: quando o topo do back stack for `Home` e houver id pendente, `add(ScheduleDetail(id))` e `consume()`. `DeepLinkState` é um `single` Koin com `MutableStateFlow<String?>`.

## Decisões de UseCase
- `ScheduleNextAlarmUseCase`: **justificado** — compartilhado por Upsert, Duplicar, receiver (reagendar série) e Reschedule pós-boot (3+ pontos de entrada) e contém a regra de cálculo de gatilho com recorrência/offset.
- Sem UseCase para "mostrar notificação": é detalhe de plataforma (`ScheduleNotifier`).

## Testes mínimos
- `ScheduleNextAlarmUseCaseTest` (MockK no `AlarmScheduler`, `FakeClock`): único futuro; único passado → cancela; offset NONE → cancela; diário já passou hoje → amanhã; semanal ter/qui; limite esgotado → cancela; dia inteiro às 09:00; offset que cruza meia-noite; EVENT → `FULL_SCREEN_EVENT`, REMINDER → `NOTIFICATION_ONLY`.
- `AndroidAlarmSchedulerRobolectricTest` (`ShadowAlarmManager`): agenda exato; substitui o anterior (mesmo `scheduleId`); cancela; fallback quando exato proibido (`shadowOf(alarmManager).setCanScheduleExactAlarms(false)`).
- `ScheduleAlarmReceiverRobolectricTest`: lembrete → notificação no canal `reminders`; evento + full-screen habilitado + atividade resolvível → `fullScreenIntent` presente; evento com full-screen desabilitado → sem `fullScreenIntent`; item inexistente → nada; sempre reagenda.
- `ScheduleNotifierTest`: conteúdo (título, horário, local), canal correto, sem permissão não lança.
- `NotificationPermissionEffectRobolectricTest` (API 33): dispara o pedido uma vez.
