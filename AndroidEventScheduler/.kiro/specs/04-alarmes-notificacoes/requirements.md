# 04 — Alarmes e notificações — Requisitos

## Introdução
Infraestrutura de alerta dos agendamentos: agenda alarmes exatos no `AlarmManager` conforme a antecedência (`ReminderOffset`) e, ao disparar, mostra uma notificação (Lembrete) ou um alerta em tela cheia (Evento). Recorrências reagendam a próxima ocorrência automaticamente. A tela cheia em si é a spec 14; o reagendamento pós-boot é a spec 15.

## Requisitos

### R1 — Cálculo do próximo alarme
1. O `ScheduleNextAlarmUseCase` SHALL, dado um `ScheduleItem`, calcular o **próximo** instante de disparo = (início da ocorrência) − `reminderOffset.minutes`, estritamente depois de `agora`, percorrendo `RecurrenceEngine.occurrenceDates`.
2. IF `reminderOffset = NONE` OR não há ocorrência futura (série encerrada / item único já passado) THEN SHALL cancelar qualquer alarme do item e devolver `null`.
3. Para item de dia inteiro, o início da ocorrência SHALL ser `09:00` local (`ALL_DAY_ALERT_TIME`).
4. O modo de apresentação SHALL ser `FULL_SCREEN_EVENT` para `EVENT` e `NOTIFICATION_ONLY` para `REMINDER`.
5. Cada item SHALL ter no máximo **um** alarme pendente (agendar substitui o anterior).
6. Fuso e relógio SHALL ser injetados.

### R2 — `AlarmScheduler` (Android)
1. `AlarmScheduler` SHALL ser interface (`schedule(AlarmRequest)`, `cancel(scheduleId)`) em `core.alarm.domain`; `AlarmRequest(scheduleId, triggerAtEpochMillis, presentationMode)`.
2. A implementação SHALL usar `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, …)` quando permitido; IF `canScheduleExactAlarms()` for `false` (API 31+) THEN SHALL cair para `setAndAllowWhileIdle` sem lançar exceção.
3. O `PendingIntent` SHALL ser `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT`, endereçado ao `ScheduleAlarmReceiver` com `data = rememberme://alarm/<scheduleId>` (identidade única por item, sem colisão de hash) e extra do modo.
4. `cancel` SHALL cancelar o `PendingIntent` correspondente e ser idempotente.

### R3 — Disparo
1. WHEN o alarme dispara THEN o `ScheduleAlarmReceiver` SHALL usar `goAsync()`, carregar o item atual do repositório e as preferências, e IF o item não existir mais THEN encerrar sem notificar.
2. IF modo = `NOTIFICATION_ONLY` THEN SHALL exibir notificação de alta prioridade (canal "Lembretes") com título, horário (`HH:mm`), local (se houver) e toque que abre o app no detalhe do item.
3. IF modo = `FULL_SCREEN_EVENT` **e** `fullScreenEventAlertEnabled = true` **e** o app pode usar full-screen intent **e** a atividade de tela cheia está instalada THEN SHALL exibir notificação no canal "Eventos" com `setFullScreenIntent` para a ação `com.br.androideventscheduler.action.SHOW_FULL_SCREEN_ALARM` (extra `scheduleId`); caso contrário SHALL degradar para o comportamento do item 2.
4. IF `POST_NOTIFICATIONS` não estiver concedida (API 33+) THEN SHALL NOT lançar exceção nem notificar.
5. Depois de notificar, o receiver SHALL chamar `ScheduleNextAlarmUseCase` para reagendar a próxima ocorrência (série recorrente) — mesmo se a notificação falhar.
6. O receiver SHALL concluir (`pendingResult.finish()`) em `finally`.

### R4 — Canais e permissões
1. Os canais `reminders` e `events` SHALL ser criados na inicialização do app (idempotente), com nomes em pt-BR.
2. O manifest SHALL declarar `POST_NOTIFICATIONS`, `USE_EXACT_ALARM` (API 33+) e `SCHEDULE_EXACT_ALARM` (`maxSdkVersion=32`), `USE_FULL_SCREEN_INTENT`.
3. `core.permissions.NotificationPermissionEffect` SHALL pedir `POST_NOTIFICATIONS` uma vez (API 33+) quando incluído numa tela, sem bloquear a UI, e lembrar a recusa (não insistir em toda abertura).

### R5 — Abrir o detalhe pelo toque na notificação
1. WHEN o usuário toca a notificação THEN a `MainActivity` SHALL abrir e, após a Splash, navegar para `ScheduleDetail(scheduleId)` sobre a Home.
2. O `scheduleId` pendente SHALL ser consumido uma única vez.

### R6 — Integração com o restante do app
1. `SaveScheduleUseCase` (spec 07) e `DuplicateScheduleUseCase` (spec 11) SHALL chamar `ScheduleNextAlarmUseCase`; `DeleteScheduleUseCase` (spec 11) SHALL chamar `AlarmScheduler.cancel`.
